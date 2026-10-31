// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0

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
import com.hydri.app.LauncherShortcuts
import com.hydri.app.R
import com.hydri.app.data.*
import com.hydri.app.domain.*
import com.hydri.app.i18n.*
import java.time.*
import kotlinx.coroutines.launch

@Composable
fun HydriApp(
    vm: HydriViewModel = viewModel(),
    shortcut: String? = null,
    onShortcutHandled: () -> Unit = {},
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var bottleEditor by remember { mutableStateOf<Bottle?>(null) }
    var bottleOpen by remember { mutableStateOf(false) }
    var entryEditor by remember { mutableStateOf<WaterEntry?>(null) }
    var entryOpen by remember { mutableStateOf(false) }
    var goalOpen by remember { mutableStateOf(false) }
    var resetTodayOpen by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    LaunchedEffect(state.today) { entryOpen = false }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted)
                scope.launch {
                    snackbar.showSnackbar(
                        tr(R.string.notifications_are_blocked_enable_them_in_android_app_setti)
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
                        if (undoAction != null) tr(R.string.undo) else null,
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
                val scrollRequest = remember {
                    kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 1)
                }
                val backStack by nav.currentBackStackEntryAsState()
                val route = backStack?.destination?.route.orEmpty()
                val selectedTab = primaryTabFor(backStack?.destination)
                fun openPrimary(name: String) {
                    require(name in primaryTabs)
                    nav.navigate(if (name == "More") "MoreSection" else name) {
                        popUpTo(nav.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        // Home is the root, never a restored stack that could contain another tab.
                        restoreState = name != "Home"
                    }
                }
                LaunchedEffect(shortcut) {
                    if (shortcut != null) {
                        entryOpen = false
                        bottleOpen = false
                        goalOpen = false
                        resetTodayOpen = false
                        confirm = null
                        when (shortcut) {
                            LauncherShortcuts.LOG_WATER -> {
                                openPrimary("Home")
                                entryEditor = null
                                entryOpen = true
                            }
                            LauncherShortcuts.CREATE_BOTTLE -> {
                                openPrimary("Bottles")
                                bottleEditor = null
                                bottleOpen = true
                            }
                            LauncherShortcuts.TRACKER -> openPrimary("Tracker")
                        }
                        onShortcutHandled()
                    }
                }
                Scaffold(
                    containerColor = Color.Transparent,
                    snackbarHost = { SnackbarHost(snackbar, Modifier.testTag("snackbar")) },
                    bottomBar = {
                        if (route in primaryTabs)
                            SlidingNavigation(
                                selectedTab ?: route,
                                state.settings.haptics,
                                onReselect = { scrollRequest.tryEmit(route) },
                            ) { name ->
                                openPrimary(name)
                            }
                    },
                ) { padding ->
                    val reduced = LocalReducedMotion.current
                    val secondary =
                        setOf("Notifications", "Hydration Guide", "Data & Backup", "About")
                    CompositionLocalProvider(
                        LocalTabScrollRequest provides scrollRequest,
                        LocalFloatingBarInset provides
                            if (route in primaryTabs) padding.calculateBottomPadding() else 0.dp,
                    ) {
                        NavHost(
                            nav,
                            "Home",
                            Modifier.padding(
                                    start =
                                        padding.calculateStartPadding(LocalLayoutDirection.current),
                                    end = padding.calculateEndPadding(LocalLayoutDirection.current),
                                    top = padding.calculateTopPadding(),
                                    bottom =
                                        if (route in primaryTabs) 0.dp
                                        else padding.calculateBottomPadding(),
                                )
                                .testTag("page-viewport")
                                .semantics { this[PrimaryTabRoute] = selectedTab.orEmpty() },
                            enterTransition = {
                                if (reduced) EnterTransition.None
                                else if (targetState.destination.route in secondary)
                                    slideInHorizontally(
                                        spring(dampingRatio = .88f, stiffness = 550f)
                                    ) {
                                        it
                                    } + fadeIn(tween(140))
                                else fadeIn(tween(160))
                            },
                            exitTransition = {
                                if (reduced) ExitTransition.None
                                else if (targetState.destination.route in secondary)
                                    slideOutHorizontally(tween(230)) { -it / 5 } +
                                        fadeOut(tween(150))
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
                                    slideOutHorizontally(
                                        spring(dampingRatio = .9f, stiffness = 600f)
                                    ) {
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
                                            haptic.performHapticFeedback(
                                                HapticFeedbackType.LongPress
                                            )
                                        vm.log(b.volume, b.name, bottle = b)
                                    },
                                    onCustom = {
                                        entryEditor = null
                                        entryOpen = true
                                    },
                                    onEntry = {
                                        if (EntryPolicy.canModify(it.date, state.today)) {
                                            entryEditor = it
                                            entryOpen = true
                                        }
                                    },
                                    onUndo = vm::undoToday,
                                    onReset = { resetTodayOpen = true },
                                    onDelete = vm::delete,
                                    onBottles = { openPrimary("Bottles") },
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
                                            tr(
                                                R.string
                                                    .delete_value_saved_water_entries_will_stay_in_your_history,
                                                bottleDisplayName(b),
                                            ) to { vm.deleteBottle(b) }
                                    },
                                )
                            }
                            composable("Tracker") {
                                TrackerScreen(
                                    state,
                                    onEntry = {
                                        if (EntryPolicy.canModify(it.date, state.today)) {
                                            entryEditor = it
                                            entryOpen = true
                                        }
                                    },
                                    onAdd = { date ->
                                        if (date == state.today) {
                                            entryEditor = null
                                            entryOpen = true
                                        }
                                    },
                                )
                            }
                            navigation(startDestination = "More", route = "MoreSection") {
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
                    state.today,
                    state.settings,
                    { entryOpen = false },
                    onDelete = { e ->
                        vm.delete(e)
                        entryOpen = false
                    },
                    today = state.today,
                ) { volume, date, time ->
                    val editing = entryEditor
                    if (editing == null) vm.log(volume, "Custom water", date, time)
                    else vm.action { vm.repo.updateEntry(editing.id, volume, time) }
                    entryOpen = false
                }
            if (goalOpen)
                AmountDialog(
                    tr(R.string.daily_water_goal),
                    state.goal(state.today),
                    state.settings.unit,
                    tr(R.string.changes_apply_from_today_earlier_goals_stay_the_same_gener),
                    { goalOpen = false },
                ) {
                    vm.goal(it)
                    goalOpen = false
                }
            if (resetTodayOpen)
                GlassDialog(
                    tr(R.string.reset_today_s_intake),
                    tr(R.string.reset),
                    { resetTodayOpen = false },
                    {
                        vm.resetToday()
                        resetTodayOpen = false
                    },
                    destructive = true,
                ) {
                    Text(
                        tr(R.string.this_removes_all_of_today_s_logged_water_entries_other_day),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            confirm?.let { (message, action) ->
                GlassDialog(
                    title = tr(R.string.please_confirm),
                    confirmLabel = tr(R.string.confirm),
                    onDismiss = { confirm = null },
                    onConfirm = {
                        confirm = null
                        action()
                    },
                    destructive = true,
                ) {
                    Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
    val titles =
        listOf(
            "Hydri",
            tr(R.string.your_name),
            tr(R.string.appearance),
            tr(R.string.measurements),
            tr(R.string.daily_goal),
        )
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
                                Text(tr(R.string.back))
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
                                if (page == 0) tr(R.string.get_started)
                                else if (page == 4) tr(R.string.done)
                                else tr(R.string.continue_action)
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
                                tr(
                                    R.string
                                        .track_water_and_set_a_daily_goal_everything_stays_on_your
                                )
                            )
                            Text(
                                tr(R.string.no_account_no_ads),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        1 -> {
                            Text(
                                tr(R.string.a_first_name_or_nickname_is_fine),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            OutlinedTextField(
                                s.name,
                                { s = s.copy(name = it.take(60)) },
                                label = { Text(tr(R.string.name_optional)) },
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
                                    "System" to tr(R.string.follow_system),
                                    "Light" to tr(R.string.light),
                                    "Dark" to tr(R.string.dark),
                                )
                                .forEach { (value, label) ->
                                    SelectionCard(label, s.theme == value) {
                                        s = s.copy(theme = value)
                                    }
                                }
                            SettingToggle(
                                tr(R.string.oled_pure_black),
                                tr(R.string.used_with_the_dark_theme),
                                s.oled,
                            ) {
                                s = s.copy(oled = it)
                            }
                        }
                        3 -> {
                            listOf(
                                    "mL" to tr(R.string.millilitres_ml),
                                    "L" to tr(R.string.litres_l),
                                    "oz" to tr(R.string.us_fluid_ounces_oz),
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
                                tr(
                                    R.string.general_starting_points_not_personalized_medical_advice
                                ),
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
                            SelectionCard(tr(R.string.custom), custom) {
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
        label = { Text(tr(R.string.amount_value, unitLabel(unit))) },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        isError = text.isNotBlank() && Volume.parse(text, unit) == null,
        supportingText = {
            if (text.isNotBlank() && Volume.parse(text, unit) == null)
                Text(tr(R.string.enter_an_amount_from_1_to_20_000_ml_or_equivalent))
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
