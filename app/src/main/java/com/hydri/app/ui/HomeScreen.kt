// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0

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
import com.hydri.app.R
import com.hydri.app.data.*
import com.hydri.app.domain.*
import com.hydri.app.i18n.*
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
    ScreenColumn(scrollKey = "Home") {
        val greeting =
            when (state.hour) {
                in 5..11 -> tr(R.string.good_morning)
                in 12..16 -> tr(R.string.good_afternoon)
                in 17..21 -> tr(R.string.good_evening)
                else -> tr(R.string.good_night)
            }
        PageHeader(if (s.name.isBlank()) greeting else tr(R.string.greeting_name, greeting, s.name))
        GlassCard(Modifier.fillMaxWidth()) {
            Eyebrow(tr(R.string.daily_flow))
            WaterProgress(total, goal, s.unit, compact = true)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onUndo,
                    enabled = entries.isNotEmpty(),
                    modifier = Modifier.weight(1f).testTag("daily-undo"),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Outlined.Undo, null)
                    Spacer(Modifier.width(6.dp))
                    Text(tr(R.string.undo))
                }
                OutlinedButton(
                    onReset,
                    enabled = entries.isNotEmpty(),
                    modifier = Modifier.weight(1f).testTag("daily-reset"),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Outlined.RestartAlt, null)
                    Spacer(Modifier.width(6.dp))
                    Text(tr(R.string.reset))
                }
            }
            Text(
                when {
                    total > goal ->
                        tr(R.string.value_above_goal, Volume.format(total - goal, s.unit))
                    total == goal -> tr(R.string.goal_reached)
                    else -> tr(R.string.value_remaining, Volume.format(goal - total, s.unit))
                },
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Column {
            SectionTitle(tr(R.string.quick_add), tr(R.string.bottles), onBottles)
            if (favorites.isEmpty())
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(tr(R.string.no_favorite_bottles))
                    TextButton(onBottles) { Text(tr(R.string.choose_favorites_in_bottles)) }
                }
            favorites.chunked(2).forEach { row ->
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { b ->
                        GlassCard(
                            Modifier.weight(1f)
                                .clip(RoundedCornerShape(28.dp))
                                .clickable { onLog(b) }
                                .semantics(mergeDescendants = true) {
                                    role = Role.Button
                                    contentDescription =
                                        tr(
                                            R.string.log_value_value,
                                            bottleDisplayName(b),
                                            Volume.format(b.volume, s.unit),
                                        )
                                }
                        ) {
                            Symbol(bottleIcons[b.icon], color = accents[b.color])
                            Text(bottleDisplayName(b), fontWeight = FontWeight.Medium)
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
                Text(tr(R.string.custom_amount))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(tr(R.string.today_s_sips))
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
        Text(tr(R.string.no_sips_recorded), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                        tr(R.string.delete),
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
                                CustomAccessibilityAction(tr(R.string.delete_sip)) {
                                    onDelete(e)
                                    true
                                }
                            )
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (framed)
                Symbol(
                    e.bottleIcon?.let { bottleIcons.getOrNull(it) } ?: Icons.Outlined.WaterDrop,
                    label =
                        e.bottleIcon
                            ?.let { bottleNames.getOrNull(it) }
                            ?.let { tr(R.string.value_sip, it) }
                            ?: if (e.name == "Custom water") tr(R.string.custom_water_sip)
                            else tr(R.string.water_sip),
                    color = e.bottleColor?.let { accents.getOrNull(it) } ?: accents[0],
                    size = 36,
                )
            Column(Modifier.weight(1f)) {
                Text(entryDisplayName(e), style = MaterialTheme.typography.bodyMedium)
                Text(
                    formattedTime(e.timestamp, s.timeFormat, system24, ZoneId.of(e.zone)) +
                        if (e.zone != ZoneId.systemDefault().id) " · ${e.zone}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(Volume.format(e.volume, s.unit), style = MaterialTheme.typography.bodyMedium)
            Icon(Icons.Outlined.ChevronRight, tr(R.string.edit_sip))
        }
    }
}
