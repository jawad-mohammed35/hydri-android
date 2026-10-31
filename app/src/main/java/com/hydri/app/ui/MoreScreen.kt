// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0

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
import com.hydri.app.R
import com.hydri.app.background.Reminders
import com.hydri.app.data.Settings
import com.hydri.app.i18n.*
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/** Replace these values when the developer elects to publish contact details. */
object DeveloperInfo {
    val name
        get() = tr(R.string.developed_by_jawad_mohammed)

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
    val appLanguage by AppLocales.selection.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editName by remember { mutableStateOf(false) }
    var reviewPreferences by remember { mutableStateOf(false) }
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
                            ?: throw UserMessage(tr(R.string.cannot_open_the_export_location))
                    }
                    vm.messages.emit(tr(R.string.csv_exported) to null)
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
                            ?: throw UserMessage(tr(R.string.cannot_open_the_backup_location))
                    }
                    vm.messages.emit(tr(R.string.local_backup_saved) to null)
                }
        }
    val restore =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                onConfirm(tr(R.string.replace_all_current_records_bottles_and_preferences_with_t)) {
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
                                                if (result.length > 10_000_000)
                                                    throw UserMessage(
                                                        tr(R.string.backup_exceeds_10_mb)
                                                    )
                                                count = reader.read(buffer)
                                            }
                                            result.toString()
                                        } ?: throw UserMessage(tr(R.string.cannot_open_this_backup))
                                try {
                                    vm.repo.restore(text)
                                } catch (e: UserMessage) {
                                    throw e
                                } catch (_: org.json.JSONException) {
                                    throw UserMessage(tr(R.string.invalid_backup))
                                } catch (_: IllegalArgumentException) {
                                    throw UserMessage(tr(R.string.invalid_backup))
                                }
                            }
                            Reminders.schedule(context, vm.repo.preferences.flow.first())
                            vm.messages.emit(tr(R.string.backup_restored) to null)
                        } finally {
                            busy = false
                        }
                    }
                }
            }
        }
    ScreenColumn(Modifier.testTag("settings-page-$page"), scrollKey = page) {
        if (page == "More") {
            PageHeader(tr(R.string.more))
            GlassCard(Modifier.fillMaxWidth()) {
                Text(
                    tr(R.string.personalization),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = {
                        name = s.name
                        editName = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                ) {
                    Text(tr(R.string.preferred_name), Modifier.weight(1f))
                    Text(
                        s.name.ifBlank { tr(R.string.add_a_name) },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = onGoal,
                    colors =
                        ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                ) {
                    Text(tr(R.string.daily_hydration_goal))
                }
                TextButton(
                    onClick = { reviewPreferences = true },
                    colors =
                        ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                ) {
                    Text(tr(R.string.review_onboarding_preferences))
                }
            }
            GlassCard(Modifier.fillMaxWidth()) {
                Eyebrow(tr(R.string.appearance_152))
                SettingMenu(tr(R.string.theme), s.theme, listOf("System", "Light", "Dark")) {
                    save { latest -> latest.copy(theme = it) }
                }
                SettingToggle(
                    tr(R.string.oled_pure_black),
                    tr(R.string.applies_whenever_the_dark_theme_is_active),
                    s.oled,
                ) {
                    save { latest -> latest.copy(oled = it) }
                }
                Text(
                    if (s.dynamicColors) tr(R.string.accent_match_device_colors)
                    else tr(R.string.accent_color_value, accentNames[s.accent]),
                    style = MaterialTheme.typography.bodyMedium,
                )
                AccentPicker(if (s.dynamicColors) -1 else s.accent) {
                    save { latest -> latest.copy(accent = it, dynamicColors = false) }
                }
                Choice(tr(R.string.match_device_colors), s.dynamicColors) {
                    save { latest -> latest.copy(dynamicColors = true) }
                }
                SettingToggle(tr(R.string.reduce_animations), value = s.reducedMotion) {
                    save { latest -> latest.copy(reducedMotion = it) }
                }
                SettingToggle(tr(R.string.haptic_feedback), value = s.haptics) {
                    save { latest -> latest.copy(haptics = it) }
                }
            }
            GlassCard(Modifier.fillMaxWidth()) {
                Eyebrow(tr(R.string.settings))
                // Native locale APIs recreate the activity with its saved navigation state.
                SettingMenu(
                    tr(R.string.language),
                    appLanguage,
                    listOf("") + AppLocales.languages.keys,
                    label = AppLocales::label,
                ) {
                    AppLocales.select(it)
                }
                SettingMenu(
                    tr(R.string.water_units),
                    s.unit,
                    listOf("mL", "L", "oz"),
                    label = {
                        when (it) {
                            "oz" -> tr(R.string.us_fl_oz)
                            else -> unitLabel(it)
                        }
                    },
                ) {
                    save { latest -> latest.copy(unit = it) }
                }
                SettingMenu(
                    tr(R.string.date_format),
                    s.dateFormat,
                    listOf("System", "MM/DD/YYYY", "DD/MM/YYYY", "YYYY-MM-DD"),
                ) {
                    save { latest -> latest.copy(dateFormat = it) }
                }
                SettingMenu(
                    tr(R.string.time_format),
                    s.timeFormat,
                    listOf("System", "12-hour", "24-hour"),
                ) {
                    save { latest -> latest.copy(timeFormat = it) }
                }
                Text(
                    tr(R.string.changing_units_changes_only_the_display_your_original_amou),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            GlassCard(Modifier.fillMaxWidth()) {
                SettingsLink(tr(R.string.notifications), Icons.Outlined.Notifications) {
                    onNavigate("Notifications")
                }
                SettingsLink(tr(R.string.hydration_guide), Icons.Outlined.Info) {
                    onNavigate("Hydration Guide")
                }
                SettingsLink(tr(R.string.data_backup), Icons.Outlined.Backup) {
                    onNavigate("Data & Backup")
                }
                SettingsLink(tr(R.string.about), Icons.Outlined.Info) { onNavigate("About") }
            }
            Column(
                Modifier.fillMaxWidth().padding(vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Symbol(Icons.Outlined.WaterDrop)
                Text("Hydri", style = MaterialTheme.typography.titleLarge)
                Text(
                    tr(R.string.version_value, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, tr(R.string.back_to_more))
                }
                Text(routeLabel(page), style = MaterialTheme.typography.headlineMedium)
            }
            when (page) {
                "Notifications" -> {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Eyebrow(tr(R.string.reminders))
                        SettingToggle(tr(R.string.hydration_reminders), value = s.reminders) {
                            save { latest -> latest.copy(reminders = it) }
                            if (it) requestNotifications()
                        }
                        if (s.reminders) {
                            SettingMenu(
                                tr(R.string.frequency),
                                s.frequency,
                                listOf(30, 60, 90, 120, 180, 240),
                                label = { tr(R.string.value_min, it) },
                            ) {
                                save { latest -> latest.copy(frequency = it) }
                            }
                            TimeSetting(tr(R.string.active_hours_start), s.start, s) {
                                save { latest -> latest.copy(start = it) }
                            }
                            TimeSetting(tr(R.string.active_hours_end), s.end, s) {
                                save { latest -> latest.copy(end = it) }
                            }
                            Text(tr(R.string.reminder_days), fontWeight = FontWeight.Medium)
                            Row(
                                Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                (1..7).forEach { day ->
                                    Choice(
                                        DayOfWeek.of(day)
                                            .getDisplayName(
                                                java.time.format.TextStyle.SHORT,
                                                AppText.locale,
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
                            SettingToggle(tr(R.string.quiet_hours), value = s.quiet) {
                                save { latest -> latest.copy(quiet = it) }
                            }
                            if (s.quiet) {
                                TimeSetting(tr(R.string.quiet_hours_start), s.quietStart, s) {
                                    save { latest -> latest.copy(quietStart = it) }
                                }
                                TimeSetting(tr(R.string.quiet_hours_end), s.quietEnd, s) {
                                    save { latest -> latest.copy(quietEnd = it) }
                                }
                            }
                            SettingToggle(
                                tr(R.string.stop_after_reaching_goal),
                                value = s.stopAtGoal,
                            ) {
                                save { latest -> latest.copy(stopAtGoal = it) }
                            }
                            SettingToggle(
                                tr(R.string.evening_check_in),
                                tr(
                                    R.string
                                        .around_20_00_if_below_goal_respects_active_and_quiet_hours
                                ),
                                s.evening,
                            ) {
                                save { latest -> latest.copy(evening = it) }
                            }
                        }
                        SettingToggle(
                            tr(R.string.goal_completion_notification),
                            value = s.goalNotification,
                        ) {
                            save { latest -> latest.copy(goalNotification = it) }
                            if (it) requestNotifications()
                        }
                        SettingToggle(tr(R.string.notification_sound), value = s.sound) {
                            save { latest -> latest.copy(sound = it) }
                        }
                        SettingToggle(tr(R.string.notification_vibration), value = s.vibration) {
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
                            Text(tr(R.string.android_notification_settings))
                        }
                        Text(
                            tr(R.string.reminders_are_approximate_and_may_be_delayed_by_android_ba),
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
                        Eyebrow(tr(R.string.your_data_your_device))
                        Text(tr(R.string.hydri_works_entirely_offline_no_accounts_ads_analytics_or))
                        TextButton(
                            onClick = { export.launch("hydri-history-${state.today}.csv") },
                            enabled = !busy,
                        ) {
                            Icon(Icons.Outlined.FileDownload, null)
                            Text(tr(R.string.export_history_to_csv))
                        }
                        TextButton(
                            onClick = { backup.launch("hydri-backup-${state.today}.json") },
                            enabled = !busy,
                        ) {
                            Text(tr(R.string.create_local_backup))
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
                            Text(
                                if (busy) tr(R.string.restoring)
                                else tr(R.string.restore_local_backup)
                            )
                        }
                        TextButton(
                            onClick = {
                                onConfirm(
                                    tr(
                                        R.string
                                            .permanently_delete_all_water_history_bottles_and_preferenc
                                    )
                                ) {
                                    vm.action {
                                        vm.repo.deleteHistory()
                                        vm.messages.emit(tr(R.string.history_deleted) to null)
                                    }
                                }
                            }
                        ) {
                            Text(
                                tr(R.string.delete_hydration_history),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        TextButton(
                            onClick = {
                                onConfirm(
                                    tr(
                                        R.string
                                            .reset_hydri_and_permanently_delete_all_local_water_entries
                                    )
                                ) {
                                    vm.action {
                                        vm.repo.reset()
                                        Reminders.schedule(context, Settings())
                                    }
                                }
                            }
                        ) {
                            Text(
                                tr(R.string.reset_all_app_data),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
                "About" -> {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Eyebrow(tr(R.string.about_the_developer))
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
                        Eyebrow(tr(R.string.open_source))
                        Text(tr(R.string.hydri_apache_license_2_0))
                        Text(tr(R.string.copyright_2026_jawad_mohammed))
                        TextButton(
                            onClick = {
                                info =
                                    tr(R.string.protocol_buffers_bsd_3_clause) to
                                        context.assets
                                            .open("Protobuf-BSD-3-Clause.txt")
                                            .bufferedReader()
                                            .use { it.readText() }
                            }
                        ) {
                            Text(tr(R.string.protocol_buffers_license))
                        }
                        TextButton(
                            onClick = {
                                info =
                                    tr(R.string.open_source_acknowledgments) to
                                        tr(
                                            R.string
                                                .hydri_apache_license_2_0_copyright_2026_jawad_mohammed_n_n
                                        )
                            }
                        ) {
                            Text(tr(R.string.libraries_licenses))
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
                            Text(tr(R.string.full_apache_license))
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
                            Text(tr(R.string.full_epl_license_test_tooling))
                        }
                    }
                }
            }
        }
    }
    if (reviewPreferences)
        GlassDialog(
            tr(R.string.review_preferences),
            tr(R.string.review),
            { reviewPreferences = false },
            {
                save { latest -> latest.copy(onboarded = false) }
                reviewPreferences = false
            },
        ) {
            Text(
                tr(R.string.review_your_name_appearance_units_and_daily_goal_in_the_se),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    if (editName)
        GlassDialog(
            tr(R.string.preferred_name),
            tr(R.string.save),
            { editName = false },
            {
                save { latest -> latest.copy(name = name.trim()) }
                editName = false
            },
        ) {
            OutlinedTextField(
                name,
                { name = it.take(60) },
                label = { Text(tr(R.string.preferred_name)) },
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().testTag("preferred-name-input"),
                keyboardOptions =
                    androidx.compose.foundation.text.KeyboardOptions(
                        capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words
                    ),
            )
        }
    info?.let { (title, text) ->
        val originalLicense =
            title == "Apache License 2.0" ||
                title == "Eclipse Public License 1.0" ||
                title == tr(R.string.protocol_buffers_bsd_3_clause)
        val displayText =
            if (originalLicense) tr(R.string.source_license_english) + "\n\n" + text else text
        AlertDialog(
            onDismissRequest = { info = null },
            title = { Text(title) },
            text = {
                Text(
                    displayText,
                    Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = { TextButton(onClick = { info = null }) { Text(tr(R.string.done)) } },
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
