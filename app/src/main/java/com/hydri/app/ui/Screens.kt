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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.hydri.app.data.*
import com.hydri.app.domain.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    state: UiState,
    onLog: (Bottle) -> Unit,
    onCustom: () -> Unit,
    onEntry: (WaterEntry) -> Unit,
    onGoal: () -> Unit,
    onBottles: () -> Unit,
) {
    val s = state.settings
    val total = state.total(state.today)
    val goal = state.goal(state.today)
    ScreenColumn {
        val greeting =
            when (state.hour) {
                in 5..11 -> "Good morning"
                in 12..16 -> "Good afternoon"
                in 17..21 -> "Good evening"
                else -> "Good night"
            }
        PageHeader(
            "$greeting${if(s.name.isBlank()) "" else ", ${s.name}"}",
            "Make a little room for a little water.",
        )
        GlassCard(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Eyebrow("YOUR DAILY FLOW")
                TextButton(onClick = onGoal) {
                    Text("Edit goal", style = MaterialTheme.typography.labelMedium)
                }
            }
            WaterProgress(total, goal, s.unit, compact = true)
            Text(
                when {
                    total > goal ->
                        "Goal reached · ${Volume.format(total-goal,s.unit)} above your goal"
                    total == goal -> "Your daily goal is complete. Nicely done."
                    else -> "${Volume.format(goal-total,s.unit)} left to your daily goal"
                },
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column {
            SectionTitle("A sip, a tap.", "Manage", onBottles)
            Text(
                "Tap a bottle to log water",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            if (state.bottles.isEmpty())
                GlassCard {
                    Text("Your next sip starts here. Create a bottle or log a custom amount.")
                }
            state.bottles.chunked(2).forEach { row ->
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
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            OutlinedButton(onClick = onCustom, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Icon(Icons.Outlined.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Custom amount")
            }
        }
        Column {
            SectionTitle("Today's sips")
            EntryList(state.entriesOn(state.today), s, onEntry)
        }
        Text(
            "One day at a time. Your history stays safely on this device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun EntryList(entries: List<WaterEntry>, s: Settings, onEntry: (WaterEntry) -> Unit) {
    val system24 = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    if (entries.isEmpty())
        GlassCard(Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.WaterDrop, null, tint = MaterialTheme.colorScheme.primary)
            Text("A fresh start", fontWeight = FontWeight.SemiBold)
            Text(
                "No water logged for this day yet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    else
        GlassCard(Modifier.fillMaxWidth()) {
            entries.forEachIndexed { i, e ->
                Row(
                    Modifier.fillMaxWidth()
                        .clickable { onEntry(e) }
                        .padding(vertical = 6.dp)
                        .semantics(mergeDescendants = true) { role = Role.Button },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Symbol(Icons.Outlined.WaterDrop, size = 40)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.name, fontWeight = FontWeight.Medium)
                        Text(
                            formattedTime(e.timestamp, s.timeFormat, system24, ZoneId.of(e.zone)) +
                                if (e.zone != ZoneId.systemDefault().id) " · ${e.zone}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(Volume.format(e.volume, s.unit))
                    Icon(
                        Icons.Outlined.ChevronRight,
                        "Edit entry",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (i < entries.lastIndex)
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f)
                    )
            }
        }
}

@Composable
fun BottlesScreen(
    state: UiState,
    onEdit: (Bottle?) -> Unit,
    onFavorite: (Bottle) -> Unit,
    onMove: (Bottle, Int) -> Unit,
    onDelete: (Bottle) -> Unit,
) {
    ScreenColumn {
        PageHeader("Your daily companions.", "Good habits come in all shapes and sizes.")
        Button(onClick = { onEdit(null) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Outlined.Add, null)
            Text("Create a bottle")
        }
        if (state.bottles.isEmpty())
            GlassCard { Text("Add your favorite glass, mug or bottle to get started.") }
        state.bottles.forEachIndexed { index, b ->
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Symbol(bottleIcons[b.icon], color = accents[b.color], size = 64)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(b.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            Volume.format(b.volume, state.settings.unit),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onFavorite(b) }) {
                        Icon(
                            if (b.favorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                            if (b.favorite) "Unfavorite ${b.name}" else "Favorite ${b.name}",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row {
                        IconButton(onClick = { onMove(b, -1) }, enabled = index > 0) {
                            Icon(Icons.Outlined.ArrowUpward, "Move ${b.name} up")
                        }
                        IconButton(
                            onClick = { onMove(b, 1) },
                            enabled = index < state.bottles.lastIndex,
                        ) {
                            Icon(Icons.Outlined.ArrowDownward, "Move ${b.name} down")
                        }
                    }
                    Row {
                        TextButton(onClick = { onEdit(b) }) { Text("Edit") }
                        IconButton(onClick = { onDelete(b) }) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                "Delete ${b.name}",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
        Text(
            "Favorites appear in the medium home-screen widget. Reorder bottles to arrange your quick-add actions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun TrackerScreen(
    state: UiState,
    onGoal: () -> Unit,
    onEntry: (WaterEntry) -> Unit,
    onAdd: (LocalDate) -> Unit,
) {
    var selectedText by rememberSaveable { mutableStateOf(state.today.toString()) }
    var monthText by rememberSaveable { mutableStateOf(YearMonth.from(state.today).toString()) }
    var period by rememberSaveable { mutableStateOf("Week") }
    var selectedBar by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = LocalDate.parse(selectedText)
    val month = YearMonth.parse(monthText)
    val stats = state.statistics
    val s = state.settings
    val start = state.goals.minOfOrNull { it.date }?.let { LocalDate.parse(it) } ?: state.today
    ScreenColumn {
        PageHeader("Every drop adds up.", "Your rhythm, reflected over time.")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassCard(Modifier.weight(1f)) {
                Symbol(Icons.Outlined.LocalFireDepartment)
                Text("${stats.current} days", style = MaterialTheme.typography.titleLarge)
                Text("Current streak", style = MaterialTheme.typography.bodySmall)
            }
            GlassCard(Modifier.weight(1f)) {
                Symbol(Icons.Outlined.EmojiEvents)
                Text("${stats.longest} days", style = MaterialTheme.typography.titleLarge)
                Text("Longest streak", style = MaterialTheme.typography.bodySmall)
            }
        }
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { monthText = month.minusMonths(1).toString() }) {
                    Icon(Icons.Outlined.ChevronLeft, "Previous month")
                }
                Text(
                    month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(
                    onClick = { monthText = month.plusMonths(1).toString() },
                    enabled = month < YearMonth.from(state.today),
                ) {
                    Icon(Icons.Outlined.ChevronRight, "Next month")
                }
            }
            Row {
                (1..7).forEach { day ->
                    Text(
                        DayOfWeek.of(day).getDisplayName(TextStyle.NARROW, Locale.getDefault()),
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
                                    date > state.today -> "Future"
                                    date < start -> "Before tracking"
                                    total > goal -> "Exceeded"
                                    total >= goal -> "Completed"
                                    else -> "Incomplete"
                                }
                            val color =
                                when (status) {
                                    "Exceeded" -> MaterialTheme.colorScheme.tertiary
                                    "Completed" -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            Column(
                                Modifier.weight(1f)
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
                                    .clickable(enabled = date <= state.today) {
                                        selectedText = date.toString()
                                    }
                                    .semantics(mergeDescendants = true) {
                                        contentDescription =
                                            "${formattedDate(date,s.dateFormat)}, $status${if(date==state.today) ", Today" else ""}"
                                        this.selected = date == selected
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    number.toString(),
                                    color =
                                        if (date > state.today) color.copy(alpha = .35f) else color,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    when (status) {
                                        "Exceeded" -> "+"
                                        "Completed" -> "✓"
                                        "Incomplete" -> "·"
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
                "· Incomplete   ✓ Completed   + Exceeded\nOutlined: today. Blank: before tracking or future.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column {
            SectionTitle(formattedDate(selected, s.dateFormat), "+ Log", { onAdd(selected) })
            val total = state.total(selected)
            val goal = state.goal(selected)
            Text(
                "${Volume.format(total,s.unit)} / ${Volume.format(goal,s.unit)} · ${(total.toDouble()/goal*100).toInt()}%",
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                if (selected < start) "Before tracking began"
                else if (total > goal) "Goal exceeded"
                else if (total == goal) "Goal complete" else "Goal incomplete",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(12.dp))
            EntryList(state.entriesOn(selected), s, onEntry)
        }
        GlassCard(Modifier.fillMaxWidth()) {
            SectionTitle("Daily goal", "Change", onGoal)
            Text(
                Volume.format(state.goal(state.today), s.unit),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                "Historical goals stay with their original dates.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        GlassCard(Modifier.fillMaxWidth()) {
            SectionTitle("Your hydration trend")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Day", "Week", "Month").forEach {
                    Choice(it, period == it) {
                        period = it
                        selectedBar = null
                    }
                }
            }
            val dates =
                when (period) {
                    "Day" -> listOf(selected)
                    "Month" ->
                        (1..month.lengthOfMonth())
                            .map { month.atDay(it) }
                            .filter { it <= state.today }
                    else -> (6 downTo 0).map { state.today.minusDays(it.toLong()) }
                }
            val activeDates = dates.filter { it >= start }
            val sum = dates.sumOf { state.total(it) }
            val complete = activeDates.count { state.total(it) >= state.goal(it) }
            val max =
                maxOf(dates.maxOfOrNull { state.total(it) } ?: 0, state.goal(state.today)).toFloat()
            if (dates.isEmpty()) Text("This month has no recorded days yet.")
            else
                Row(
                    Modifier.fillMaxWidth().height(140.dp),
                    horizontalArrangement =
                        Arrangement.spacedBy(if (dates.size > 7) 3.dp else 8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    dates.forEach { date ->
                        val total = state.total(date)
                        val height = (total / max * 110).coerceAtLeast(3f)
                        Column(
                            Modifier.weight(1f)
                                .fillMaxHeight()
                                .clickable { selectedBar = date.toString() }
                                .semantics(mergeDescendants = true) {
                                    contentDescription =
                                        "${formattedDate(date,s.dateFormat)}, ${Volume.format(total,s.unit)}"
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
                                    date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                        }
                    }
                }
            selectedBar?.let { d ->
                Text(
                    "${formattedDate(LocalDate.parse(d),s.dateFormat)}: ${Volume.format(state.total(LocalDate.parse(d)),s.unit)}",
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                "${Volume.format(sum,s.unit)} total · ${Volume.format(if(activeDates.isEmpty()) 0 else sum/activeDates.size,s.unit)} daily average"
            )
            Text(
                "$complete of ${activeDates.size} days complete · ${if(activeDates.isEmpty()) 0 else complete*100/activeDates.size}% completion",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "${stats.completed} completed days overall. Today counts in averages. Tap a bar for details.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Little milestones")
            Text(
                "Consistency earns badges. Additional water earns no extra credit. Editing history recalculates unlocks and their dates.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                            Text(badge.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Reach your daily goal ${badge.days} ${if(badge.days==1) "day" else "consecutive days"}.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                badge.unlocked?.let { "Unlocked ${formattedDate(it,s.dateFormat)}" }
                                    ?: "Locked · ${minOf(stats.longest,badge.days)} / ${badge.days} days",
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
