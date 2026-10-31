package com.hydri.app

import android.app.*
import android.appwidget.*
import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import androidx.test.platform.app.InstrumentationRegistry
import com.hydri.app.background.*
import com.hydri.app.data.*
import com.hydri.app.domain.Volume
import com.hydri.app.ui.hydriColors
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*

class WidgetSyncDeviceTest {
    private val instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private val context
        get() = instrumentation.targetContext

    private lateinit var repo: Repository
    private lateinit var host: AppWidgetHost
    private val manager
        get() = AppWidgetManager.getInstance(context)

    private var snapshot: String? = null

    @Before
    fun setup() = runBlocking {
        Assume.assumeTrue(
            "Dedicated QA emulator only",
            InstrumentationRegistry.getArguments().getString("hydriQa") == "true",
        )
        repo = (context.applicationContext as HydriApplication).repository
        snapshot = repo.backup()
        shell("appwidget grantbind --package com.hydri.app --user 0")
        host = AppWidgetHost(context, 9876)
        host.deleteHost()
        instrumentation.runOnMainSync { host.startListening() }
        repo.preferences.save(
            repo.preferences.flow
                .first()
                .copy(theme = "Light", oled = false, dynamicColors = false, unit = "mL")
        )
    }

    @After
    fun cleanup() = runBlocking {
        if (snapshot != null) {
            instrumentation.runOnMainSync {
                host.stopListening()
                host.deleteHost()
            }
            repo.restore(snapshot!!)
            HydriWidgets.update(context)
            shell("appwidget revokebind --package com.hydri.app --user 0")
        }
    }

    private fun shell(command: String) {
        instrumentation.uiAutomation.executeShellCommand(command).use { fd ->
            java.io.FileInputStream(fd.fileDescriptor).use { it.readBytes() }
        }
    }

    private fun textViews(view: View): List<TextView> =
        when (view) {
            is TextView -> listOf(view)
            is ViewGroup -> (0 until view.childCount).flatMap { textViews(view.getChildAt(it)) }
            else -> emptyList()
        }

    private fun text(view: View): List<String> {
        var result = emptyList<String>()
        instrumentation.runOnMainSync { result = textViews(view).map { it.text.toString() } }
        return result
    }

    private suspend fun await(message: String, condition: () -> Boolean) {
        repeat(150) {
            if (condition()) return
            delay(100)
        }
        fail(message)
    }

