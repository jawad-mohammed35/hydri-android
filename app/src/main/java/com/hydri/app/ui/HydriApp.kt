package com.hydri.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.hydri.app.data.*
import com.hydri.app.domain.*
import java.time.*
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
fun HydriApp(vm: HydriViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var bottleEditor by remember { mutableStateOf<Bottle?>(null) }
    var bottleOpen by remember { mutableStateOf(false) }
    var entryEditor by remember { mutableStateOf<WaterEntry?>(null) }
    var entryOpen by remember { mutableStateOf(false) }
    var entryDate by remember { mutableStateOf(LocalDate.now()) }
    var goalOpen by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted)
                scope.launch {
                    snackbar.showSnackbar(
                        "Notifications are blocked. Enable them in Android app settings to receive reminders."
                    )
                }
        }
    fun requestNotifications() {
        if (
            Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
        )
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    LaunchedEffect(vm) {
        vm.messages.collect { (message, entry) ->
            launch {
                snackbar.currentSnackbarData?.dismiss()
                if (
                    snackbar.showSnackbar(
                        message,
                        if (entry != null) "Undo" else null,
                        withDismissAction = true,
                        duration = SnackbarDuration.Short,
                    ) == SnackbarResult.ActionPerformed && entry != null
                )
                    vm.undo(entry)
            }
        }
    }
    HydriTheme(state.settings) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (!state.ready)
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            else if (!state.settings.onboarded) Onboarding(state, vm, ::requestNotifications)
            else {
                val nav = rememberNavController()
                val backStack by nav.currentBackStackEntryAsState()
                val route = backStack?.destination?.route ?: "Home"
                Scaffold(
                    containerColor = Color.Transparent,
                    snackbarHost = { SnackbarHost(snackbar) },
                    bottomBar = {
                        Surface(
                            Modifier.navigationBarsPadding()
                                .padding(horizontal = 24.dp, vertical = 10.dp)
                                .fillMaxWidth()
                                .widthIn(max = 640.dp),
                            shape = RoundedCornerShape(32.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = .92f),
                            tonalElevation = 3.dp,
                            border =
                                BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = .12f),
                                ),
                        ) {
                            Row(
                                Modifier.padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                val tabs =
                                    listOf(
                                        "Home" to Icons.Outlined.Home,
                                        "Bottles" to Icons.Outlined.WaterDrop,
                                        "Tracker" to Icons.Outlined.CalendarMonth,
                                        "More" to Icons.Outlined.GridView,
                                    )
                                tabs.forEach { (name, icon) ->
                                    val selected = route == name
                                    val color by
                                        animateColorAsState(
                                            if (selected)
                                                MaterialTheme.colorScheme.primary.copy(alpha = .12f)
                                            else Color.Transparent,
                                            animationSpec =
                                                tween(if (LocalReducedMotion.current) 0 else 180),
                                            label = "Selected tab",
                                        )
                                    Column(
                                        Modifier.weight(1f)
                                            .clip(RoundedCornerShape(26.dp))
                                            .background(color)
                                            .selectableTab(selected, name) {
                                                if (state.settings.haptics)
                                                    haptic.performHapticFeedback(
                                                        HapticFeedbackType.TextHandleMove
                                                    )
                                                nav.navigate(name) {
                                                    popUpTo(nav.graph.startDestinationId) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                            .padding(vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Icon(
                                            icon,
                                            null,
                                            tint =
                                                if (selected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color =
                                                if (selected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    },
                ) { padding ->
                    val transitionDuration = if (LocalReducedMotion.current) 0 else 180
                    NavHost(
                        nav,
                        "Home",
                        Modifier.padding(padding),
                        enterTransition = { fadeIn(tween(transitionDuration)) },
                        exitTransition = { fadeOut(tween(transitionDuration)) },
                    ) {
                        composable("Home") {
                            HomeScreen(
                                state,
                                onLog = { b ->
                                    if (state.settings.haptics)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    vm.log(b.volume, b.name)
                                },
                                onCustom = {
                                    entryEditor = null
                                    entryDate = state.today
                                    entryOpen = true
                                },
                                onEntry = {
                                    entryEditor = it
                                    entryOpen = true
                                },
                                onGoal = { goalOpen = true },
                                onBottles = { nav.navigate("Bottles") },
                            )
                        }
                        composable("Bottles") {
                            BottlesScreen(
                                state,
                                { b ->
                                    bottleEditor = b
                                    bottleOpen = true
                                },
                                { b -> vm.bottle(b.copy(favorite = !b.favorite)) },
                                vm::move,
                                { b ->
                                    confirm =
                                        "Delete ${b.name}? Saved water entries will stay in your history." to
                                            {
                                                vm.deleteBottle(b)
                                            }
                                },
                            )
                        }
                        composable("Tracker") {
                            TrackerScreen(
                                state,
                                onGoal = { goalOpen = true },
                                onEntry = {
                                    entryEditor = it
                                    entryOpen = true
                                },
                                onAdd = { date ->
                                    entryEditor = null
                                    entryDate = date
                                    entryOpen = true
                                },
                            )
                        }
                        composable("More") {
                            MoreScreen(
                                state,
                                vm,
                                ::requestNotifications,
                                { goalOpen = true },
                                { message, action -> confirm = message to action },
                                snackbar,
                            )
                        }
                    }
                }
            }
            if (bottleOpen)
                BottleDialog(
                    bottleEditor,
                    state.settings.unit,
                    state.bottles.size,
                    { bottleOpen = false },
                ) {
                    vm.bottle(it)
                    bottleOpen = false
                }
            if (entryOpen)
                EntryDialog(
                    entryEditor,
                    entryDate,
                    state.settings,
                    { entryOpen = false },
                    onDelete = { e ->
                        confirm =
                            "Delete this water entry?" to
                                {
                                    vm.delete(e)
                                    entryOpen = false
                                }
                    },
                ) { volume, date, time ->
                    val editing = entryEditor
                    if (editing == null) vm.log(volume, "Custom water", date, time)
                    else
                        vm.action {
                            if (vm.repo.dao.allGoals().none { it.date <= date.toString() })
                                vm.repo.dao.putGoal(Goal(date.toString(), state.goal(date)))
                            vm.repo.dao.putEntry(
                                editing.copy(
                                    volume = volume,
                                    date = date.toString(),
                                    timestamp = time,
                                )
                            )
                        }
                    entryOpen = false
                }
            if (goalOpen)
                AmountDialog(
                    "Daily water goal",
                    state.goal(state.today),
                    state.settings.unit,
                    "A general starting point, not personalized medical advice. Changes apply from today; earlier goals stay the same.",
                    { goalOpen = false },
                ) {
                    vm.goal(it)
                    goalOpen = false
                }
            confirm?.let { (message, action) ->
                AlertDialog(
                    onDismissRequest = { confirm = null },
                    title = { Text("Please confirm") },
                    text = { Text(message) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                confirm = null
                                action()
                            }
                        ) {
                            Text("Confirm")
                        }
                    },
                    dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } },
                )
            }
        }
    }
}

private fun Modifier.selectableTab(selected: Boolean, name: String, onClick: () -> Unit) =
    this.semantics {
            role = Role.Tab
            this.selected = selected
            contentDescription = name
        }
        .clickable(onClick = onClick)

@Composable
fun Onboarding(state: UiState, vm: HydriViewModel, requestNotifications: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    var s by remember(state.settings) { mutableStateOf(state.settings) }
    var goalText by rememberSaveable {
        mutableStateOf(Volume.input(state.goal(state.today), s.unit))
    }
    var goalUnit by rememberSaveable { mutableStateOf(s.unit) }
    var customBottle by remember { mutableStateOf(false) }
    val titles =
        listOf(
            "Small sips.\nBetter days.",
            "Make it yours.",
            "Your way to measure.",
            "Find your daily rhythm.",
            "Meet your everyday bottles.",
            "A gentle nudge.",
            "A space that feels like you.",
        )
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            Column(Modifier.navigationBarsPadding().padding(24.dp)) {
                Button(
                    onClick = {
                        if (page == 6) {
                            vm.settings(s.copy(onboarded = true))
                            vm.action {
                                vm.repo.goal(Volume.parse(goalText, goalUnit) ?: 2_500_000)
                            }
                        } else page++
                    },
                    enabled = page != 3 || Volume.parse(goalText, goalUnit) != null,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                ) {
                    Text(
                        if (page == 0) "Let's begin"
                        else if (page == 6) "Start hydrating" else "Continue"
                    )
                }
                if (page > 0)
                    TextButton(onClick = { page-- }, modifier = Modifier.fillMaxWidth()) {
                        Text("Back")
                    }
            }
        },
    ) { padding ->
        Column(
            Modifier.padding(padding)
                .statusBarsPadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(7) {
                    Box(
                        Modifier.weight(1f)
                            .height(4.dp)
                            .background(
                                if (it <= page) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(3.dp),
                            )
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Symbol(Icons.Outlined.WaterDrop, size = 80)
            Eyebrow("HYDRI / ${page+1} OF 7")
            Text(titles[page], style = MaterialTheme.typography.headlineLarge)
            when (page) {
                0 -> {
                    Text(
                        "A little water, a little more often. Build a hydration habit that fits your life."
                    )
                    WaterProgress(750_000, 2_500_000, "mL")
                    Text(
                        "Private by nature. Entirely offline. Always yours.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                1 -> {
                    Text("What should we call you? A nickname is perfect.")
                    OutlinedTextField(
                        s.name,
                        { s = s.copy(name = it.take(60)) },
                        label = { Text("Your name (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                2 -> {
                    Text("All measurements convert accurately. Change this any time.")
                    listOf(
                            "mL" to "Metric · millilitres",
                            "L" to "Metric · litres",
                            "oz" to "Imperial · US fluid ounces",
                        )
                        .forEach { (unit, label) ->
                            Choice(label, s.unit == unit) {
                                val amount = Volume.parse(goalText, goalUnit) ?: 2_500_000
                                s = s.copy(unit = unit)
                                goalText = Volume.input(amount, unit)
                                goalUnit = unit
                            }
                        }
                }
                3 -> {
                    Text(
                        "Choose a comfortable starting point. Hydration needs vary; suggested goals are not personalized medical advice."
                    )
                    listOf(1_500_000L, 2_000_000L, 2_500_000L).forEach { value ->
                        Choice(
                            Volume.format(value, s.unit),
                            Volume.parse(goalText, goalUnit) == value,
                        ) {
                            goalText = Volume.input(value, s.unit)
                            goalUnit = s.unit
                        }
                    }
                    VolumeField(goalText, { goalText = it }, goalUnit)
                    Text(
                        "Track drinking water here. Reference values for total water also include food and other beverages.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                4 -> {
                    Text(
                        "These presets are ready for one-tap logging. Tap a star to pick favorites, or add your own."
                    )
                    state.bottles.forEach { b ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${b.name} · ${Volume.format(b.volume,s.unit)}",
                                Modifier.weight(1f),
                            )
                            IconButton(onClick = { vm.bottle(b.copy(favorite = !b.favorite)) }) {
                                Icon(
                                    if (b.favorite) Icons.Outlined.Star
                                    else Icons.Outlined.StarBorder,
                                    "Favorite ${b.name}",
                                )
                            }
                        }
                    }
                    OutlinedButton(onClick = { customBottle = true }) { Text("Create a bottle") }
                }
                5 -> {
                    Text(
                        "Optional reminders work offline during your chosen hours. Android may delay delivery to save battery."
                    )
                    SettingToggle("Hydration reminders", "Every 2 hours, 8:00–21:00", s.reminders) {
                        s = s.copy(reminders = it)
                        if (it) requestNotifications()
                    }
                    Text("You can fine-tune days, quiet hours and frequency in More.")
                }
                6 -> {
                    Text("Choose an appearance. More customization is waiting in Settings.")
                    listOf("System", "Light", "Dark").forEach {
                        Choice(it, s.theme == it) { s = s.copy(theme = it) }
                    }
                    SettingToggle("OLED pure black", value = s.oled) { s = s.copy(oled = it) }
                }
            }
        }
    }
    if (customBottle)
        BottleDialog(null, s.unit, state.bottles.size, { customBottle = false }) {
            vm.bottle(it)
            customBottle = false
        }
}

@Composable
fun VolumeField(text: String, onChange: (String) -> Unit, unit: String) {
    OutlinedTextField(
        text,
        onChange,
        label = { Text("Amount ($unit)") },
        singleLine = true,
        isError = text.isNotBlank() && Volume.parse(text, unit) == null,
        supportingText = {
            if (text.isNotBlank() && Volume.parse(text, unit) == null)
                Text("Enter an amount from 1 to 20,000 mL (or equivalent).")
        },
        keyboardOptions =
            androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
            ),
        modifier = Modifier.fillMaxWidth(),
    )
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(help)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(1_500_000L, 2_000_000L, 2_500_000L).forEach { amount ->
                        Choice(Volume.format(amount, unit), Volume.parse(text, unit) == amount) {
                            text = Volume.input(amount, unit)
                            dirty = true
                        }
                    }
                }
                VolumeField(
                    text,
                    {
                        text = it
                        dirty = true
                    },
                    unit,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(if (dirty) Volume.parse(text, unit)!! else initial) },
                enabled = Volume.parse(text, unit) != null,
            ) {
                Text("Save goal")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (bottle == null) "Create a bottle" else "Edit bottle") },
        text = {
            Column(
                Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Symbol(bottleIcons[icon], color = accents[color], size = 72)
                }
                OutlinedTextField(
                    name,
                    { name = it.take(60) },
                    label = { Text("Bottle name") },
                    singleLine = true,
                )
                VolumeField(
                    volume,
                    {
                        volume = it
                        dirty = true
                    },
                    unit,
                )
                Text("Container")
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    bottleIcons.forEachIndexed { i, image ->
                        IconButton(
                            onClick = { icon = i },
                            modifier =
                                Modifier.background(
                                    if (icon == i)
                                        MaterialTheme.colorScheme.primary.copy(alpha = .15f)
                                    else Color.Transparent,
                                    RoundedCornerShape(14.dp),
                                ),
                        ) {
                            Icon(image, bottleNames[i])
                        }
                    }
                }
                Text("Color")
                AccentPicker(color) { color = it }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        Bottle(
                            bottle?.id ?: UUID.randomUUID().toString(),
                            name.trim(),
                            if (!dirty && bottle != null) bottle.volume
                            else Volume.parse(volume, unit)!!,
                            icon,
                            color,
                            bottle?.favorite ?: true,
                            bottle?.position ?: position,
                        )
                    )
                },
                enabled = name.isNotBlank() && Volume.parse(volume, unit) != null,
            ) {
                Text("Save bottle")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun EntryDialog(
    entry: WaterEntry?,
    defaultDate: LocalDate,
    s: com.hydri.app.data.Settings,
    onDismiss: () -> Unit,
    onDelete: (WaterEntry) -> Unit,
    onSave: (Long, LocalDate, Long) -> Unit,
) {
    val context = LocalContext.current
    val zone = remember(entry) { entry?.zone?.let { ZoneId.of(it) } ?: ZoneId.systemDefault() }
    var amount by remember { mutableStateOf(Volume.input(entry?.volume ?: 250_000, s.unit)) }
    var dirty by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf(entry?.date?.let { LocalDate.parse(it) } ?: defaultDate) }
    var time by remember {
        mutableStateOf(
            entry?.let { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalTime() }
                ?: LocalTime.now()
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (entry == null) "Log water" else "Edit water entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                VolumeField(
                    amount,
                    {
                        amount = it
                        dirty = true
                    },
                    s.unit,
                )
                OutlinedButton(
                    onClick = {
                        android.app
                            .DatePickerDialog(
                                context,
                                { _, y, m, d -> date = LocalDate.of(y, m + 1, d) },
                                date.year,
                                date.monthValue - 1,
                                date.dayOfMonth,
                            )
                            .apply { datePicker.maxDate = System.currentTimeMillis() }
                            .show()
                    }
                ) {
                    Icon(Icons.Outlined.CalendarMonth, null)
                    Text(formattedDate(date, s.dateFormat))
                }
                OutlinedButton(
                    onClick = {
                        android.app
                            .TimePickerDialog(
                                context,
                                { _, h, m -> time = LocalTime.of(h, m) },
                                time.hour,
                                time.minute,
                                s.timeFormat == "24-hour" ||
                                    (s.timeFormat == "System" &&
                                        android.text.format.DateFormat.is24HourFormat(context)),
                            )
                            .show()
                    }
                ) {
                    Icon(Icons.Outlined.Schedule, null)
                    Text(
                        formattedTime(
                            time.atDate(date).atZone(zone).toInstant().toEpochMilli(),
                            s.timeFormat,
                            android.text.format.DateFormat.is24HourFormat(context),
                            zone,
                        )
                    )
                }
                Text("Time zone: ${zone.id}", style = MaterialTheme.typography.bodySmall)
                if (entry != null)
                    TextButton(onClick = { onDelete(entry) }) {
                        Text("Delete entry", color = MaterialTheme.colorScheme.error)
                    }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val timestamp =
                        if (
                            entry != null &&
                                date.toString() == entry.date &&
                                time ==
                                    Instant.ofEpochMilli(entry.timestamp).atZone(zone).toLocalTime()
                        )
                            entry.timestamp
                        else date.atTime(time).atZone(zone).toInstant().toEpochMilli()
                    onSave(
                        if (!dirty && entry != null) entry.volume
                        else Volume.parse(amount, s.unit)!!,
                        date,
                        timestamp,
                    )
                },
                enabled = Volume.parse(amount, s.unit) != null && date <= LocalDate.now(),
            ) {
                Text("Save entry")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun AccentPicker(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        accents.forEachIndexed { i, color ->
            IconButton(
                onClick = { onSelect(i) },
                modifier = Modifier.semantics { this.selected = selected == i },
            ) {
                Box(
                    Modifier.size(30.dp).background(color, RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected == i)
                        Icon(Icons.Outlined.Check, accentNames[i], tint = Color.White)
                    else Icon(Icons.Outlined.Circle, accentNames[i], tint = color)
                }
            }
        }
    }
}
