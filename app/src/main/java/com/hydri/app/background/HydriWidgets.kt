package com.hydri.app.background

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.*
import androidx.glance.action.*
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.*
import androidx.glance.layout.*
import androidx.glance.text.*
import com.hydri.app.*
import com.hydri.app.domain.Volume
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object HydriWidgets {
    suspend fun update(context: Context) {
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

class SmallHydriWidget : HydriWidget(false)

class MediumHydriWidget : HydriWidget(true)

open class HydriWidget(private val medium: Boolean) : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = (context.applicationContext as HydriApplication).repository
        repo.initialize()
        val s = repo.preferences.flow.first()
        val today = LocalDate.now()
        val total = repo.dao.allEntries().filter { it.date == today.toString() }.sumOf { it.volume }
        val goal = com.hydri.app.data.Repository.goalFor(repo.dao.allGoals(), today)
        val bottles = repo.dao.allBottles().filter { it.favorite }.take(2)
        provideContent {
            GlanceTheme {
                Column(
                    GlanceModifier.fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground)
                        .appWidgetBackground()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    Text(
                        "HYDRI · TODAY",
                        style =
                            TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontWeight = FontWeight.Bold,
                            ),
                    )
                    Text(
                        Volume.format(total, s.unit),
                        style =
                            TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontWeight = FontWeight.Bold,
                            ),
                    )
                    Text(
                        "${(total.toDouble()/goal*100).toInt()}% of ${Volume.format(goal,s.unit)}",
                        style = TextStyle(color = GlanceTheme.colors.onSurface),
                    )
                    Spacer(GlanceModifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = (total.toFloat() / goal).coerceIn(0f, 1f),
                        modifier = GlanceModifier.fillMaxWidth(),
                    )
                    if (medium) {
                        Text(
                            "${Volume.format((goal-total).coerceAtLeast(0),s.unit)} remaining",
                            style = TextStyle(color = GlanceTheme.colors.onSurface),
                        )
                        Row {
                            bottles.forEach { b ->
                                Button(
                                    "+ ${Volume.format(b.volume,s.unit)}",
                                    actionRunCallback<WidgetAdd>(
                                        actionParametersOf(
                                            ActionParameters.Key<String>("bottle") to b.id
                                        )
                                    ),
                                )
                            }
                        }
                    }
                    Text(
                        "Open Hydri",
                        modifier =
                            GlanceModifier.padding(top = 8.dp)
                                .clickable(actionStartActivity<MainActivity>()),
                        style = TextStyle(color = GlanceTheme.colors.primary),
                    )
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
        val s = repo.preferences.flow.first()
        val today = LocalDate.now()
        val before =
            repo.dao.allEntries().filter { it.date == today.toString() }.sumOf { it.volume }
        val goal = com.hydri.app.data.Repository.goalFor(repo.dao.allGoals(), today)
        repo.log(bottle.volume, bottle.name)
        if (s.goalNotification && before < goal && before + bottle.volume >= goal)
            Reminders.notify(context, "You've reached today's hydration goal!", 2, s)
        HydriWidgets.update(context)
    }
}
