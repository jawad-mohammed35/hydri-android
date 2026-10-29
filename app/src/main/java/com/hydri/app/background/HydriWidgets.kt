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

class SmallHydriWidget : HydriWidget()

class MediumHydriWidget : HydriWidget()

data class WidgetSnapshot(
    val settings: Settings,
    val total: Long,
    val goal: Long,
    val bottles: List<Bottle>,
)

open class HydriWidget : GlanceAppWidget() {
    override val sizeMode =
        SizeMode.Responsive(
            setOf(
                DpSize(100.dp, 80.dp),
                DpSize(160.dp, 120.dp),
                DpSize(160.dp, 200.dp),
                DpSize(240.dp, 110.dp),
                DpSize(240.dp, 160.dp),
                DpSize(320.dp, 220.dp),
            )
        )

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
            key(revision) { WidgetContent(context, snapshot) }
        }
    }
}

@Composable
private fun WidgetContent(context: Context, state: WidgetSnapshot) {
    val size = LocalSize.current
    val colors = widgetColors(context, state.settings)
    val compact = size.width < 150.dp || size.height < 100.dp
    val roomy = size.width >= 240.dp && size.height >= 160.dp
    val controls =
        (size.width >= 240.dp && size.height >= 160.dp) ||
            (size.width >= 150.dp && size.height >= 200.dp)
    val percent = (state.total.toDouble() / state.goal * 100).toInt()
    val padding = if (compact) 8.dp else 12.dp
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
        Row(
            GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                "HYDRI",
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            if (roomy)
                Text(
                    "$percent%",
                    style =
                        TextStyle(color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
        }
        Spacer(GlanceModifier.height(if (compact) 3.dp else 7.dp))
        if (roomy) {
            Row(
                GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Box(GlanceModifier.size(if (size.height >= 210.dp) 64.dp else 52.dp)) {
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
                        "$percent percent of daily goal",
                        modifier = GlanceModifier.fillMaxSize(),
                        colorFilter = ColorFilter.tint(accent),
                    )
                }
                Spacer(GlanceModifier.width(12.dp))
                Column(GlanceModifier.defaultWeight()) {
                    Text(
                        Volume.format(state.total, state.settings.unit),
                        style =
                            TextStyle(color = ink, fontSize = 22.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                    )
                    Text(
                        "of ${Volume.format(state.goal,state.settings.unit)}",
                        style = TextStyle(color = muted, fontSize = 12.sp),
                        maxLines = 1,
                    )
                    if (size.height >= 210.dp)
                        Text(
                            "${Volume.format((state.goal-state.total).coerceAtLeast(0),state.settings.unit)} left",
                            style = TextStyle(color = muted, fontSize = 11.sp),
                            maxLines = 1,
                        )
                }
            }
        } else {
            Text(
                Volume.format(state.total, state.settings.unit),
                style =
                    TextStyle(
                        color = ink,
                        fontSize = if (compact) 16.sp else 23.sp,
                        fontWeight = FontWeight.Bold,
                    ),
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
                if (compact) "$percent%"
                else "$percent% of ${Volume.format(state.goal,state.settings.unit)}",
                style = TextStyle(color = muted, fontSize = 11.sp),
                maxLines = 1,
            )
        }
        if (controls) {
            Spacer(GlanceModifier.height(8.dp))
            val favorites = state.bottles.take(if (size.width >= 240.dp) 2 else 1)
            if (favorites.isEmpty())
                Text(
                    "Favorite a bottle in Hydri",
                    style = TextStyle(color = muted, fontSize = 12.sp),
                    maxLines = 1,
                )
            else
                Row(
                    GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                ) {
                    favorites.forEachIndexed { index, bottle ->
                        if (index > 0) Spacer(GlanceModifier.width(8.dp))
                        Box(
                            GlanceModifier.defaultWeight()
                                .height(44.dp)
                                .background(
                                    ImageProvider(R.drawable.widget_surface),
                                    colorFilter = ColorFilter.tint(colors.primaryContainer),
                                )
                                .clickable(
                                    actionRunCallback<WidgetAdd>(
                                        actionParametersOf(
                                            ActionParameters.Key<String>("bottle") to bottle.id
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                                if (size.width >= 320.dp)
                                    Text(
                                        bottle.name,
                                        style =
                                            TextStyle(
                                                color = colors.onPrimaryContainer,
                                                fontSize = 11.sp,
                                            ),
                                        maxLines = 1,
                                    )
                                Text(
                                    "+ ${Volume.format(bottle.volume,state.settings.unit)}",
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
        repo.log(bottle.volume, bottle.name)
        HydriWidgets.update(context)
        if (settings.goalNotification && before < goal && before + bottle.volume >= goal)
            Reminders.notify(context, "You've reached today's hydration goal!", 2, settings)
    }
}
