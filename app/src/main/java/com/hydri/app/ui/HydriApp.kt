package com.hydri.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
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
    var entryDate by remember { mutableStateOf<LocalDate?>(null) }
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
        vm.messages.collect { (message, undoAction) ->
            launch {
                snackbar.currentSnackbarData?.dismiss()
                if (
                    snackbar.showSnackbar(
                        message,
                        if (undoAction != null) "Undo" else null,
                        withDismissAction = true,
                        duration = SnackbarDuration.Short,
                    ) == SnackbarResult.ActionPerformed && undoAction != null
                )
                    undoAction()
            }
        }
    }
    HydriTheme(state.settings) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (!state.ready)
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            else if (!state.settings.onboarded) Onboarding(state, vm)
            else {
                val nav = rememberNavController()
                val backStack by nav.currentBackStackEntryAsState()
                val route = backStack?.destination?.route ?: "Home"
                Scaffold(
                    containerColor = Color.Transparent,
                    snackbarHost = { SnackbarHost(snackbar, Modifier.testTag("snackbar")) },
                    bottomBar = {
                        if (route in primaryTabs)
                            SlidingNavigation(route, state.settings.haptics) { name ->
                                nav.navigate(name) {
                                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                    },
                ) { padding ->
                    val reduced = LocalReducedMotion.current
                    val secondary =
                        setOf("Notifications", "Hydration Guide", "Data & Backup", "About")
                    NavHost(
                        nav,
                        "Home",
                        Modifier.padding(padding),
                        enterTransition = {
                            if (reduced) EnterTransition.None
                            else if (targetState.destination.route in secondary)
                                slideInHorizontally(spring(dampingRatio = .88f, stiffness = 550f)) {
                                    it
                                } + fadeIn(tween(140))
                            else fadeIn(tween(160))
                        },
                        exitTransition = {
                            if (reduced) ExitTransition.None
                            else if (targetState.destination.route in secondary)
                                slideOutHorizontally(tween(230)) { -it / 5 } + fadeOut(tween(150))
                            else fadeOut(tween(160))
                        },
                        popEnterTransition = {
                            if (reduced) EnterTransition.None
                            else if (initialState.destination.route in secondary)
                                slideInHorizontally(tween(230)) { -it / 5 } + fadeIn(tween(170))
                            else fadeIn(tween(160))
                        },
                        popExitTransition = {
                            if (reduced) ExitTransition.None
                            else if (initialState.destination.route in secondary)
                                slideOutHorizontally(spring(dampingRatio = .9f, stiffness = 600f)) {
                                    it
                                } + fadeOut(tween(180))
                            else fadeOut(tween(160))
                        },
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
                                    entryDate = null
                                    entryOpen = true
                                },
                                onEntry = {
                                    entryEditor = it
                                    entryOpen = true
                                },
                                onUndo = vm::undoToday,
                                onReset = {
                                    confirm =
                                        "Clear all of today's sips? Other days will be kept." to
                                            {
                                                vm.resetToday()
                                            }
                                },
                                onDelete = vm::delete,
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
                                onNavigate = { nav.navigate(it) },
                            )
                        }
                        listOf("Notifications", "Hydration Guide", "Data & Backup", "About")
                            .forEach { page ->
                                composable(page) {
                                    MoreScreen(
                                        state,
                                        vm,
                                        ::requestNotifications,
                                        { goalOpen = true },
                                        { message, action -> confirm = message to action },
                                        snackbar,
                                        page = page,
                                        onNavigate = { nav.navigate(it) },
                                        onBack = { nav.popBackStack() },
                                    )
                                }
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
                    entryDate ?: state.today,
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
                    if (editing == null) {
                        val targetDate = entryDate ?: LocalDate.now()
                        val targetTime =
                            if (targetDate == date) time
                            else
                                EntryTime.resolve(
                                    targetDate,
                                    Instant.ofEpochMilli(time)
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalTime(),
                                    ZoneId.systemDefault(),
                                )
                        if (targetTime == null) {
                            vm.action {
                                error("The local date changed. Choose a valid time for today.")
                            }
                            return@EntryDialog
                        }
                        vm.log(volume, "Custom water", targetDate, targetTime)
                    } else
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
                    "Changes apply from today. Earlier goals stay the same. General starting points are not medical advice.",
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
fun Onboarding(state: UiState, vm: HydriViewModel) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    var s by
        rememberSaveable(
            stateSaver =
                androidx.compose.runtime.saveable.Saver<Settings, String>(
                    save = { it.json().toString() },
                    restore = { Settings.from(org.json.JSONObject(it)) },
                )
        ) {
            mutableStateOf(state.settings)
        }
    var selectedGoal by rememberSaveable { mutableLongStateOf(state.goal(state.today)) }
    var custom by rememberSaveable { mutableStateOf(false) }
    var customText by rememberSaveable { mutableStateOf(Volume.input(selectedGoal, s.unit)) }
    val keyboard = LocalSoftwareKeyboardController.current
    val goal = if (custom) Volume.parse(customText, s.unit) else selectedGoal
    val titles = listOf("Hydri", "Your name", "Appearance", "Measurements", "Daily goal")
    HydriTheme(s) {
        Scaffold(
            modifier = Modifier.imePadding().testTag("onboarding"),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                Column(
                    Modifier.navigationBarsPadding()
                        .padding(horizontal = 24.dp)
                        .padding(top = 8.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(5) { i ->
                            Box(
                                Modifier.weight(1f)
                                    .height(3.dp)
                                    .background(
                                        if (i <= page) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(3.dp),
                                    )
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (page > 0)
                            TextButton(
                                onClick = {
                                    keyboard?.hide()
                                    page--
                                }
                            ) {
                                Text("Back")
                            }
                        Button(
                            onClick = {
                                keyboard?.hide()
                                if (page == 4)
                                    vm.action {
                                        vm.repo.goal(goal!!)
                                        vm.repo.preferences.save(s.copy(onboarded = true))
                                        com.hydri.app.background.Reminders.schedule(
                                            vm.getApplication(),
                                            s.copy(onboarded = true),
                                        )
                                    }
                                else page++
                            },
                            enabled = page != 4 || goal != null,
                            modifier = Modifier.weight(1f).height(48.dp),
                        ) {
                            Text(
                                if (page == 0) "Get started"
                                else if (page == 4) "Done" else "Continue"
                            )
                        }
                    }
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier.widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Symbol(Icons.Outlined.WaterDrop, size = if (page == 0) 96 else 44)
                    Text(titles[page], style = MaterialTheme.typography.headlineMedium)
                    when (page) {
                        0 -> {
                            Text(
                                "Track water and set a daily goal. Everything stays on your device."
                            )
                            Text(
                                "No account. No ads.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        1 -> {
                            Text(
                                "A first name or nickname is fine.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            OutlinedTextField(
                                s.name,
                                { s = s.copy(name = it.take(60)) },
                                label = { Text("Name (optional)") },
                                singleLine = true,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions =
                                    androidx.compose.foundation.text.KeyboardOptions(
                                        capitalization =
                                            androidx.compose.ui.text.input.KeyboardCapitalization
                                                .Words,
                                        imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                                    ),
                                keyboardActions =
                                    androidx.compose.foundation.text.KeyboardActions(
                                        onNext = {
                                            keyboard?.hide()
                                            page++
                                        }
                                    ),
                            )
                        }
                        2 -> {
                            listOf(
                                    "System" to "Follow system",
                                    "Light" to "Light",
                                    "Dark" to "Dark",
                                )
                                .forEach { (value, label) ->
                                    SelectionCard(label, s.theme == value) {
                                        s = s.copy(theme = value)
                                    }
                                }
                            SettingToggle("OLED pure black", "Used with the dark theme", s.oled) {
                                s = s.copy(oled = it)
                            }
                        }
                        3 -> {
                            listOf(
                                    "mL" to "Millilitres (mL)",
                                    "L" to "Litres (L)",
                                    "oz" to "US fluid ounces (oz)",
                                )
                                .forEach { (unit, label) ->
                                    SelectionCard(label, s.unit == unit) {
                                        val value =
                                            if (custom)
                                                Volume.parse(customText, s.unit) ?: selectedGoal
                                            else selectedGoal
                                        s = s.copy(unit = unit)
                                        customText = Volume.input(value, unit)
                                    }
                                }
                        }
                        4 -> {
                            Text(
                                "General starting points, not personalized medical advice.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            listOf(1_500_000L, 2_000_000L, 2_500_000L).forEach { value ->
                                SelectionCard(
                                    Volume.format(value, s.unit),
                                    !custom && selectedGoal == value,
                                ) {
                                    selectedGoal = value
                                    custom = false
                                }
                            }
                            SelectionCard("Custom", custom) {
                                custom = true
                                customText = Volume.input(selectedGoal, s.unit)
                            }
                            if (custom) VolumeField(customText, { customText = it }, s.unit)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SelectionCard(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color =
            if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .65f)
            else MaterialTheme.colorScheme.surface.copy(alpha = .82f),
        border =
            BorderStroke(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .5f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = .4f),
            ),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 4.dp).heightIn(min = 44.dp).semantics {
                this.selected = selected
                role = Role.RadioButton
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, Modifier.weight(1f))
            RadioButton(selected, onClick = null)
        }
    }
}

@Composable
fun VolumeField(text: String, onChange: (String) -> Unit, unit: String) {
    OutlinedTextField(
        text,
        onChange,
        label = { Text("Amount ($unit)") },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
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
fun AccentPicker(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.testTag("accent-picker").horizontalScroll(rememberScrollState()),
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
