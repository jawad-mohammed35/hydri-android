package com.hydri.app

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.hydri.app.data.*
import com.hydri.app.ui.primaryTabs
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class FinalVisualDeviceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var repo: Repository
    private var snapshot: String? = null

    @Before
    fun setup() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("hydriQa") == "true")
        repo = (context.applicationContext as HydriApplication).repository
        runBlocking {
            snapshot = repo.backup()
            repo.preferences.save(
                repo.preferences.flow.first().copy(onboarded = true, haptics = false)
            )
        }
        rule.waitUntil(10000) {
            rule.onAllNodesWithTag("sliding-navigation").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @After
    fun restore() {
        snapshot?.let { runBlocking { repo.restore(it) } }
    }

    @Test
    fun reselectScrollsEveryPrimaryPageAndSwitchingRetainsPosition() {
        fun scroll() = rule.onNodeWithTag("page-scroll")
        fun position() =
            scroll()
                .fetchSemanticsNode()
                .config[SemanticsProperties.VerticalScrollAxisRange]
                .value()
        primaryTabs.forEach { name ->
            rule.onNodeWithContentDescription(name).performClick()
            rule.waitForIdle()
            primaryTabs.forEach { tab ->
                rule
                    .onNodeWithTag(
                        "tab-icon-$tab-${if (tab == name) "filled" else "outlined"}",
                        useUnmergedTree = true,
                    )
                    .assertExists()
            }
            scroll().performTouchInput { swipeUp() }
            rule.waitForIdle()
            val before = position()
            assertTrue("$name has scrollable content", before > 0)
            val other = if (name == "Home") "Bottles" else "Home"
            rule.onNodeWithContentDescription(other).performClick()
            rule.onNodeWithContentDescription(name).performClick()
            rule.waitForIdle()
            assertEquals("$name restored scroll", before, position(), 2f)
            rule.onNodeWithContentDescription(name).performClick()
            rule.waitForIdle()
            assertEquals("$name reselect top", 0f, position(), 1f)
        }
    }

    @Test
    fun bottleSnapshotAndCustomFallbackAreShownAcrossThemes() {
        val bottle = Bottle("snapshot-qa", "Snapshot bottle", 315000, 5, 3)
        runBlocking {
            repo.dao.putBottle(bottle)
            repo.log(bottle.volume, bottle.name, bottle = bottle)
            repo.log(123000, "Custom water")
            repo.dao.putBottle(bottle.copy(icon = 0, color = 1))
            repo.dao.deleteBottle(bottle.id)
        }
        listOf(
                Triple("Light", false, false),
                Triple("Dark", false, false),
                Triple("Dark", true, false),
                Triple("Dark", true, true),
            )
            .forEach { (theme, oled, dynamic) ->
                runBlocking {
                    repo.preferences.save(
                        repo.preferences.flow
                            .first()
                            .copy(theme = theme, oled = oled, dynamicColors = dynamic)
                    )
                }
                rule.onNodeWithContentDescription("Mug sip").performScrollTo().assertExists()
                rule.onAllNodesWithContentDescription("Custom water sip").onFirst().assertExists()
            }
    }

    @Test
    fun quickAddHeldCornersStayClippedAndLoggingWorks() {
        val b = runBlocking { repo.dao.allBottles().first { it.favorite } }
        listOf(
                Triple("Light", false, false),
                Triple("Dark", false, false),
                Triple("Dark", true, false),
                Triple("Light", false, true),
            )
            .forEachIndexed { index, (theme, oled, dynamic) ->
                runBlocking {
                    repo.preferences.save(
                        repo.preferences.flow
                            .first()
                            .copy(theme = theme, oled = oled, dynamicColors = dynamic)
                    )
                }
                val button = rule.onNode(hasContentDescription("Log ${b.name}", substring = true))
                button.performScrollTo().clearFloatingNavigation(rule)
                rule.waitForIdle()
                Thread.sleep(
                    400
                ) // Await DataStore-driven theme recomposition and Android rendering.
                rule.waitForIdle()
                val bounds = button.getUnclippedBoundsInRoot()
                val density = rule.activity.resources.displayMetrics.density
                val x = ((bounds.left.value + 2) * density).toInt()
                val y = ((bounds.top.value + 2) * density).toInt()
                val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
                val before = automation.takeScreenshot()
                val count = runBlocking { repo.dao.allEntries().size }
                button.performTouchInput { down(center) }
                rule.mainClock.advanceTimeBy(500)
                Thread.sleep(150)
                val pressed = automation.takeScreenshot()
                assertEquals(
                    "Ripple outside rounded corner ($theme)",
                    before.getPixel(x, y),
                    pressed.getPixel(x, y),
                )
                File(context.getExternalFilesDir(null), "v123-pressed-$index.png")
                    .outputStream()
                    .use { pressed.compress(Bitmap.CompressFormat.PNG, 100, it) }
                button.performTouchInput { up() }
                rule.waitUntil(10000) { runBlocking { repo.dao.allEntries().size == count + 1 } }
                val entry = runBlocking { repo.dao.allEntries().first { it.bottleId == b.id } }
                assertEquals(b.icon, entry.bottleIcon)
                assertEquals(b.color, entry.bottleColor)
            }
    }

    @Test
    fun portraitAndApacheAboutAreConfigured() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, rule.activity.requestedOrientation)
        assertEquals(
            Configuration.ORIENTATION_PORTRAIT,
            rule.activity.resources.configuration.orientation,
        )
        rule.onNodeWithContentDescription("More").performClick()
        rule.onNodeWithText("About").performScrollTo().clearFloatingNavigation(rule).performClick()
        rule.onNodeWithText("Hydri — Apache License 2.0").performScrollTo().assertExists()
        rule.onNodeWithText("Copyright 2026 Jawad Mohammed").assertExists()
        assertTrue(
            context.assets
                .open("Hydri-NOTICE.txt")
                .bufferedReader()
                .readText()
                .contains("Copyright 2026 Jawad Mohammed")
        )
        assertTrue(
            context.assets
                .open("Protobuf-BSD-3-Clause.txt")
                .bufferedReader()
                .readText()
                .contains("Copyright 2008 Google Inc.")
        )
    }

    @Test
    fun snapshotsSurviveBackupRestoreAndLegacyBackupsRemainValid() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, HydriDatabase::class.java).build()
        try {
            val isolated = Repository(db, repo.preferences)
            isolated.initialize()
            val b = isolated.defaults().first().copy(icon = 6, color = 4)
            val entry = isolated.log(b.volume, b.name, bottle = b)
            val backup = isolated.backup()
            isolated.dao.clearEntries()
            isolated.restore(backup)
            assertEquals(entry, isolated.dao.allEntries().single())
            val json = org.json.JSONObject(backup)
            json.getJSONArray("entries").getJSONObject(0).apply {
                remove("bottleId")
                remove("bottleIcon")
                remove("bottleColor")
            }
            isolated.restore(json.toString())
            assertEquals(
                entry.copy(bottleId = null, bottleIcon = null, bottleColor = null),
                isolated.dao.allEntries().single(),
            )
        } finally {
            db.close()
        }
    }

    @Test
    fun schemaOneMigrationPreservesRowsWithoutGuessingBottle() = runBlocking {
        val file = context.getDatabasePath("migration-123-qa.db")
        context.deleteDatabase(file.name)
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            old.execSQL(
                "CREATE TABLE entries (id TEXT NOT NULL PRIMARY KEY, volume INTEGER NOT NULL, timestamp INTEGER NOT NULL, date TEXT NOT NULL, name TEXT NOT NULL, zone TEXT NOT NULL)"
            )
            old.execSQL(
                "CREATE TABLE bottles (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, volume INTEGER NOT NULL, icon INTEGER NOT NULL, color INTEGER NOT NULL, favorite INTEGER NOT NULL, position INTEGER NOT NULL)"
            )
            old.execSQL(
                "CREATE TABLE goals (date TEXT NOT NULL PRIMARY KEY, volume INTEGER NOT NULL)"
            )
            old.execSQL(
                "INSERT INTO entries VALUES ('legacy', 456000, 1700000000000, '2023-11-14', 'Water bottle', 'UTC')"
            )
            old.execSQL("INSERT INTO bottles VALUES ('bottle', 'Water bottle', 500000, 3, 4, 1, 0)")
            old.execSQL("INSERT INTO goals VALUES ('2023-11-14', 2500000)")
            old.version = 1
        }
        val db =
            Room.databaseBuilder(context, HydriDatabase::class.java, file.name)
                .addMigrations(HydriDatabase.MIGRATION_1_2)
                .build()
        try {
            assertEquals(
                WaterEntry("legacy", 456000, 1700000000000, "2023-11-14", "Water bottle", "UTC"),
                db.dao().allEntries().single(),
            )
            assertEquals(3, db.dao().allBottles().single().icon)
            assertEquals(2500000L, db.dao().allGoals().single().volume)
        } finally {
            db.close()
            context.deleteDatabase(file.name)
        }
    }
}
