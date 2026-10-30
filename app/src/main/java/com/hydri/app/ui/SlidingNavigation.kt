package com.hydri.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import com.hydri.app.domain.TabPosition

val primaryTabs = listOf("Home", "Bottles", "Tracker", "More")

@Composable
fun SlidingNavigation(selected: String, haptics: Boolean, onSelect: (String) -> Unit) {
    val feedback = LocalHapticFeedback.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val current by rememberUpdatedState(selected)
    val select by rememberUpdatedState(onSelect)
    var width by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var pointerX by remember { mutableFloatStateOf(0f) }
    var lastCrossed by remember { mutableIntStateOf(-1) }
    fun choose(index: Int, force: Boolean = false) {
        if (force || primaryTabs[index] != current) {
            if (haptics) feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            select(primaryTabs[index])
        }
    }
    val index = primaryTabs.indexOf(selected).coerceAtLeast(0)
    val physical = if (rtl) 3 - index else index
    val slot = width / 4f
    val target =
        if (dragging) (pointerX - slot / 2).coerceIn(0f, (width - slot).coerceAtLeast(0f))
        else physical * slot
    val left by
        animateFloatAsState(
            target,
            animationSpec =
                if (dragging || LocalReducedMotion.current) snap()
                else spring(dampingRatio = .88f, stiffness = 600f),
            label = "Navigation indicator",
        )
    val tint = MaterialTheme.colorScheme.primary
    Box(
        Modifier.fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            Modifier.widthIn(max = 640.dp).fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = .94f),
            tonalElevation = 3.dp,
            border = BorderStroke(1.dp, tint.copy(alpha = .14f)),
        ) {
            Box(
                Modifier.padding(6.dp)
                    .onSizeChanged { width = it.width.toFloat() }
                    .testTag("sliding-navigation")
                    .pointerInput(haptics, rtl) {
                        detectHorizontalDragGestures(
                            onDragStart = { position ->
                                dragging = true
                                pointerX = position.x.coerceIn(0f, width)
                                lastCrossed = primaryTabs.indexOf(current)
                                val next = TabPosition.indexAt(pointerX, width, rtl)
                                if (next != lastCrossed) {
                                    lastCrossed = next
                                    choose(next, true)
                                }
                            },
                            onDragEnd = { dragging = false },
                            onDragCancel = { dragging = false },
                            onHorizontalDrag = { change, _ ->
                                change.consume()
                                pointerX = change.position.x.coerceIn(0f, width)
                                val next = TabPosition.indexAt(pointerX, width, rtl)
                                if (next != lastCrossed) {
                                    lastCrossed = next
                                    choose(next, true)
                                }
                            },
                        )
                    }
            ) {
                Canvas(Modifier.matchParentSize()) {
                    drawRoundRect(
                        tint.copy(alpha = .13f),
                        topLeft = Offset(left, 0f),
                        size = Size(slot, size.height),
                        cornerRadius = CornerRadius(26.dp.toPx()),
                    )
                }
                Row(Modifier.fillMaxWidth().selectableGroup()) {
                    val icons =
                        listOf(
                            Icons.Outlined.Home,
                            Icons.Outlined.WaterDrop,
                            Icons.Outlined.CalendarMonth,
                            Icons.Outlined.GridView,
                        )
                    primaryTabs.forEachIndexed { i, name ->
                        val active = name == selected
                        val interaction = remember { MutableInteractionSource() }
                        val focused by interaction.collectIsFocusedAsState()
                        Column(
                            Modifier.weight(1f)
                                .border(
                                    1.5.dp,
                                    if (focused) tint
                                    else androidx.compose.ui.graphics.Color.Transparent,
                                    RoundedCornerShape(26.dp),
                                )
                                .selectable(
                                    active,
                                    interactionSource = interaction,
                                    indication = null,
                                    role = Role.Tab,
                                    onClick = { choose(i) },
                                )
                                .semantics(mergeDescendants = true) { contentDescription = name }
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                icons[i],
                                null,
                                tint =
                                    if (active) tint else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                name,
                                style = MaterialTheme.typography.labelSmall,
                                color =
                                    if (active) tint else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
