package com.hydri.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hydri.app.data.*
import com.hydri.app.domain.*
import java.time.ZoneId

@Composable
fun HomeScreen(
    state: UiState,
    onLog: (Bottle) -> Unit,
    onCustom: () -> Unit,
    onEntry: (WaterEntry) -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    onDelete: (WaterEntry) -> Unit,
    onBottles: () -> Unit,
) {
    val s = state.settings
    val total = state.total(state.today)
    val goal = state.goal(state.today)
    val favorites = state.bottles.filter { it.favorite }
    val entries = state.entriesOn(state.today)
    ScreenColumn {
        val greeting =
            when (state.hour) {
                in 5..11 -> "Good morning"
                in 12..16 -> "Good afternoon"
                in 17..21 -> "Good evening"
                else -> "Good night"
            }
        PageHeader("$greeting${if(s.name.isBlank()) "" else ", ${s.name}"}")
        GlassCard(Modifier.fillMaxWidth()) {
            Eyebrow("Daily flow")
            WaterProgress(total, goal, s.unit, compact = true)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onUndo,
                    enabled = entries.isNotEmpty(),
                    modifier = Modifier.weight(1f).testTag("daily-undo"),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Outlined.Undo, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Undo")
                }
                OutlinedButton(
                    onReset,
                    enabled = entries.isNotEmpty(),
                    modifier = Modifier.weight(1f).testTag("daily-reset"),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Outlined.RestartAlt, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Reset")
                }
            }
            Text(
                when {
                    total > goal -> "${Volume.format(total-goal,s.unit)} above goal"
                    total == goal -> "Goal reached"
                    else -> "${Volume.format(goal-total,s.unit)} remaining"
                },
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Column {
            SectionTitle("Quick add", "Bottles", onBottles)
            if (favorites.isEmpty())
                GlassCard(Modifier.fillMaxWidth()) {
                    Text("No favorite bottles")
                    TextButton(onBottles) { Text("Choose favorites in Bottles") }
                }
            favorites.chunked(2).forEach { row ->
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { b ->
                        GlassCard(
                            Modifier.weight(1f)
                                .clickable { onLog(b) }
                                .semantics(mergeDescendants = true) {
                                    role = Role.Button
                                    contentDescription =
                                        "Log ${b.name}, ${Volume.format(b.volume,s.unit)}"
                                }
                        ) {
                            Symbol(bottleIcons[b.icon], color = accents[b.color])
                            Text(b.name, fontWeight = FontWeight.Medium)
                            Text(
                                "+ ${Volume.format(b.volume,s.unit)}",
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            OutlinedButton(
                onCustom,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Outlined.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Custom amount")
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("Today's sips")
            EntryList(entries, s, onEntry, onDelete)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryList(
    entries: List<WaterEntry>,
    s: Settings,
    onEntry: (WaterEntry) -> Unit,
    onDelete: ((WaterEntry) -> Unit)? = null,
    framed: Boolean = true,
) {
    if (entries.isEmpty())
        Text("No sips recorded.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    else
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            entries.forEach { e ->
                key(e.id) {
                    if (onDelete != null) {
                        val delete by rememberUpdatedState(onDelete)
                        val dismiss = rememberSwipeToDismissBoxState()
                        LaunchedEffect(dismiss.currentValue) {
                            if (dismiss.currentValue != SwipeToDismissBoxValue.Settled) delete(e)
                        }
                        SwipeToDismissBox(
                            dismiss,
                            modifier = Modifier.testTag("sip-${e.id}"),
                            enableDismissFromStartToEnd = true,
                            backgroundContent = {
                                Row(
                                    Modifier.fillMaxSize()
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(MaterialTheme.colorScheme.errorContainer)
                                        .padding(horizontal = 20.dp),
                                    horizontalArrangement =
                                        if (
                                            dismiss.dismissDirection ==
                                                SwipeToDismissBoxValue.StartToEnd
                                        )
                                            Arrangement.Start
                                        else Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Outlined.DeleteOutline,
                                        null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Delete",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                            },
                        ) {
                            SipRow(e, s, onEntry, framed, onDelete)
                        }
                    } else SipRow(e, s, onEntry, framed, null)
                }
            }
        }
}

@Composable
private fun SipRow(
    e: WaterEntry,
    s: Settings,
    onEntry: (WaterEntry) -> Unit,
    framed: Boolean,
    onDelete: ((WaterEntry) -> Unit)?,
) {
    val system24 = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    Surface(
        shape = RoundedCornerShape(24.dp),
        color =
            if (framed) MaterialTheme.colorScheme.surface
            else androidx.compose.ui.graphics.Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth()
                .clickable { onEntry(e) }
                .padding(if (framed) 16.dp else 6.dp)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    if (onDelete != null)
                        customActions =
                            listOf(
                                CustomAccessibilityAction("Delete sip") {
                                    onDelete(e)
                                    true
                                }
                            )
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (framed) Symbol(Icons.Outlined.WaterDrop, size = 36)
            Column(Modifier.weight(1f)) {
                Text(e.name, style = MaterialTheme.typography.bodyMedium)
                Text(
                    formattedTime(e.timestamp, s.timeFormat, system24, ZoneId.of(e.zone)) +
                        if (e.zone != ZoneId.systemDefault().id) " · ${e.zone}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(Volume.format(e.volume, s.unit), style = MaterialTheme.typography.bodyMedium)
            Icon(Icons.Outlined.ChevronRight, "Edit sip")
        }
    }
}
