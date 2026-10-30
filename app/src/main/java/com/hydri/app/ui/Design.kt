package com.hydri.app.ui

import android.provider.Settings as AndroidSettings
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.core.view.WindowCompat
import com.hydri.app.data.Settings
import com.hydri.app.domain.Volume
import kotlin.math.*

val accents =
    listOf(
        Color(0xFF247BEA),
        Color(0xFF009A8B),
        Color(0xFF8064D5),
        Color(0xFFD36587),
        Color(0xFFBD7519),
        Color(0xFF347A98),
    )
val accentNames = listOf("Water blue", "Lagoon", "Lavender", "Rose", "Amber", "Slate")
val bottleIcons =
    listOf(
        Icons.Outlined.LocalDrink,
        Icons.Outlined.WaterDrop,
        Icons.Outlined.Water,
        Icons.Outlined.Coffee,
        Icons.Outlined.FitnessCenter,
        Icons.Outlined.EmojiFoodBeverage,
        Icons.Outlined.Category,
    )
val bottleNames =
    listOf("Glass", "Bottle", "Large bottle", "Tumbler", "Sports bottle", "Mug", "Custom container")
val LocalReducedMotion = staticCompositionLocalOf { false }
val LocalFloatingBarInset = staticCompositionLocalOf { 0.dp }

@Composable
fun HydriTheme(s: Settings, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val dark =
        usesDarkTheme(
            s,
            configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES,
        )
    val scheme = hydriColors(context, s)
    SideEffect {
        (context as? android.app.Activity)?.let { activity ->
            WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    val motionOff =
        s.reducedMotion ||
            AndroidSettings.Global.getFloat(
                context.contentResolver,
                AndroidSettings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
    CompositionLocalProvider(LocalReducedMotion provides motionOff) {
        MaterialTheme(
            colorScheme = scheme,
            typography =
                Typography(
                    headlineLarge =
                        androidx.compose.ui.text.TextStyle(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-1).sp,
                        ),
                    headlineMedium =
                        androidx.compose.ui.text.TextStyle(
                            fontSize = 27.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-.7).sp,
                        ),
                    titleLarge =
                        androidx.compose.ui.text.TextStyle(
                            fontSize = 21.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    bodyLarge =
                        androidx.compose.ui.text.TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
                    bodyMedium =
                        androidx.compose.ui.text.TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
                ),
            content = content,
        )
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = .94f),
                        MaterialTheme.colorScheme.surface.copy(alpha = .68f),
                    )
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = .06f),
                        MaterialTheme.colorScheme.primary.copy(alpha = .10f),
                    )
                ),
                shape,
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun ScreenColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val defaultBringIntoView = LocalBringIntoViewSpec.current
    val obscured =
        with(androidx.compose.ui.platform.LocalDensity.current) {
            LocalFloatingBarInset.current.toPx()
        }
    val bringIntoView =
        remember(defaultBringIntoView, obscured) {
            object : BringIntoViewSpec {
                override fun calculateScrollDistance(
                    offset: Float,
                    size: Float,
                    containerSize: Float,
                ): Float =
                    defaultBringIntoView.calculateScrollDistance(
                        offset,
                        size,
                        (containerSize - obscured).coerceAtLeast(1f),
                    )
            }
        }
    CompositionLocalProvider(LocalBringIntoViewSpec provides bringIntoView) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 20.dp, bottom = 20.dp + LocalFloatingBarInset.current),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                content = content,
            )
        }
    }
}

@Composable
fun Eyebrow(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 2.sp,
    )
}

