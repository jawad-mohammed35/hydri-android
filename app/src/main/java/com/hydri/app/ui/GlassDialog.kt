// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0

package com.hydri.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.hydri.app.R
import com.hydri.app.i18n.*

@Composable
fun GlassDialog(
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    enabled: Boolean = true,
    destructive: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val transition = remember { MutableTransitionState(false).apply { targetState = true } }
    var finish by remember { mutableStateOf<(() -> Unit)?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current
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
                    Modifier.widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .heightIn(max = maxHeight)
                        .testTag("glass-dialog"),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = .98f),
                    shadowElevation = 12.dp,
                    border =
                        BorderStroke(
                            1.dp,
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = .24f),
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f),
                                )
                            ),
                        ),
                ) {
                    Column(
                        Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                title,
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            IconButton({ close(onDismiss) }) {
                                Icon(Icons.Outlined.Close, tr(R.string.close_editor))
                            }
                        }
                        Column(
                            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            content = content,
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                { close(onDismiss) },
                                Modifier.weight(1f),
                                shape = RoundedCornerShape(18.dp),
                            ) {
                                Text(tr(R.string.cancel))
                            }
                            Button(
                                { close(onConfirm) },
                                Modifier.weight(1.3f),
                                enabled = enabled && finish == null,
                                shape = RoundedCornerShape(18.dp),
                                colors =
                                    if (destructive)
                                        ButtonDefaults.buttonColors(
                                            containerColor =
                                                MaterialTheme.colorScheme.errorContainer,
                                            contentColor =
                                                MaterialTheme.colorScheme.onErrorContainer,
                                        )
                                    else ButtonDefaults.buttonColors(),
                            ) {
                                Text(confirmLabel)
                            }
                        }
                    }
                }
            }
        }
    }
}
