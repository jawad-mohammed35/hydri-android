// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0

package com.hydri.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*

@Composable
fun GlassTimePicker(
    title: String,
    initialMinute: Int,
    is24Hour: Boolean,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    val initialHour = initialMinute / 60
    var hour by remember {
        mutableStateOf(
            (if (is24Hour) initialHour else (initialHour % 12).let { if (it == 0) 12 else it })
                .toString()
                .padStart(2, '0')
        )
    }
    var minute by remember { mutableStateOf((initialMinute % 60).toString().padStart(2, '0')) }
    var pm by remember { mutableStateOf(initialHour >= 12) }
    val keyboard = LocalSoftwareKeyboardController.current
    val h = hour.toIntOrNull()
    val m = minute.toIntOrNull()
    val valid = h != null && h in (if (is24Hour) 0..23 else 1..12) && m != null && m in 0..59
    val transition = remember { MutableTransitionState(false).apply { targetState = true } }
    var finish by remember { mutableStateOf<(() -> Unit)?>(null) }
    fun close(action: () -> Unit) {
        if (finish == null) {
            keyboard?.hide()
            finish = action
            transition.targetState = false
        }
    }
    LaunchedEffect(transition.isIdle, transition.currentState) {
        if (transition.isIdle && !transition.currentState) finish?.invoke()
    }
    val reduced = LocalReducedMotion.current
    Dialog(
        { close(onDismiss) },
        properties =
            DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        BoxWithConstraints(
            Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                transition,
                enter =
                    if (reduced) EnterTransition.None
                    else
                        fadeIn(tween(130)) +
                            scaleIn(
                                spring(dampingRatio = .92f, stiffness = 600f),
                                initialScale = .96f,
                            ),
                exit =
                    if (reduced) ExitTransition.None
                    else fadeOut(tween(110)) + scaleOut(tween(110), targetScale = .98f),
            ) {
                Surface(
                    Modifier.widthIn(max = 420.dp)
                        .fillMaxWidth()
                        .heightIn(max = maxHeight)
                        .testTag("glass-time-picker"),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = .98f),
                    tonalElevation = 4.dp,
                    shadowElevation = 12.dp,
                    border =
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .22f)),
                ) {
                    Column(
                        Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        Text(title, style = MaterialTheme.typography.titleLarge)
                        Column(
                            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    hour,
                                    { value ->
                                        if (value.length <= 2 && value.all(Char::isDigit))
                                            hour = value
                                    },
                                    label = { Text("Hour") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(18.dp),
                                    textStyle = MaterialTheme.typography.headlineMedium,
                                    keyboardOptions =
                                        KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Next,
                                        ),
                                )
                                OutlinedTextField(
                                    minute,
                                    { value ->
                                        if (value.length <= 2 && value.all(Char::isDigit))
                                            minute = value
                                    },
                                    label = { Text("Minute") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(18.dp),
                                    textStyle = MaterialTheme.typography.headlineMedium,
                                    keyboardOptions =
                                        KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Done,
                                        ),
                                    keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                                )
                            }
                            if (!is24Hour)
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Choice("AM", !pm) { pm = false }
                                    Choice("PM", pm) { pm = true }
                                }
                            Text(
                                if (is24Hour) "24-hour time" else "12-hour time",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                { close(onDismiss) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(18.dp),
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                {
                                    if (valid) {
                                        val value =
                                            (if (is24Hour) h!! else h!! % 12 + if (pm) 12 else 0) *
                                                60 + m!!
                                        close { onSave(value) }
                                    }
                                },
                                enabled = valid,
                                modifier = Modifier.weight(1.3f),
                                shape = RoundedCornerShape(18.dp),
                            ) {
                                Text("Save time")
                            }
                        }
                    }
                }
            }
        }
    }
}