@Composable
fun PageHeader(title: String, subtitle: String = "") {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge)
        if (subtitle.isNotBlank())
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SectionTitle(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (action != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
fun Symbol(
    icon: ImageVector,
    label: String? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    size: Int = 48,
) {
    Box(
        Modifier.size(size.dp)
            .clip(RoundedCornerShape((size / 3).dp))
            .background(color.copy(alpha = .10f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = color, modifier = Modifier.size((size * .5).dp))
    }
}

@Composable
fun WaterProgress(total: Long, goal: Long, unit: String, compact: Boolean = false) {
    val ratio = (total.toFloat() / goal).coerceIn(0f, 1f)
    val progress by
        animateFloatAsState(
            ratio,
            tween(if (LocalReducedMotion.current) 0 else 750),
            label = "Water progress",
        )
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(
        Modifier.fillMaxWidth().height(if (compact) 224.dp else 278.dp).semantics(
            mergeDescendants = true
        ) {
            contentDescription =
                "${Volume.format(total,unit)} of ${Volume.format(goal,unit)}, ${(total.toDouble()/goal*100).toInt()} percent"
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(if (compact) 208.dp else 258.dp).clearAndSetSemantics {}) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2
            drawCircle(track, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
            drawArc(
                Brush.sweepGradient(listOf(primary.copy(alpha = .35f), primary)),
                -90f,
                360f * progress,
                false,
                Offset(inset, inset),
                Size(size.width - stroke, size.height - stroke),
                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round),
            )
            val r = size.width / 2 - 22.dp.toPx()
            drawCircle(
                Brush.radialGradient(
                    listOf(primary.copy(alpha = .14f), primary.copy(alpha = .02f))
                ),
                r,
            )
            val y = center.y + r - (2 * r * progress)
            val wave =
                Path().apply {
                    moveTo(center.x - r, center.y + r)
                    for (i in 0..100) {
                        val x = center.x - r + 2 * r * i / 100
                        val circleTop =
                            center.y - sqrt((r * r - (x - center.x).pow(2)).coerceAtLeast(0f))
                        val circleBottom =
                            center.y + sqrt((r * r - (x - center.x).pow(2)).coerceAtLeast(0f))
                        lineTo(
                            x,
                            (y + sin(i / 100f * 2 * PI).toFloat() * 5.dp.toPx()).coerceIn(
                                circleTop,
                                circleBottom,
                            ),
                        )
                    }
                    for (i in 100 downTo 0) {
                        val x = center.x - r + 2 * r * i / 100
                        lineTo(
                            x,
                            center.y + sqrt((r * r - (x - center.x).pow(2)).coerceAtLeast(0f)),
                        )
                    }
                    close()
                }
            drawPath(
                wave,
                Brush.verticalGradient(
                    listOf(primary.copy(alpha = .04f), primary.copy(alpha = .2f))
                ),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.clearAndSetSemantics {},
        ) {
            Icon(
                if (total >= goal) Icons.Outlined.CheckCircle else Icons.Outlined.WaterDrop,
                null,
                tint = primary,
            )
            Text(Volume.format(total, unit), style = MaterialTheme.typography.headlineLarge)
            Text(
                "of ${Volume.format(goal,unit)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${(total.toDouble()/goal*100).toInt()}%",
                color = primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected, onClick, label = { Text(label) })
}

@Composable
fun SettingToggle(
    title: String,
    subtitle: String = "",
    value: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title)
            if (subtitle.isNotBlank())
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
        }
        Switch(value, onChange, modifier = Modifier.semantics { contentDescription = title })
    }
}

@Composable
fun <T> SettingMenu(
    title: String,
    value: T,
    options: List<T>,
    label: (T) -> String = { it.toString() },
    onChange: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Box {
            OutlinedButton(
                onClick = { expanded = !expanded },
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.semantics { contentDescription = "$title: ${label(value)}" },
            ) {
                Text(label(value))
                Icon(Icons.Outlined.ExpandMore, null)
            }
            DropdownMenu(
                expanded,
                { expanded = false },
                modifier = Modifier.widthIn(min = 152.dp, max = 280.dp),
                shape = RoundedCornerShape(22.dp),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .97f),
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .16f)),
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(label(option)) },
                        onClick = {
                            expanded = false
                            onChange(option)
                        },
                        trailingIcon = { if (option == value) Icon(Icons.Outlined.Check, null) },
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsLink(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) {
                role = androidx.compose.ui.semantics.Role.Button
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Symbol(icon, size = 40)
        Text(title, Modifier.weight(1f))
        Icon(Icons.Outlined.ChevronRight, null)
    }
}
