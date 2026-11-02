// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0

package com.hydri.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.hydri.app.R
import com.hydri.app.data.*
import com.hydri.app.domain.*
import com.hydri.app.i18n.*
import java.time.*

@Composable
fun BottlesScreen(
    state: UiState,
    onEdit: (Bottle?) -> Unit,
    onFavorite: (Bottle) -> Unit,
    onMove: (Bottle, Int) -> Unit,
    onDelete: (Bottle) -> Unit,
) {
    ScreenColumn(scrollKey = "Bottles") {
        PageHeader(tr(R.string.bottles))
        Button(onClick = { onEdit(null) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Outlined.Add, null)
            Text(tr(R.string.create_a_bottle))
        }
        if (state.bottles.isEmpty())
            GlassCard { Text(tr(R.string.add_your_favorite_glass_mug_or_bottle_to_get_started)) }
        state.bottles.forEachIndexed { index, b ->
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Symbol(bottleIcons[b.icon], color = accents[b.color], size = 64)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(bottleDisplayName(b), style = MaterialTheme.typography.titleMedium)
                        Text(
                            Volume.format(b.volume, state.settings.unit),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onFavorite(b) }) {
                        Icon(
                            if (b.favorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                            if (b.favorite) tr(R.string.unfavorite_value, bottleDisplayName(b))
                            else tr(R.string.favorite_value, bottleDisplayName(b)),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row {
                        IconButton(onClick = { onMove(b, -1) }, enabled = index > 0) {
                            Icon(
                                Icons.Outlined.ArrowUpward,
                                tr(R.string.move_value_up, bottleDisplayName(b)),
                            )
                        }
                        IconButton(
                            onClick = { onMove(b, 1) },
                            enabled = index < state.bottles.lastIndex,
                        ) {
                            Icon(
                                Icons.Outlined.ArrowDownward,
                                tr(R.string.move_value_down, bottleDisplayName(b)),
                            )
                        }
                    }
                    Row {
                        TextButton(onClick = { onEdit(b) }) { Text(tr(R.string.edit)) }
                        IconButton(onClick = { onDelete(b) }) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                tr(R.string.delete_value, bottleDisplayName(b)),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
        Text(
            tr(R.string.favorites_appear_on_home_and_in_the_widget),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun TrackerScreen(state: UiState, onEntry: (WaterEntry) -> Unit) {
    var selectedText by rememberSaveable { mutableStateOf(state.today.toString()) }
    var monthText by rememberSaveable { mutableStateOf(YearMonth.from(state.today).toString()) }
    var period by rememberSaveable { mutableStateOf("Week") }
    var selectedBar by rememberSaveable { mutableStateOf<String?>(null) }
    val activePeriod = TrackerRanges.period(period)
    LaunchedEffect(period) {
        if (period != activePeriod) {
            period = activePeriod
            selectedBar = null
        }
    }
    val selected = LocalDate.parse(selectedText)
    val month = YearMonth.parse(monthText)
    val stats = state.statistics
    val s = state.settings
    val start = state.goals.minOfOrNull { it.date }?.let { LocalDate.parse(it) } ?: state.today
    ScreenColumn(scrollKey = "Tracker") {
        PageHeader(tr(R.string.tracker))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassCard(Modifier.weight(1f)) {
                Symbol(Icons.Outlined.LocalFireDepartment)
                Text(
                    plural(R.plurals.days_count, stats.current),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(tr(R.string.current_streak), style = MaterialTheme.typography.bodySmall)
            }
            GlassCard(Modifier.weight(1f)) {
                Symbol(Icons.Outlined.EmojiEvents)
                Text(
                    plural(R.plurals.days_count, stats.longest),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(tr(R.string.longest_streak), style = MaterialTheme.typography.bodySmall)
            }
        }
        TrackerCalendar(
            state,
            month,
            selected,
            onMonth = {
                monthText = it.toString()
                if (activePeriod == "Month") selectedBar = null
            },
            onSelect = { selectedText = it.toString() },
        )
        SipHistory(state, onEntry)
        GlassCard(Modifier.fillMaxWidth()) {
            SectionTitle(tr(R.string.intake))
            Row(
                Modifier.fillMaxWidth().testTag("intake-range"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("Week", "Month").forEach { value ->
                    FilterChip(
                        selected = activePeriod == value,
                        onClick = {
                            period = value
                            selectedBar = null
                        },
                        modifier = Modifier.weight(1f).testTag("intake-range-$value"),
                        label = {
                            Text(
                                displaySetting(value),
                                Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        },
                    )
                }
            }
            val dates = TrackerRanges.dates(activePeriod, month, state.today)
            val activeDates = dates.filter { it >= start }
            val sum = dates.sumOf { state.total(it) }
            val complete = activeDates.count { state.total(it) >= state.goal(it) }
            val max =
                maxOf(dates.maxOfOrNull { state.total(it) } ?: 0, state.goal(state.today)).toFloat()
            if (dates.isEmpty()) Text(tr(R.string.this_month_has_no_recorded_days_yet))
            else
                Row(
                    Modifier.fillMaxWidth().height(140.dp).testTag("intake-bars"),
                    horizontalArrangement =
                        Arrangement.spacedBy(if (dates.size > 7) 3.dp else 8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    dates.forEach { date ->
                        val total = state.total(date)
                        val height = (total / max * 110).coerceAtLeast(3f)
                        Column(
                            Modifier.weight(1f)
                                .testTag("intake-bar-$date")
                                .fillMaxHeight()
                                .clickable { selectedBar = date.toString() }
                                .semantics(mergeDescendants = true) {
                                    contentDescription =
                                        tr(
                                            R.string.value_value_238,
                                            formattedDate(date, s.dateFormat),
                                            Volume.format(total, s.unit),
                                        )
                                },
                            verticalArrangement = Arrangement.Bottom,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                Modifier.fillMaxWidth()
                                    .height(height.dp)
                                    .background(
                                        if (selectedBar == date.toString())
                                            MaterialTheme.colorScheme.tertiary
                                        else
                                            MaterialTheme.colorScheme.primary.copy(
                                                alpha = if (total >= state.goal(date)) 1f else .45f
                                            ),
                                        RoundedCornerShape(5.dp),
                                    )
                            )
                            if (dates.size <= 7)
                                Text(
                                    AppText.number(date.dayOfMonth),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                        }
                    }
                }
            selectedBar?.let { d ->
                Text(
                    tr(
                        R.string.value_value,
                        formattedDate(LocalDate.parse(d), s.dateFormat),
                        Volume.format(state.total(LocalDate.parse(d)), s.unit),
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                tr(
                    R.string.value_total_value_daily_average,
                    Volume.format(sum, s.unit),
                    Volume.format(if (activeDates.isEmpty()) 0 else sum / activeDates.size, s.unit),
                )
            )
            Text(
                plural(
                    R.plurals.days_completion,
                    activeDates.size,
                    complete,
                    activeDates.size,
                    if (activeDates.isEmpty()) 0 else complete * 100 / activeDates.size,
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(tr(R.string.badges))

            stats.badges.forEachIndexed { index, badge ->
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Symbol(
                            listOf(
                                Icons.Outlined.WaterDrop,
                                Icons.Outlined.Spa,
                                Icons.Outlined.Star,
                                Icons.Outlined.WorkspacePremium,
                                Icons.Outlined.EmojiEvents,
                                Icons.Outlined.Diamond,
                            )[index],
                            color =
                                if (badge.unlocked != null) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tr(badge.name), fontWeight = FontWeight.SemiBold)
                            Text(
                                if (badge.days == 1) tr(R.string.reach_your_goal_once)
                                else plural(R.plurals.reach_goal_days, badge.days),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                badge.unlocked?.let {
                                    tr(R.string.unlocked_value, formattedDate(it, s.dateFormat))
                                }
                                    ?: plural(
                                        R.plurals.locked_days,
                                        badge.days,
                                        minOf(stats.longest, badge.days),
                                        badge.days,
                                    ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { minOf(stats.longest.toFloat() / badge.days, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
fun SipHistory(state: UiState, onEntry: (WaterEntry) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(listOf<String>()) }
    val duration = if (LocalReducedMotion.current) 0 else 180
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(tr(R.string.sip_history))
        val recent = EntryPolicy.recentHistory(state.entries, state.today)
        if (recent.isEmpty())
            Text(tr(R.string.no_sips_recorded), color = MaterialTheme.colorScheme.onSurfaceVariant)
        recent.forEach { (date, entries) ->
            GlassCard(Modifier.fillMaxWidth()) {
                val open = date in expanded
                Row(
                    Modifier.fillMaxWidth()
                        .testTag("history-$date")
                        .clickable { expanded = if (open) expanded - date else expanded + date }
                        .semantics(mergeDescendants = true) {
                            stateDescription =
                                if (open) tr(R.string.expanded) else tr(R.string.collapsed)
                            role = Role.Button
                            contentDescription =
                                tr(
                                    R.string.history_value,
                                    formattedDate(LocalDate.parse(date), state.settings.dateFormat),
                                )
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        formattedDate(LocalDate.parse(date), state.settings.dateFormat),
                        Modifier.weight(1f),
                    )
                    Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
                }
                androidx.compose.animation.AnimatedVisibility(
                    open,
                    enter =
                        androidx.compose.animation.expandVertically(
                            androidx.compose.animation.core.tween(duration)
                        ) +
                            androidx.compose.animation.fadeIn(
                                androidx.compose.animation.core.tween(duration)
                            ),
                    exit =
                        androidx.compose.animation.shrinkVertically(
                            androidx.compose.animation.core.tween(duration)
                        ) +
                            androidx.compose.animation.fadeOut(
                                androidx.compose.animation.core.tween(duration)
                            ),
                ) {
                    Column {
                        Text(
                            Volume.format(entries.sumOf { it.volume }, state.settings.unit),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        EntryList(
                            entries,
                            state.settings,
                            onEntry,
                            framed = false,
                            today = state.today,
                        )
                    }
                }
            }
        }
    }
}
