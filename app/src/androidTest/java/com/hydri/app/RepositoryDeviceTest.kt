package com.hydri.app

import android.appwidget.*
import android.content.ComponentName
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.glance.action.*
import androidx.glance.appwidget.AppWidgetId
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import com.hydri.app.background.*
import com.hydri.app.data.*
import java.time.LocalDate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryDeviceTest {
    @Before
    fun requireIsolatedQa() {
        Assume.assumeTrue(
            "Use the dedicated QA emulator",
            InstrumentationRegistry.getArguments().getString("hydriQa") == "true",
        )
    }

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun backupRoundTripAndInvalidRestoreIsNonDestructive() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, HydriDatabase::class.java).build()
        val prefs = PreferenceStore(context)
        val previous = prefs.flow.first()
        val repo = Repository(db, prefs)
        try {
            repo.initialize()
            prefs.save(
                Settings(
                    onboarded = true,
                    name = "Test",
                    unit = "oz",
                    theme = "Dark",
                    oled = true,
                    accent = 2,
                )
            )
            repo.log(473176, "=formula", LocalDate.now().minusDays(1))
            repo.goal(3_000_000)
            val snapshot = repo.backup()
            val csv = repo.csv()
            assertTrue(csv.contains("'=formula"))
            assertTrue(csv.contains("473.176"))
            repo.dao.clearEntries()
            prefs.save(Settings())
            repo.restore(snapshot)
            assertEquals(473176L, repo.dao.allEntries().single().volume)
            assertEquals("Test", prefs.flow.first().name)
            assertEquals("oz", prefs.flow.first().unit)
            val bad = JSONObject(snapshot)
            bad.getJSONArray("entries").getJSONObject(0).put("volume", -1)
            try {
                repo.restore(bad.toString())
                fail("Invalid backup accepted")
            } catch (_: IllegalArgumentException) {}
            assertEquals(473176L, repo.dao.allEntries().single().volume)
            assertEquals(snapshot, repo.backup())
        } finally {
            prefs.save(previous)
            db.close()
        }
    }

    @Test
    fun diskPersistenceAndHistoricalDate() = runBlocking {
        val file = "hydri-device-test.db"
        context.deleteDatabase(file)
        val first = Room.databaseBuilder(context, HydriDatabase::class.java, file).build()
        val repo = Repository(first, PreferenceStore(context))
        repo.initialize()
        val yesterday = LocalDate.now().minusDays(1)
        repo.log(500_000, "Glass", yesterday)
        repo.goal(3_000_000)
        first.close()
        val second = Room.databaseBuilder(context, HydriDatabase::class.java, file).build()
        try {
            assertEquals(yesterday.toString(), second.dao().allEntries().single().date)
            assertEquals(500_000L, second.dao().allEntries().single().volume)
            assertEquals(2_500_000L, Repository.goalFor(second.dao().allGoals(), yesterday))
            assertEquals(3_000_000L, Repository.goalFor(second.dao().allGoals(), LocalDate.now()))
        } finally {
            second.close()
            context.deleteDatabase(file)
        }
    }

    @Test
    fun reminderSchedulingAndCancellation() {
        val manager = WorkManager.getInstance(context)
        Reminders.schedule(
            context,
            Settings(onboarded = true, reminders = true, evening = true, frequency = 60),
        )
        assertTrue(
            manager.getWorkInfosForUniqueWork("hydration-reminders").get().any {
                !it.state.isFinished
            }
        )
        assertTrue(
            manager.getWorkInfosForUniqueWork("hydration-evening").get().any {
                !it.state.isFinished
            }
        )
        Reminders.schedule(context, Settings())
        // Query queued after cancellation, so this observes the committed cancellation.
        assertTrue(
            manager.getWorkInfosForUniqueWork("hydration-reminders").get().all {
                it.state.isFinished
            }
        )
    }

    @Test fun widgetsUpdateOffline() = runBlocking { HydriWidgets.update(context) }

    @Test
    fun boundWidgetsRenderAndFavoriteQuickAddWorks() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        fun shell(command: String) {
            instrumentation.uiAutomation.executeShellCommand(command).use { fd ->
                java.io.FileInputStream(fd.fileDescriptor).use { it.readBytes() }
            }
        }
        shell("appwidget grantbind --package com.hydri.app --user 0")
        val host = AppWidgetHost(context, 4242)
        val manager = AppWidgetManager.getInstance(context)
        val repo = (context.applicationContext as HydriApplication).repository
        val snapshot = repo.backup()
        try {
            for (receiver in
                listOf(SmallWidgetReceiver::class.java, MediumWidgetReceiver::class.java)) {
                val id = host.allocateAppWidgetId()
                val options =
                    Bundle().apply {
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 320)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 320)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 180)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 180)
                    }
                assertTrue(
                    manager.bindAppWidgetIdIfAllowed(id, ComponentName(context, receiver), options)
                )
                lateinit var view: AppWidgetHostView
                instrumentation.runOnMainSync {
                    host.startListening()
                    view = host.createView(context, id, manager.getAppWidgetInfo(id))
                }
                HydriWidgets.update(context)
                fun texts(v: View): List<String> =
                    when (v) {
                        is TextView -> listOf(v.text.toString())
                        is ViewGroup -> (0 until v.childCount).flatMap { texts(v.getChildAt(it)) }
                        else -> emptyList()
                    }
                var rendered = false
                repeat(100) {
                    if (!rendered) {
                        delay(100)
                        instrumentation.runOnMainSync {
                            rendered = texts(view).any { it.contains("HYDRI") }
                        }
                    }
                }
                assertTrue("Widget content was not rendered for ${receiver.simpleName}", rendered)
                if (receiver == MediumWidgetReceiver::class.java) {
                    val bottle = repo.dao.allBottles().first { it.favorite }
                    val before = repo.dao.allEntries().sumOf { it.volume }
                    WidgetAdd()
                        .onAction(
                            context,
                            AppWidgetId(id),
                            actionParametersOf(ActionParameters.Key<String>("bottle") to bottle.id),
                        )
                    assertEquals(before + bottle.volume, repo.dao.allEntries().sumOf { it.volume })
                }
                host.deleteAppWidgetId(id)
            }
        } finally {
            host.stopListening()
            host.deleteHost()
            repo.restore(snapshot)
            shell("appwidget revokebind --package com.hydri.app --user 0")
        }
    }

    @Test
    fun notificationPostsWithRuntimePermission() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        if (android.os.Build.VERSION.SDK_INT >= 33)
            instrumentation.uiAutomation
                .executeShellCommand("pm grant com.hydri.app android.permission.POST_NOTIFICATIONS")
                .use { fd -> java.io.FileInputStream(fd.fileDescriptor).use { it.readBytes() } }
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        Reminders.notify(
            context,
            "Device test reminder",
            99,
            Settings(sound = false, vibration = false),
        )
        var posted = false
        repeat(40) {
            if (!posted) {
                Thread.sleep(50)
                posted = manager.activeNotifications.any { it.id == 99 }
            }
        }
        assertTrue("Notification did not appear", posted)
        manager.cancel(99)
    }
}
