// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0

package com.hydri.app.background

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.unit.*
import androidx.glance.*
import androidx.glance.action.*
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.*
import androidx.glance.layout.*
import androidx.glance.text.*
import com.hydri.app.*
import com.hydri.app.R
import com.hydri.app.data.*
import com.hydri.app.domain.Volume
import com.hydri.app.domain.WidgetLayout
import com.hydri.app.i18n.*
import java.time.LocalDate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

object HydriWidgets {
    val revision = MutableStateFlow(0L)

    suspend fun update(context: Context) {
        revision.update { it + 1 }
        SmallHydriWidget().updateAll(context)
        MediumHydriWidget().updateAll(context)
    }
}

abstract class HydriWidgetReceiver : GlanceAppWidgetReceiver() {
    private fun reschedule(context: Context) {
        val app = context.applicationContext as HydriApplication
        app.scope.launch { Reminders.schedule(context, app.repository.preferences.flow.first()) }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        reschedule(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        reschedule(context)
    }
}

class SmallWidgetReceiver : HydriWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmallHydriWidget()
}

class MediumWidgetReceiver : HydriWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MediumHydriWidget()
}

class SmallHydriWidget : HydriWidget(quickAdd = false)

class MediumHydriWidget : HydriWidget(quickAdd = true)

data class WidgetSnapshot(
    val settings: Settings,
    val total: Long,
    val goal: Long,
    val bottles: List<Bottle>,
)

open class HydriWidget(private val quickAdd: Boolean = false) : GlanceAppWidget() {
    // Render each actual launcher-provided size, including pre-Android 12 min/max sizes.
    override val sizeMode = SizeMode.Exact

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = (context.applicationContext as HydriApplication).repository
        repo.initialize()
        val dates =
            flow {
                    while (currentCoroutineContext().isActive) {
                        emit(LocalDate.now().toString())
                        delay(60_000)
                    }
                }
                .distinctUntilChanged()
        val data =
            dates.flatMapLatest { date ->
                combine(
                    repo.preferences.flow,
                    repo.dao.entriesOn(date),
                    repo.dao.goals(),
                    repo.dao.bottles(),
                ) { s, entries, goals, bottles ->
                    WidgetSnapshot(
                        s,
                        entries.sumOf { it.volume },
                        Repository.goalFor(goals, LocalDate.parse(date)),
                        bottles.filter { it.favorite },
                    )
                }
            }
        val initial = data.first()
        provideContent {
            // Collect inside the live Glance composition; updateAll alone cannot refresh a captured
            // snapshot.
            val snapshot by data.collectAsState(initial)
            val revision by HydriWidgets.revision.collectAsState()
            key(revision) { WidgetContent(context, snapshot, quickAdd) }
        }
    }
}

