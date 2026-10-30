package com.hydri.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.hydri.app.data.*
import com.hydri.app.domain.*
import java.time.*
import java.util.UUID

@Composable
fun GlassEditor(
    title: String,
    saveLabel: String,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassDialog(title, saveLabel, onDismiss, onSave, enabled = enabled, content = content)
}

@Composable
fun AmountDialog(
    title: String,
    initial: Long,
    unit: String,
    help: String,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
) {
    var text by remember { mutableStateOf(Volume.input(initial, unit)) }
    var dirty by remember { mutableStateOf(false) }
    var preset by remember { mutableStateOf<Long?>(null) }
    GlassEditor(
        title,
        "Save goal",
        Volume.parse(text, unit) != null,
        onDismiss,
        { onSave(preset ?: if (dirty) Volume.parse(text, unit)!! else initial) },
    ) {
        Text(
            help,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(1_500_000L, 2_000_000L, 2_500_000L).forEach { value ->
                Choice(
                    Volume.format(value, unit),
                    preset == value || (!dirty && initial == value),
                ) {
                    text = Volume.input(value, unit)
                    dirty = true
                    preset = value
                }
            }
        }
        VolumeField(
            text,
            {
                text = it
                dirty = true
                preset = null
            },
            unit,
        )
    }
}

@Composable
fun BottleDialog(
    bottle: Bottle?,
    unit: String,
    position: Int,
    onDismiss: () -> Unit,
    onSave: (Bottle) -> Unit,
) {
    var name by remember { mutableStateOf(bottle?.name ?: "") }
    var volume by remember { mutableStateOf(Volume.input(bottle?.volume ?: 500_000, unit)) }
    var dirty by remember { mutableStateOf(false) }
    var icon by remember { mutableIntStateOf(bottle?.icon ?: 1) }
    var color by remember { mutableIntStateOf(bottle?.color ?: 0) }
    GlassEditor(
        if (bottle == null) "Create bottle" else "Edit bottle",
        "Save bottle",
        name.isNotBlank() && Volume.parse(volume, unit) != null,
        onDismiss,
        {
            onSave(
                Bottle(
                    bottle?.id ?: UUID.randomUUID().toString(),
                    name.trim(),
                    if (!dirty && bottle != null) bottle.volume else Volume.parse(volume, unit)!!,
                    icon,
                    color,
                    bottle?.favorite ?: true,
                    bottle?.position ?: position,
                )
            )
        },
    ) {
        Row(
            Modifier.fillMaxWidth()
                .background(accents[color].copy(alpha = .08f), RoundedCornerShape(22.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Symbol(bottleIcons[icon], bottleNames[icon], accents[color], 56)
            Column(Modifier.weight(1f)) {
                Text(
                    name.ifBlank { "Bottle preview" },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    Volume.parse(volume, unit)?.let { Volume.format(it, unit) } ?: "—",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        OutlinedTextField(
            name,
            { name = it.take(60) },
            label = { Text("Bottle name") },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        VolumeField(
            volume,
            {
                volume = it
                dirty = true
            },
            unit,
        )
        Text("Shape", style = MaterialTheme.typography.labelLarge)
        Row(
            Modifier.testTag("bottle-shapes").horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            bottleIcons.forEachIndexed { i, image ->
                IconButton(
                    { icon = i },
                    Modifier.background(
                            if (icon == i) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f),
                            RoundedCornerShape(16.dp),
                        )
                        .semantics { selected = icon == i },
                ) {
                    Icon(image, bottleNames[i])
                }
            }
        }
        Text("Color", style = MaterialTheme.typography.labelLarge)
        AccentPicker(color) { color = it }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDialog(
    entry: WaterEntry?,
    defaultDate: LocalDate,
    s: Settings,
    onDismiss: () -> Unit,
    onDelete: (WaterEntry) -> Unit,
    onSave: (Long, LocalDate, Long) -> Unit,
) {
    val context = LocalContext.current
    val zone = entry?.zone?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
    val date = entry?.date?.let { LocalDate.parse(it) } ?: defaultDate
    var amount by remember { mutableStateOf(Volume.input(entry?.volume ?: 250_000, s.unit)) }
    var dirty by remember { mutableStateOf(false) }
    val initialTime = remember {
        entry?.let { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalTime() }
            ?: LocalTime.now()
    }
    val system24 = android.text.format.DateFormat.is24HourFormat(context)
    val timeState =
        rememberTimePickerState(
            initialTime.hour,
            initialTime.minute,
            s.timeFormat == "24-hour" || (s.timeFormat == "System" && system24),
        )
    var showTime by remember { mutableStateOf(false) }
    val time =
        if (timeState.hour == initialTime.hour && timeState.minute == initialTime.minute)
            initialTime
        else LocalTime.of(timeState.hour, timeState.minute)
    val timestamp = EntryTime.resolve(date, time, zone, entry?.timestamp)
    GlassEditor(
        if (entry == null) "Log water" else "Edit sip",
        "Save entry",
        Volume.parse(amount, s.unit) != null && timestamp != null && date <= LocalDate.now(),
        onDismiss,
        {
            onSave(
                if (!dirty && entry != null) entry.volume else Volume.parse(amount, s.unit)!!,
                date,
                timestamp!!,
            )
        },
    ) {
        Text(
            formattedDate(date, s.dateFormat),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        VolumeField(
            amount,
            {
                amount = it
                dirty = true
            },
            s.unit,
        )
        OutlinedButton(
            onClick = { showTime = !showTime },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
        ) {
            Icon(Icons.Outlined.Schedule, null)
            Spacer(Modifier.width(10.dp))
            Text(
                "Time: " +
                    time.format(
                        java.time.format.DateTimeFormatter.ofPattern(
                            if (s.timeFormat == "24-hour" || (s.timeFormat == "System" && system24))
                                "HH:mm"
                            else "h:mm a"
                        )
                    )
            )
        }
        if (showTime) TimeInput(timeState, modifier = Modifier.align(Alignment.CenterHorizontally))
        if (timestamp == null)
            Text(
                "This time does not exist on this date. Choose another time.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        if (zone != ZoneId.systemDefault())
            Text(zone.id, style = MaterialTheme.typography.bodySmall)
        if (entry != null)
            TextButton(onClick = { onDelete(entry) }) {
                Icon(Icons.Outlined.DeleteOutline, null)
                Text("Delete entry", color = MaterialTheme.colorScheme.error)
            }
    }
}
