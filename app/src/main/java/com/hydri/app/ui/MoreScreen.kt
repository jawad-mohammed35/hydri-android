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
    const val name = "Hydri developer"
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
) {
    val s = state.settings
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editName by remember { mutableStateOf(false) }
    var name by remember(s.name) { mutableStateOf(s.name) }
    var info by remember { mutableStateOf<Pair<String, String>?>(null) }
    var busy by remember { mutableStateOf(false) }
    fun save(value: Settings) {
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
    ScreenColumn {
        PageHeader("Make Hydri yours.", "A calmer space for your everyday habits.")
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
                        save(s.copy(onboarded = false))
                    }
                }
            ) {
                Text("Review onboarding preferences")
            }
        }
        GlassCard(Modifier.fillMaxWidth()) {
            Eyebrow("APPEARANCE")
            SettingMenu("Theme", s.theme, listOf("System", "Light", "Dark")) {
                save(s.copy(theme = it))
            }
            SettingToggle("OLED pure black", "Applies whenever the dark theme is active", s.oled) {
                save(s.copy(oled = it))
            }
            Text(
                "Accent color · ${accentNames[s.accent]}",
                style = MaterialTheme.typography.bodyMedium,
            )
            AccentPicker(s.accent) { save(s.copy(accent = it)) }
            SettingToggle("Reduce animations", value = s.reducedMotion) {
                save(s.copy(reducedMotion = it))
            }
            SettingToggle("Haptic feedback", value = s.haptics) { save(s.copy(haptics = it)) }
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
                save(s.copy(unit = it))
            }
            SettingMenu(
                "Date format",
                s.dateFormat,
                listOf("System", "MM/DD/YYYY", "DD/MM/YYYY", "YYYY-MM-DD"),
            ) {
                save(s.copy(dateFormat = it))
            }
            SettingMenu("Time format", s.timeFormat, listOf("System", "12-hour", "24-hour")) {
                save(s.copy(timeFormat = it))
            }
            Text(
                "Changing units changes only the display. Your original amounts stay precise.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        GlassCard(Modifier.fillMaxWidth()) {
            Eyebrow("GENTLE REMINDERS")
            SettingToggle("Hydration reminders", value = s.reminders) {
                save(s.copy(reminders = it))
                if (it) requestNotifications()
            }
            if (s.reminders) {
                SettingMenu(
                    "Frequency",
                    s.frequency,
                    listOf(30, 60, 90, 120, 180, 240),
                    label = { "$it min" },
                ) {
                    save(s.copy(frequency = it))
                }
                TimeSetting("Active hours start", s.start, s) { save(s.copy(start = it)) }
                TimeSetting("Active hours end", s.end, s) { save(s.copy(end = it)) }
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
                            save(s.copy(days = if (day in s.days) s.days - day else s.days + day))
                        }
                    }
                }
                SettingToggle("Quiet hours", value = s.quiet) { save(s.copy(quiet = it)) }
                if (s.quiet) {
                    TimeSetting("Quiet hours start", s.quietStart, s) {
                        save(s.copy(quietStart = it))
                    }
                    TimeSetting("Quiet hours end", s.quietEnd, s) { save(s.copy(quietEnd = it)) }
                }
                SettingToggle("Stop after reaching goal", value = s.stopAtGoal) {
                    save(s.copy(stopAtGoal = it))
                }
                SettingToggle(
                    "Evening check-in",
                    "Around 20:00 if below goal; respects active and quiet hours",
                    s.evening,
                ) {
                    save(s.copy(evening = it))
                }
            }
            SettingToggle("Goal completion notification", value = s.goalNotification) {
                save(s.copy(goalNotification = it))
                if (it) requestNotifications()
            }
            SettingToggle("Notification sound", value = s.sound) { save(s.copy(sound = it)) }
            SettingToggle("Notification vibration", value = s.vibration) {
                save(s.copy(vibration = it))
            }
            TextButton(
                onClick = {
                    context.startActivity(
                        Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
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
        GlassCard(Modifier.fillMaxWidth()) {
            Eyebrow("HYDRATION REFERENCE")
            Text("A reference, not a prescription.", style = MaterialTheme.typography.titleLarge)
            Text(
                "Total water includes drinking water, other beverages and moisture in food. These are adequate intake reference values, not drinking-water-only goals or personalized medical advice."
            )
            listOf(
                    Triple("Age group", "Male", "Female"),
                    Triple("1–3 years", "1.3 L", "1.3 L"),
                    Triple("4–8 years", "1.7 L", "1.7 L"),
                    Triple("9–13 years", "2.4 L", "2.1 L"),
                    Triple("14–18 years", "3.3 L", "2.3 L"),
                    Triple("19+ years", "3.7 L", "2.7 L"),
                )
                .forEach { (age, male, female) ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            age,
                            Modifier.weight(1.4f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(male, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            female,
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            Text(
                "Needs vary with activity, climate, health conditions and individual circumstances. Follow clinician guidance if you have a fluid restriction. Hydri never sets goals from age or gender.",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "Source: Institute of Medicine (2005), Dietary Reference Intakes for Water, Potassium, Sodium, Chloride, and Sulfate. doi:10.17226/10925, chapter 4 and summary tables.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            android.net.Uri.parse(
                                "https://www.nationalacademies.org/read/10925/chapter/6"
                            ),
                        )
                    )
                }
            ) {
                Text("Read the source (browser)")
            }
        }
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
                        arrayOf("application/json", "text/plain", "application/octet-stream")
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
                Text("Delete hydration history", color = MaterialTheme.colorScheme.error)
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
        GlassCard(Modifier.fillMaxWidth()) {
            Eyebrow("OPEN SOURCE")
            Text("Built with care, on open foundations.")
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
                                    context.assets.open("EPL-1.0.html").bufferedReader().use {
                                        it.readText()
                                    },
                                    android.text.Html.FROM_HTML_MODE_LEGACY,
                                )
                                .toString()
                }
            ) {
                Text("Full EPL license (test tooling)")
            }
        }
        GlassCard(Modifier.fillMaxWidth()) {
            Eyebrow("ABOUT THE DEVELOPER")
            Text(DeveloperInfo.name)
            Text(
                "Developer details can be configured in DeveloperInfo before release.",
                style = MaterialTheme.typography.bodySmall,
            )
            listOf(DeveloperInfo.website, DeveloperInfo.github, DeveloperInfo.contact)
                .filter { it.isNotBlank() }
                .forEach { Text(it) }
        }
        Column(
            Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Symbol(Icons.Outlined.WaterDrop)
            Text("Hydri", style = MaterialTheme.typography.titleLarge)
            Text(
                "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "A little water. A little more you.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (editName)
        AlertDialog(
            onDismissRequest = { editName = false },
            title = { Text("What should we call you?") },
            text = {
                OutlinedTextField(
                    name,
                    { name = it.take(60) },
                    label = { Text("Preferred name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        save(s.copy(name = name.trim()))
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
    TextButton(
        onClick = {
            android.app
                .TimePickerDialog(
                    context,
                    { _, h, m -> onChange(h * 60 + m) },
                    minute / 60,
                    minute % 60,
                    s.timeFormat == "24-hour" || (s.timeFormat == "System" && system24),
                )
                .show()
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(title, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
        Text(label)
    }
}
