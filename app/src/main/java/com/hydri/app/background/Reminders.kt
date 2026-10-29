package com.hydri.app.background

import android.Manifest
import android.app.*
import android.appwidget.AppWidgetManager
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.hydri.app.*
import com.hydri.app.R
import com.hydri.app.data.*
import com.hydri.app.domain.Hydration
import java.time.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object Reminders {
    fun schedule(context: Context, s: Settings) {
        val manager = WorkManager.getInstance(context)
        if (!s.reminders) manager.cancelUniqueWork("hydration-reminders")
        else
            manager.enqueueUniquePeriodicWork(
                "hydration-reminders",
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<ReminderWorker>(s.frequency.toLong(), TimeUnit.MINUTES)
                    .setInitialDelay(s.frequency.toLong(), TimeUnit.MINUTES)
                    .build(),
            )
        if (!s.reminders || !s.evening) manager.cancelUniqueWork("hydration-evening")
        else {
            val now = ZonedDateTime.now()
            var next = now.withHour(20).withMinute(0).withSecond(0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            manager.enqueueUniqueWork(
                "hydration-evening",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<EveningWorker>()
                    .setInitialDelay(Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS)
                    .build(),
            )
        }
        val widgets = AppWidgetManager.getInstance(context)
        val hasWidgets =
            listOf(SmallWidgetReceiver::class.java, MediumWidgetReceiver::class.java).any {
                widgets.getAppWidgetIds(ComponentName(context, it)).isNotEmpty()
            }
        if (hasWidgets)
            manager.enqueueUniquePeriodicWork(
                "widget-refresh",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<WidgetWorker>(30, TimeUnit.MINUTES).build(),
            )
        else manager.cancelUniqueWork("widget-refresh")
    }

    fun notify(context: Context, message: String, id: Int, s: Settings) {
        if (
            Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
        )
            return
        val channel = "hydration-${s.sound}-${s.vibration}"
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                    channel,
                    "Hydration · ${if(s.sound) "sound" else "silent"} · ${if(s.vibration) "vibrate" else "no vibration"}",
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
                .apply {
                    enableVibration(s.vibration)
                    if (!s.sound) setSound(null, null)
                }
        )
        val intent =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        NotificationManagerCompat.from(context)
            .notify(
                id,
                NotificationCompat.Builder(context, channel)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("Hydri")
                    .setContentText(message)
                    .setContentIntent(intent)
                    .setAutoCancel(true)
                    .build(),
            )
    }

    suspend fun shouldRemind(context: Context): Pair<Settings, Boolean> {
        val repo = (context.applicationContext as HydriApplication).repository
        val s = repo.preferences.flow.first()
        val now = ZonedDateTime.now()
        val date = now.toLocalDate()
        val total = repo.dao.allEntries().filter { it.date == date.toString() }.sumOf { it.volume }
        val allowed =
            s.onboarded &&
                s.reminders &&
                Hydration.reminderAllowed(
                    now,
                    s.days,
                    s.start,
                    s.end,
                    s.quiet,
                    s.quietStart,
                    s.quietEnd,
                ) &&
                (!s.stopAtGoal || total < Repository.goalFor(repo.dao.allGoals(), date))
        return s to allowed
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val (s, allowed) = Reminders.shouldRemind(applicationContext)
        if (allowed)
            Reminders.notify(
                applicationContext,
                "Time for a little water. A quick sip can help you stay on track.",
                1,
                s,
            )
        HydriWidgets.update(applicationContext)
        return Result.success()
    }
}

class EveningWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val (s, allowed) = Reminders.shouldRemind(applicationContext)
        val repo = (applicationContext as HydriApplication).repository
        val today = LocalDate.now()
        val behind =
            repo.dao.allEntries().filter { it.date == today.toString() }.sumOf { it.volume } <
                Repository.goalFor(repo.dao.allGoals(), today)
        if (allowed && s.evening && behind)
            Reminders.notify(
                applicationContext,
                "A gentle evening check-in: there's still time for a little water.",
                1,
                s,
            )
        Reminders.schedule(applicationContext, s)
        return Result.success()
    }
}

class WidgetWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        HydriWidgets.update(applicationContext)
        return Result.success()
    }
}

class TimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action !in
                setOf(
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED,
                    Intent.ACTION_DATE_CHANGED,
                    Intent.ACTION_BOOT_COMPLETED,
                )
        )
            return
        val pending = goAsync()
        (context.applicationContext as HydriApplication).scope.launch {
            try {
                Reminders.schedule(
                    context,
                    (context.applicationContext as HydriApplication)
                        .repository
                        .preferences
                        .flow
                        .first(),
                )
                HydriWidgets.update(context)
            } finally {
                pending.finish()
            }
        }
    }
}
