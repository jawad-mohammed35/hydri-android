// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0
package com.hydri.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hydri.app.R
import com.hydri.app.domain.*
import com.hydri.app.i18n.*
import java.time.*
import java.time.format.TextStyle
import kotlin.math.abs

val CalendarMonth = SemanticsPropertyKey<String>("CalendarMonth")

@Composable
fun TrackerCalendar(
    state: UiState,
    month: YearMonth,
    selected: LocalDate,
    onMonth: (YearMonth) -> Unit,
    onSelect: (LocalDate) -> Unit,
) {
    val s = state.settings
    val start = state.goals.minOfOrNull { it.date }?.let(LocalDate::parse) ?: state.today
    val transition = updateTransition(month, label = "Calendar month")
    val busy = transition.isRunning
    val duration = if (LocalReducedMotion.current) 0 else 240
    fun navigate(direction: Int) {
        if (!transition.isRunning) onMonth(TrackerRanges.stepMonth(month, direction, state.today))
    }
    GlassCard(
        Modifier.fillMaxWidth()
            .testTag("tracker-calendar")
            .semantics { this[CalendarMonth] = month.toString() }
            .pointerInput(month, state.today, busy) {
                var distance = 0f
                detectHorizontalDragGestures(
                    onDragStart = { distance = 0f },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        distance += amount
                    },
                    onDragCancel = { distance = 0f },
                    onDragEnd = {
                        if (abs(distance) >= maxOf(48.dp.toPx(), size.width * .18f))
                            navigate(if (distance < 0f) 1 else -1)
                    },
                )
            }
    ) {
        transition.AnimatedContent(
            modifier = Modifier.fillMaxWidth().clipToBounds(),
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(duration, easing = FastOutSlowInEasing)) {
                        direction * it
                    } + fadeIn(tween(duration)))
                    .togetherWith(
                        slideOutHorizontally(tween(duration, easing = FastOutSlowInEasing)) {
                            -direction * it
                        } + fadeOut(tween(duration))
                    )
                    .using(
                        SizeTransform(clip = true, sizeAnimationSpec = { _, _ -> tween(duration) })
                    )
            },
        ) { month ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navigate(-1) }, enabled = !busy) {
                        Icon(Icons.Outlined.ChevronLeft, tr(R.string.previous_month))
                    }
                    Text(
                        formattedMonth(month),
                        Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(
                        onClick = { navigate(1) },
                        enabled = !busy && month < YearMonth.from(state.today),
                    ) {
                        Icon(Icons.Outlined.ChevronRight, tr(R.string.next_month))
                    }
                }
                Row {
                    (1..7).forEach { day ->
                        Text(
                            DayOfWeek.of(day).getDisplayName(TextStyle.NARROW, AppText.locale),
                            Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                val offset = month.atDay(1).dayOfWeek.value - 1
                val cells = (offset + month.lengthOfMonth() + 6) / 7 * 7
                (0 until cells step 7).forEach { row ->
                    Row {
                        (row until row + 7).forEach { cell ->
                            val number = cell - offset + 1
                            if (number !in 1..month.lengthOfMonth())
                                Spacer(Modifier.weight(1f).height(48.dp))
                            else {
                                val date = month.atDay(number)
                                val total = state.total(date)
                                val goal = state.goal(date)
                                val status =
                                    when {
                                        date > state.today -> tr(R.string.future)
                                        date < start -> tr(R.string.before_tracking)
                                        total > goal -> tr(R.string.exceeded)
                                        total >= goal -> tr(R.string.completed)
                                        else -> tr(R.string.incomplete)
                                    }
                                val color =
                                    when (status) {
                                        tr(R.string.exceeded) -> MaterialTheme.colorScheme.tertiary
                                        tr(R.string.completed) -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                Column(
                                    Modifier.weight(1f)
                                        .testTag("calendar-day-$date")
                                        .heightIn(min = 48.dp)
                                        .padding(2.dp)
                                        .background(
                                            if (date == selected)
                                                MaterialTheme.colorScheme.primary.copy(alpha = .16f)
                                            else Color.Transparent,
                                            RoundedCornerShape(12.dp),
                                        )
                                        .then(
                                            if (date == state.today)
                                                Modifier.border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.primary,
                                                    RoundedCornerShape(12.dp),
                                                )
                                            else Modifier
                                        )
                                        .clickable(enabled = date <= state.today) { onSelect(date) }
                                        .semantics(mergeDescendants = true) {
                                            contentDescription =
                                                tr(
                                                    R.string.value_value_value,
                                                    formattedDate(date, s.dateFormat),
                                                    status,
                                                    if (date == state.today)
                                                        tr(R.string.today_suffix)
                                                    else "",
                                                )
                                            this.selected = date == selected
                                            stateDescription =
                                                tr(
                                                    R.string.value_of_value_value_percent,
                                                    Volume.format(total, s.unit),
                                                    Volume.format(goal, s.unit),
                                                    (total.toDouble() / goal * 100).toInt(),
                                                )
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Text(
                                        AppText.number(number),
                                        color =
                                            if (date > state.today) color.copy(alpha = .35f)
                                            else color,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        when (status) {
                                            tr(R.string.exceeded) -> "+"
                                            tr(R.string.completed) -> "✓"
                                            tr(R.string.incomplete) -> "·"
                                            else -> " "
                                        },
                                        color = color,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        }
                    }
                }
                Text(
                    tr(R.string.incomplete_completed_exceeded_noutlined_today_blank_before),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