    private fun dimensions(width: Int, height: Int) =
        Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, width)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, width)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, height)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, height)
            putParcelableArrayList(
                AppWidgetManager.OPTION_APPWIDGET_SIZES,
                arrayListOf(android.util.SizeF(width.toFloat(), height.toFloat())),
            )
        }

    private fun resize(view: AppWidgetHostView, id: Int, width: Int, height: Int) {
        manager.updateAppWidgetOptions(id, dimensions(width, height))
        instrumentation.runOnMainSync {
            val density = context.resources.displayMetrics.density
            // The options describe content dp, while AppWidgetHostView adds launcher padding.
            view.measure(
                View.MeasureSpec.makeMeasureSpec(
                    (width * density).toInt() + view.paddingLeft + view.paddingRight,
                    View.MeasureSpec.EXACTLY,
                ),
                View.MeasureSpec.makeMeasureSpec(
                    (height * density).toInt() + view.paddingTop + view.paddingBottom,
                    View.MeasureSpec.EXACTLY,
                ),
            )
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        }
    }

    private suspend fun bind(
        receiver: Class<*>,
        width: Int = 320,
        height: Int = 220,
    ): Pair<Int, AppWidgetHostView> {
        val id = host.allocateAppWidgetId()
        assertTrue(
            manager.bindAppWidgetIdIfAllowed(
                id,
                ComponentName(context, receiver),
                dimensions(width, height),
            )
        )
        lateinit var view: AppWidgetHostView
        instrumentation.runOnMainSync {
            view = host.createView(context, id, manager.getAppWidgetInfo(id))
        }
        resize(view, id, width, height)
        HydriWidgets.update(context)
        await("Widget did not render") { text(view).contains("HYDRI") }
        return id to view
    }

    private fun capture(view: View, name: String) {
        instrumentation.runOnMainSync {
            view.measure(
                View.MeasureSpec.makeMeasureSpec(view.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(view.height, View.MeasureSpec.EXACTLY),
            )
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            File(context.getExternalFilesDir(null), "v120-widget-$name.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    @Test
    fun widgetsNotificationsAndShortcutsUseAllSevenAppLanguages() = runBlocking {
        androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java).use {
            val originalLanguage = com.hydri.app.i18n.AppLocales.current(context)
            try {
                val (id, view) = bind(MediumWidgetReceiver::class.java)
                shell("pm grant com.hydri.app android.permission.POST_NOTIFICATIONS")
                listOf("en", "es", "fr", "zh-Hans", "ko", "ja", "id").forEach { code ->
                    instrumentation.runOnMainSync { com.hydri.app.i18n.AppLocales.select(code) }
                    await("App language did not change: $code") {
                        com.hydri.app.i18n.AppLocales.current(context) == code &&
                            com.hydri.app.i18n.AppText.locale.language ==
                                java.util.Locale.forLanguageTag(code).language
                    }
                    val goal = Repository.goalFor(repo.dao.allGoals(), LocalDate.now())
                    val expected =
                        com.hydri.app.i18n.tr(R.string.of_value, Volume.format(goal, "mL"))
                    await("Widget did not adopt language $code: ${text(view)}") {
                        text(view).contains(expected)
                    }
                    val shortcuts =
                        context.getSystemService(android.content.pm.ShortcutManager::class.java)
                    await("Shortcut label not localized: $code") {
                        shortcuts.dynamicShortcuts
                            .firstOrNull { it.id == "log-water" }
                            ?.shortLabel
                            .toString() == com.hydri.app.i18n.tr(R.string.shortcut_log)
                    }
                    val message =
                        com.hydri.app.i18n.tr(
                            R.string.time_for_a_little_water_a_quick_sip_can_help_you_stay_on_t
                        )
                    Reminders.notify(
                        context,
                        message,
                        941,
                        repo.preferences.flow.first().copy(sound = false, vibration = false),
                    )
                    val notifications =
                        context.getSystemService(android.app.NotificationManager::class.java)
                    await("Notification missing for $code") {
                        notifications.activeNotifications.any { it.id == 941 }
                    }
                    assertEquals(
                        message,
                        notifications.activeNotifications
                            .first { it.id == 941 }
                            .notification
                            .extras
                            .getCharSequence(android.app.Notification.EXTRA_TEXT)
                            .toString(),
                    )
                    notifications.cancel(941)
                    capture(view, "language-$code")
                }
                resize(view, id, 160, 120)
                assertTrue(text(view).isNotEmpty())
            } finally {
                instrumentation.runOnMainSync {
                    com.hydri.app.i18n.AppLocales.select(originalLanguage)
                }
                await("Restore original language") {
                    com.hydri.app.i18n.AppLocales.current(context) == originalLanguage
                }
            }
        }
    }

    @Test
    fun widgetQuickAddAndLiveChangesRefreshWithoutOpeningApp() = runBlocking {
        val (id, view) = bind(MediumWidgetReceiver::class.java)
        val today = LocalDate.now().toString()
        val initial = repo.dao.entriesOn(today).first().sumOf { it.volume }
        await("Initial total wrong") { text(view).contains(Volume.format(initial, "mL")) }
        val bottle = repo.dao.allBottles().first { it.favorite }
        var activities = 0
        val listener =
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(a: Activity, b: Bundle?) {
                    activities++
                }

                override fun onActivityStarted(a: Activity) {}

                override fun onActivityResumed(a: Activity) {}

                override fun onActivityPaused(a: Activity) {}

                override fun onActivityStopped(a: Activity) {}

                override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}

                override fun onActivityDestroyed(a: Activity) {}
            }
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(listener)
        try {
            var clicked = false
            instrumentation.runOnMainSync {
                var node: View? =
                    textViews(view).first {
                        it.text.toString() == "+ ${Volume.format(bottle.volume,"mL")}"
                    }
                while (node != null && node !== view) {
                    if (node.isClickable) {
                        clicked = node.performClick()
                        break
                    }
                    node = node.parent as? View
                }
            }
            assertTrue("Could not click the widget's real quick-add control", clicked)
            var newEntry: WaterEntry? = null
            repeat(100) {
                if (newEntry == null) {
                    delay(100)
                    newEntry =
                        repo.dao.allEntries().firstOrNull {
                            it.date == today &&
                                it.volume == bottle.volume &&
                                it.timestamp > System.currentTimeMillis() - 15000
                        }
                }
            }
            assertNotNull("Widget button did not log water", newEntry)
            val added = initial + bottle.volume
            await("Widget total did not refresh after quick-add") {
                text(view).contains(Volume.format(added, "mL"))
            }
            assertEquals("Widget logging must not launch an activity", 0, activities)
            repo.dao.putEntry(newEntry!!.copy(volume = 750_000))
            HydriWidgets.update(context)
            await("Edit did not refresh widget") {
                text(view).contains(Volume.format(initial + 750_000, "mL"))
            }
            repo.dao.deleteEntry(newEntry!!.id)
            HydriWidgets.update(context)
            await("Delete did not refresh widget") {
                text(view).contains(Volume.format(initial, "mL"))
            }
            repo.dao.putEntry(newEntry!!)
            HydriWidgets.update(context)
            await("Restore/Undo did not refresh widget") {
                text(view).contains(Volume.format(added, "mL"))
            }
            repo.goal(3_000_000)
            HydriWidgets.update(context)
            await("Goal did not refresh widget") { text(view).contains("of 3,000 mL") }
            repo.dao.clearEntriesOn(today)
            HydriWidgets.update(context)
            await("Reset did not refresh widget") { text(view).contains("0 mL") }
            val appEntry = repo.log(600_000, "App-side test")
            HydriWidgets.update(context)
            await("App logging did not refresh widget") { text(view).contains("600 mL") }
            repo.dao.deleteEntry(appEntry.id)
            HydriWidgets.update(context)
            await("Logging Undo did not refresh widget") { text(view).contains("0 mL") }
            capture(view, "live-updates")
        } finally {
            (context.applicationContext as Application).unregisterActivityLifecycleCallbacks(
                listener
            )
            host.deleteAppWidgetId(id)
        }
    }

    @Test
    fun materialYouPaletteAndSystemNightChangesReachWidgets() = runBlocking {
        val resolver = context.contentResolver
        val key = "theme_customization_overlay_packages"
        val automation = instrumentation.uiAutomation
        automation.adoptShellPermissionIdentity("android.permission.WRITE_SECURE_SETTINGS")
        val originalPalette = android.provider.Settings.Secure.getString(resolver, key)
        val wasDark =
            context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        try {
            val s =
                repo.preferences.flow
                    .first()
                    .copy(theme = "System", dynamicColors = true, oled = false)
            repo.preferences.save(s)
            val (id, view) = bind(MediumWidgetReceiver::class.java)
            try {
                shell("cmd uimode night no")
                val before = hydriColors(context, s, false).primary
                android.provider.Settings.Secure.putString(
                    resolver,
                    key,
                    org.json
                        .JSONObject()
                        .apply {
                            put("android.theme.customization.system_palette", "FF008577")
                            put("android.theme.customization.accent_color", "FF008577")
                            put("android.theme.customization.color_source", "preset")
                            put("android.theme.customization.theme_style", "TONAL_SPOT")
                            put("_applied_timestamp", System.currentTimeMillis())
                        }
                        .toString(),
                )
                await("Android did not apply the changed Material You palette") {
                    hydriColors(context, s, false).primary != before
                }
                suspend fun awaitAccent() {
                    await("Widget did not resolve the updated device palette") {
                        val expected = hydriColors(context, s).primary.toArgb()
                        var actual = 0
                        instrumentation.runOnMainSync {
                            actual =
                                textViews(view)
                                    .firstOrNull { it.text.toString() == "HYDRI" }
                                    ?.currentTextColor ?: 0
                        }
                        actual == expected
                    }
                }
                awaitAccent()
                capture(view, "material-you-palette-change")
                shell("cmd uimode night yes")
                await("Night configuration did not change") {
                    context.resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                        android.content.res.Configuration.UI_MODE_NIGHT_YES
                }
                awaitAccent()
                capture(view, "material-you-system-night")
            } finally {
                host.deleteAppWidgetId(id)
            }
        } finally {
            android.provider.Settings.Secure.putString(resolver, key, originalPalette)
            shell("cmd uimode night ${if (wasDark) "yes" else "no"}")
            automation.dropShellPermissionIdentity()
        }
    }

    @Test
    fun responsiveSizesThemesAndAccentsRender() = runBlocking {
        for (receiver in
            listOf(SmallWidgetReceiver::class.java, MediumWidgetReceiver::class.java)) {
            repo.preferences.save(
                repo.preferences.flow
                    .first()
                    .copy(theme = "Light", oled = false, dynamicColors = false)
            )
            val (id, view) = bind(receiver)
            try {
                for ((width, height) in
                    listOf(100 to 80, 160 to 120, 160 to 200, 240 to 110, 240 to 160, 320 to 220)) {
                    resize(view, id, width, height)
                    HydriWidgets.update(context)
                    await("Missing content at ${width}x$height") { text(view).contains("HYDRI") }
                    delay(750)
                    capture(view, "${receiver.simpleName}-${width}x$height")
                    val hasControls =
                        (width >= 240 && height >= 160) || (width >= 150 && height >= 200)
                    assertEquals(
                        "Quick-add controls at ${width}x$height",
                        hasControls,
                        text(view).any { it.startsWith("+ ") },
                    )
                }
                resize(view, id, 320, 220)
                for (settings in
                    listOf(
                        Settings(theme = "Light", accent = 1),
                        Settings(theme = "Dark", accent = 2),
                        Settings(theme = "Dark", oled = true, accent = 3),
                        Settings(theme = "System", dynamicColors = true),
                        Settings(theme = "Light", dynamicColors = true),
                        Settings(theme = "Dark", dynamicColors = true, oled = true),
                    )) {
                    val s =
                        repo.preferences.flow
                            .first()
                            .copy(
                                theme = settings.theme,
                                accent = settings.accent,
                                oled = settings.oled,
                                dynamicColors = settings.dynamicColors,
                            )
                    repo.preferences.save(s)
                    HydriWidgets.update(context)
                    val expected = hydriColors(context, s).primary.toArgb()
                    await("Widget accent did not match ${s.theme}, dynamic=${s.dynamicColors}") {
                        var color = 0
                        instrumentation.runOnMainSync {
                            color =
                                textViews(view)
                                    .firstOrNull { it.text.toString() == "HYDRI" }
                                    ?.currentTextColor ?: 0
                        }
                        color == expected
                    }
                    if (s.oled && s.theme == "Dark")
                        assertEquals(
                            androidx.compose.ui.graphics.Color.Black,
                            widgetColors(context, s).surface.getColor(context),
                        )
                    capture(view, "${receiver.simpleName}-${s.theme}-${s.dynamicColors}-${s.oled}")
                }
            } finally {
                host.deleteAppWidgetId(id)
            }
        }
    }
}