@Composable
private fun WidgetContent(context: Context, state: WidgetSnapshot, quickAdd: Boolean) {
    val size = LocalSize.current
    val colors = widgetColors(context, state.settings)
    val layout = WidgetLayout.forSize(size.width.value, size.height.value, quickAdd)
    val compact = layout.compact
    val roomy = layout.ring
    val controls = layout.buttonCount > 0
    val percent = (state.total.toDouble() / state.goal * 100).toInt()
    val padding = if (compact) 8.dp else 12.dp
    val amountText = Volume.format(state.total, state.settings.unit)
    val amountWidth =
        size.width.value -
            2 * padding.value -
            if (roomy && !layout.verticalRing) layout.ringSize + 12 else 0f
    val amountFont =
        fittingAmountFont(context, amountText, amountWidth, if (compact) 16f else 23f).sp
    val background = colors.surface
    val ink = colors.onSurface
    val muted = colors.onSurfaceVariant
    val accent = colors.primary
    val fraction = (state.total.toFloat() / state.goal).coerceIn(0f, 1f)
    Column(
        GlanceModifier.fillMaxSize()
            .background(
                ImageProvider(R.drawable.widget_surface),
                colorFilter = ColorFilter.tint(background),
            )
            .appWidgetBackground()
            .padding(padding)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        // At the minimum launcher height, larger fonts need the space for intake and progress.
        if (size.height >= 100.dp || context.resources.configuration.fontScale <= 1.1f) {
            Row(
                GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Text(
                    "HYDRI",
                    modifier = GlanceModifier.defaultWeight(),
                    style =
                        TextStyle(color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
                if (roomy)
                    Text(
                        percentText(percent),
                        style =
                            TextStyle(
                                color = accent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        maxLines = 1,
                    )
            }
            Spacer(GlanceModifier.height(if (compact) 3.dp else 7.dp))
        }
        if (layout.verticalRing) {
            Box(GlanceModifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                WidgetRing(fraction, percent, colors, layout.ringSize.dp)
            }
            Spacer(GlanceModifier.height(8.dp))
            Text(
                amountText,
                style = TextStyle(color = ink, fontSize = amountFont, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            Text(
                tr(R.string.of_value, Volume.format(state.goal, state.settings.unit)),
                style = TextStyle(color = muted, fontSize = 12.sp),
                maxLines = 2,
            )
        } else if (roomy) {
            Row(
                GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                WidgetRing(fraction, percent, colors, layout.ringSize.dp)
                Spacer(GlanceModifier.width(12.dp))
                Column(GlanceModifier.defaultWeight()) {
                    Text(
                        amountText,
                        style =
                            TextStyle(
                                color = ink,
                                fontSize = amountFont,
                                fontWeight = FontWeight.Bold,
                            ),
                        maxLines = 1,
                    )
                    Text(
                        tr(R.string.of_value, Volume.format(state.goal, state.settings.unit)),
                        style = TextStyle(color = muted, fontSize = 12.sp),
                        maxLines = 1,
                    )
                    if (size.height >= 210.dp)
                        Text(
                            tr(
                                R.string.value_left,
                                Volume.format(
                                    (state.goal - state.total).coerceAtLeast(0),
                                    state.settings.unit,
                                ),
                            ),
                            style = TextStyle(color = muted, fontSize = 11.sp),
                            maxLines = 1,
                        )
                }
            }
        } else {
            Text(
                amountText,
                style = TextStyle(color = ink, fontSize = amountFont, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            Spacer(GlanceModifier.height(4.dp))
            LinearProgressIndicator(
                fraction,
                modifier = GlanceModifier.fillMaxWidth().height(5.dp),
                color = accent,
                backgroundColor = colors.surfaceVariant,
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                if (compact) percentText(percent)
                else
                    tr(
                        R.string.value_of_value,
                        percent,
                        Volume.format(state.goal, state.settings.unit),
                    ),
                style = TextStyle(color = muted, fontSize = 11.sp),
                maxLines = 1,
            )
        }
        if (controls) {
            Spacer(GlanceModifier.height(8.dp))
            val favorites = state.bottles.take(layout.buttonCount)
            if (favorites.isEmpty())
                Text(
                    tr(R.string.favorite_a_bottle_in_hydri),
                    style = TextStyle(color = muted, fontSize = 12.sp),
                    maxLines = 1,
                )
            else
                Column(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    favorites.chunked(layout.columns).forEachIndexed { rowIndex, row ->
                        if (rowIndex > 0) Spacer(GlanceModifier.height(8.dp))
                        Row(GlanceModifier.fillMaxWidth()) {
                            row.forEachIndexed { index, bottle ->
                                if (index > 0) Spacer(GlanceModifier.width(8.dp))
                                Box(
                                    GlanceModifier.defaultWeight()
                                        .height(48.dp)
                                        .background(
                                            ImageProvider(R.drawable.widget_surface),
                                            colorFilter = ColorFilter.tint(colors.primaryContainer),
                                        )
                                        .clickable(
                                            actionRunCallback<WidgetAdd>(
                                                actionParametersOf(
                                                    ActionParameters.Key<String>("bottle") to
                                                        bottle.id
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(
                                        horizontalAlignment =
                                            Alignment.Horizontal.CenterHorizontally
                                    ) {
                                        if (size.width / layout.columns >= 145.dp)
                                            Text(
                                                bottleDisplayName(bottle),
                                                style =
                                                    TextStyle(
                                                        color = colors.onPrimaryContainer,
                                                        fontSize = 11.sp,
                                                    ),
                                                maxLines = 1,
                                            )
                                        Text(
                                            "+ ${Volume.format(bottle.volume, state.settings.unit)}",
                                            style =
                                                TextStyle(
                                                    color = colors.onPrimaryContainer,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                ),
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }
}

/** Keep the amount readable without scaling the entire RemoteViews layout. */
private fun fittingAmountFont(
    context: Context,
    text: String,
    availableDp: Float,
    maximum: Float,
): Float {
    val metrics = context.resources.displayMetrics
    val paint = android.graphics.Paint().apply { typeface = android.graphics.Typeface.DEFAULT_BOLD }
    var font = maximum
    while (font > 12f) {
        paint.textSize =
            android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_SP,
                font,
                metrics,
            )
        if (paint.measureText(text) <= availableDp.coerceAtLeast(1f) * metrics.density) break
        font -= 1f
    }
    return font
}

@Composable
private fun WidgetRing(fraction: Float, percent: Int, colors: WidgetColors, diameter: Dp) {
    Box(GlanceModifier.size(diameter)) {
        Image(
            ImageProvider(
                WidgetProgress.ring(
                    0f,
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.WHITE,
                )
            ),
            null,
            modifier = GlanceModifier.fillMaxSize(),
            colorFilter = ColorFilter.tint(colors.surfaceVariant),
        )
        Image(
            ImageProvider(
                WidgetProgress.ring(
                    fraction,
                    android.graphics.Color.WHITE,
                    android.graphics.Color.TRANSPARENT,
                )
            ),
            tr(R.string.value_percent_of_daily_goal, percent),
            modifier = GlanceModifier.fillMaxSize(),
            colorFilter = ColorFilter.tint(colors.primary),
        )
    }
}

class WidgetAdd : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val repo = (context.applicationContext as HydriApplication).repository
        val bottle =
            repo.dao.allBottles().find {
                it.id == parameters[ActionParameters.Key<String>("bottle")]
            } ?: return
        val settings = repo.preferences.flow.first()
        val today = LocalDate.now()
        val before = repo.dao.entriesOn(today.toString()).first().sumOf { it.volume }
        val goal = Repository.goalFor(repo.dao.allGoals(), today)
        repo.log(bottle.volume, bottle.name, bottle = bottle)
        HydriWidgets.update(context)
        if (settings.goalNotification && before < goal && before + bottle.volume >= goal)
            Reminders.notify(
                context,
                tr(R.string.you_ve_reached_today_s_hydration_goal),
                2,
                settings,
            )
    }
}
