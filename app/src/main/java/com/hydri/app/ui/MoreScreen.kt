package com.hydri.app.ui

import android.content.Intent
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hydri.app.BuildConfig
import com.hydri.app.background.Reminders
import com.hydri.app.data.Settings
import java.time.*
import java.util.Locale
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/** Replace these values when the developer elects to publish contact details. */
object DeveloperInfo {
    const val name = "Developed by Jawad Mohammed"
    const val linkedin = "https://www.linkedin.com/in/jawad-mohammed01"
    const val website = ""
    const val github = ""
    const val contact = ""
}

@Composable
fun MoreScreen(
    state: UiState,
    vm: HydriViewModel,
    requestNotifications: () -> Unit,
    onGoal: () -> Unit,
    onConfirm: (String, () -> Unit) -> Unit,
    snackbar: SnackbarHostState,
    page: String = "More",
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val s = state.settings
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editName by remember { mutableStateOf(false) }
    var name by remember(s.name) { mutableStateOf(s.name) }
    var info by remember { mutableStateOf<Pair<String, String>?>(null) }
    var busy by remember { mutableStateOf(false) }
    fun save(value: (Settings) -> Settings) {
        vm.settings(value)
    }
    val export =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri
            ->
            if (uri != null)
                vm.action {
                    withContext(Dispatchers.IO) {
                        context.contentResolver
                            .openOutputStream(uri, "wt")
                            ?.bufferedWriter(Charsets.UTF_8)
                            ?.use { it.write(vm.repo.csv()) }
                            ?: error("Cannot open the export location.")
                    }
                    vm.messages.emit("CSV exported" to null)
                }
        }
    val backup =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            if (uri != null)
                vm.action {
                    withContext(Dispatchers.IO) {
                        context.contentResolver
                            .openOutputStream(uri, "wt")
                            ?.bufferedWriter(Charsets.UTF_8)
                            ?.use { it.write(vm.repo.backup()) }
                            ?: error("Cannot open the backup location.")
                    }
                    vm.messages.emit("Local backup saved" to null)
                }
        }
    val restore =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                onConfirm(
                    "Replace all current records, bottles and preferences with this backup? This cannot be undone. Create a backup first if you want to keep your current data."
                ) {
                    busy = true
                    vm.action {
                        try {
                            withContext(Dispatchers.IO) {
                                val text =
                                    context.contentResolver
                                        .openInputStream(uri)
                                        ?.bufferedReader()
                                        ?.use { reader ->
                                            val buffer = CharArray(8192)
                                            val result = StringBuilder()
                                            var count = reader.read(buffer)
                                            while (count != -1) {
                                                result.append(buffer, 0, count)
                                                require(result.length <= 10_000_000) {
                                                    "Backup exceeds 10 MB."
                                                }
                                                count = reader.read(buffer)
                                            }
                                            result.toString()
                                        } ?: error("Cannot open this backup.")
                                vm.repo.restore(text)
                            }
                            Reminders.schedule(context, vm.repo.preferences.flow.first())
                            vm.messages.emit("Backup restored" to null)
                        } finally {
                            busy = false
                        }
                    }
                }
            }
        }
    ScreenColumn(Modifier.testTag("settings-page-$page")) {
        if (page == "More") {
            PageHeader("More")
            GlassCard(Modifier.fillMaxWidth()) {
                Eyebrow("PERSONALIZATION")
                TextButton(onClick = { editName = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Preferred name", Modifier.weight(1f))
                    Text(s.name.ifBlank { "Add a name" })
                }
                TextButton(onClick = onGoal) { Text("Daily hydration goal") }
                TextButton(
                    onClick = {
                        onConfirm(
                            "Reopen the setup flow? Your water history and bottles will be kept."
                        ) {
                            save { latest -> latest.copy(onboarded = false) }
                        }
                    }
                ) {
                    Text("Review onboarding preferences")
                }
            }
            GlassCard(Modifier.fillMaxWidth()) {
                Eyebrow("APPEARANCE")
                SettingMenu("Theme", s.theme, listOf("System", "Light", "Dark")) {
                    save { latest -> latest.copy(theme = it) }
                }
                SettingToggle(
                    "OLED pure black",
                    "Applies whenever the dark theme is active",
                    s.oled,
                ) {
                    save { latest -> latest.copy(oled = it) }
                }
                Text(
                    if (s.dynamicColors) "Accent · Match Device Colors"
                    else "Accent color · ${accentNames[s.accent]}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                AccentPicker(if (s.dynamicColors) -1 else s.accent) {
                    save { latest -> latest.copy(accent = it, dynamicColors = false) }
                }
                Choice("Match Device Colors", s.dynamicColors) {
                    save { latest -> latest.copy(dynamicColors = true) }
                }
                SettingToggle("Reduce animations", value = s.reducedMotion) {
                    save { latest -> latest.copy(reducedMotion = it) }
                }
                SettingToggle("Haptic feedback", value = s.haptics) {
                    save { latest -> latest.copy(haptics = it) }
                }
            }
            GlassCard(Modifier.fillMaxWidth()) {
                Eyebrow("MEASUREMENTS & FORMATS")
                SettingMenu(
                    "Water units",
                    s.unit,
                    listOf("mL", "L", "oz"),
                    label = {
                        when (it) {
                            "oz" -> "US fl oz"
                            else -> it
                        }
                    },
                ) {
                    save { latest -> latest.copy(unit = it) }
                }
                SettingMenu(
                    "Date format",
                    s.dateFormat,
                    listOf("System", "MM/DD/YYYY", "DD/MM/YYYY", "YYYY-MM-DD"),
                ) {
                    save { latest -> latest.copy(dateFormat = it) }
                }
                SettingMenu("Time format", s.timeFormat, listOf("System", "12-hour", "24-hour")) {
                    save { latest -> latest.copy(timeFormat = it) }
                }
                Text(
                    "Changing units changes only the display. Your original amounts stay precise.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            GlassCard(Modifier.fillMaxWidth()) {
                SettingsLink("Notifications", Icons.Outlined.Notifications) {
                    onNavigate("Notifications")
                }
                SettingsLink("Hydration Guide", Icons.Outlined.Info) {
                    onNavigate("Hydration Guide")
                }
                SettingsLink("Data & Backup", Icons.Outlined.Backup) { onNavigate("Data & Backup") }
                SettingsLink("About", Icons.Outlined.Info) { onNavigate("About") }
            }
            Column(
                Modifier.fillMaxWidth().padding(vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Symbol(Icons.Outlined.WaterDrop)
                Text("Hydri", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Version ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back to More") }
                Text(page, style = MaterialTheme.typography.headlineMedium)
            }
            when (page) {
                "Notifications" -> {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Eyebrow("REMINDERS")
                        SettingToggle("Hydration reminders", value = s.reminders) {
                            save { latest -> latest.copy(reminders = it) }
                            if (it) requestNotifications()
                        }
                        if (s.reminders) {
                            SettingMenu(
                                "Frequency",
                                s.frequency,
                                listOf(30, 60, 90, 120, 180, 240),
                                label = { "$it min" },
                            ) {
                                save { latest -> latest.copy(frequency = it) }
                            }
                            TimeSetting("Active hours start", s.start, s) {
                                save { latest -> latest.copy(start = it) }
                            }
                            TimeSetting("Active hours end", s.end, s) {
                                save { latest -> latest.copy(end = it) }
                            }
                            Text("Reminder days", fontWeight = FontWeight.Medium)
                            Row(
                                Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                (1..7).forEach { day ->
                                    Choice(
                                        DayOfWeek.of(day)
                                            .getDisplayName(
                                                java.time.format.TextStyle.SHORT,
                                                Locale.getDefault(),
                                            ),
                                        day in s.days,
                                    ) {
                                        save { latest ->
                                            latest.copy(
                                                days =
                                                    if (day in latest.days) latest.days - day
                                                    else latest.days + day
                                            )
                                        }
                                    }
                                }
                            }
                            SettingToggle("Quiet hours", value = s.quiet) {
                                save { latest -> latest.copy(quiet = it) }
                            }
                            if (s.quiet) {
                                TimeSetting("Quiet hours start", s.quietStart, s) {
                                    save { latest -> latest.copy(quietStart = it) }
                                }
                                TimeSetting("Quiet hours end", s.quietEnd, s) {
                                    save { latest -> latest.copy(quietEnd = it) }
                                }
                            }
                            SettingToggle("Stop after reaching goal", value = s.stopAtGoal) {
                                save { latest -> latest.copy(stopAtGoal = it) }
                            }
                            SettingToggle(
                                "Evening check-in",
                                "Around 20:00 if below goal; respects active and quiet hours",
                                s.evening,
                            ) {
                                save { latest -> latest.copy(evening = it) }
                            }
                        }
                        SettingToggle("Goal completion notification", value = s.goalNotification) {
                            save { latest -> latest.copy(goalNotification = it) }
                            if (it) requestNotifications()
                        }
                        SettingToggle("Notification sound", value = s.sound) {
                            save { latest -> latest.copy(sound = it) }
                        }
                        SettingToggle("Notification vibration", value = s.vibration) {
                            save { latest -> latest.copy(vibration = it) }
                        }
                        TextButton(
                            onClick = {
                                context.startActivity(
                                    Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(
                                            AndroidSettings.EXTRA_APP_PACKAGE,
                                            context.packageName,
                                        )
                                )
                            }
                        ) {
                            Text("Android notification settings")
                        }
                        Text(
                            "Reminders are approximate and may be delayed by Android battery restrictions. Matching start and end times mean all day. Quiet hours take priority; no selected days means no reminders. Android channel settings can override sound and vibration.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                "Hydration Guide" -> {
                    HydrationGuide(s.unit)
                }
                "Data & Backup" -> {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Eyebrow("YOUR DATA, YOUR DEVICE")
                        Text(
                            "Hydri works entirely offline. No accounts, ads, analytics or uploads. Exports and backups are saved only where you choose. Files contain your hydration history and preferences; keep them somewhere private."
                        )
                        TextButton(
                            onClick = { export.launch("hydri-history-${state.today}.csv") },
                            enabled = !busy,
                        ) {
                            Icon(Icons.Outlined.FileDownload, null)
                            Text("Export history to CSV")
                        }
                        TextButton(
                            onClick = { backup.launch("hydri-backup-${state.today}.json") },
                            enabled = !busy,
                        ) {
                            Text("Create local backup")
                        }
                        TextButton(
                            onClick = {
                                restore.launch(
                                    arrayOf(
                                        "application/json",
                                        "text/plain",
                                        "application/octet-stream",
                                    )
                                )
                            },
                            enabled = !busy,
                        ) {
                            Text(if (busy) "Restoring…" else "Restore local backup")
                        }
                        TextButton(
                            onClick = {
                                onConfirm(
                                    "Permanently delete all water history? Bottles and preferences will be kept. Achievements will be recalculated."
                                ) {
                                    vm.action {
                                        vm.repo.deleteHistory()
                                        vm.messages.emit("History deleted" to null)
                                    }
                                }
                            }
                        ) {
                            Text(
                                "Delete hydration history",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        TextButton(
                            onClick = {
                                onConfirm(
                                    "Reset Hydri and permanently delete all local water entries, bottles, goals and preferences? This cannot be undone."
                                ) {
                                    vm.action {
                                        vm.repo.reset()
                                        Reminders.schedule(context, Settings())
                                    }
                                }
                            }
                        ) {
                            Text("Reset all app data", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                "About" -> {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Eyebrow("ABOUT THE DEVELOPER")
                        Text(DeveloperInfo.name)
                        TextButton(
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        android.net.Uri.parse(DeveloperInfo.linkedin),
                                    )
                                )
                            }
                        ) {
                            Text("LinkedIn")
                        }
                        listOf(DeveloperInfo.website, DeveloperInfo.github, DeveloperInfo.contact)
                            .filter { it.isNotBlank() }
                            .forEach { Text(it) }
                    }
                    GlassCard(Modifier.fillMaxWidth()) {
                        Eyebrow("OPEN SOURCE")
                        TextButton(
                            onClick = {
                                info =
                                    "Open-source acknowledgments" to
                                        "AndroidX: Compose, Material 3, Navigation, Room, DataStore, Lifecycle, WorkManager, Glance, Activity and Core — Apache License 2.0.\n\nKotlin and kotlinx.coroutines — Apache License 2.0.\n\nMaterial Icons — Apache License 2.0.\n\nJUnit — Eclipse Public License 1.0 (tests only).\n\nFull license texts and upstream links are included in the project's THIRD_PARTY_NOTICES.md and LICENSES directory.\n\nApache License 2.0\nhttps://www.apache.org/licenses/LICENSE-2.0\n\nThese libraries are provided without warranties. Copyright belongs to their respective contributors."
                            }
                        ) {
                            Text("Libraries & licenses")
                        }
                        TextButton(
                            onClick = {
                                info =
                                    "Apache License 2.0" to
                                        context.assets.open("Apache-2.0.txt").bufferedReader().use {
                                            it.readText()
                                        }
                            }
                        ) {
                            Text("Full Apache license")
                        }
                        TextButton(
                            onClick = {
                                info =
                                    "Eclipse Public License 1.0" to
                                        android.text.Html.fromHtml(
                                                context.assets
                                                    .open("EPL-1.0.html")
                                                    .bufferedReader()
                                                    .use { it.readText() },
                                                android.text.Html.FROM_HTML_MODE_LEGACY,
                                            )
                                            .toString()
                            }
                        ) {
                            Text("Full EPL license (test tooling)")
                        }
                    }
                }
            }
        }
    }
    if (editName)
        AlertDialog(
            onDismissRequest = { editName = false },
            title = { Text("Preferred name") },
            text = {
                OutlinedTextField(
                    name,
                    { name = it.take(60) },
                    label = { Text("Preferred name") },
                    singleLine = true,
                    keyboardOptions =
                        androidx.compose.foundation.text.KeyboardOptions(
                            capitalization =
                                androidx.compose.ui.text.input.KeyboardCapitalization.Words
                        ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        save { latest -> latest.copy(name = name.trim()) }
                        editName = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = { TextButton(onClick = { editName = false }) { Text("Cancel") } },
        )
    info?.let { (title, text) ->
        AlertDialog(
            onDismissRequest = { info = null },
            title = { Text(title) },
            text = {
                Text(text, Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()))
            },
            confirmButton = { TextButton(onClick = { info = null }) { Text("Done") } },
        )
    }
}

@Composable
fun TimeSetting(title: String, minute: Int, s: Settings, onChange: (Int) -> Unit) {
    val context = LocalContext.current
    val system24 = android.text.format.DateFormat.is24HourFormat(context)
    var open by remember { mutableStateOf(false) }
    val label =
        com.hydri.app.domain.formattedTime(
            LocalDate.now()
                .atTime(minute / 60, minute % 60)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
            s.timeFormat,
            system24,
        )
    TextButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
        Text(title, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
        Text(label)
    }
    if (open)
        GlassTimePicker(
            title,
            minute,
            s.timeFormat == "24-hour" || (s.timeFormat == "System" && system24),
            { open = false },
        ) { value ->
            onChange(value)
            open = false
        }
}
