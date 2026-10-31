package com.hydri.app

import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.*

class BaselineCaptureDeviceTest {
    @Test
    fun captureBeforeUpdate() = runBlocking {
        Assume.assumeTrue(
            "Explicit QA baseline capture only",
            InstrumentationRegistry.getArguments().getString("captureBaseline") == "true",
        )
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = context.applicationContext as HydriApplication
        app.repository.initialize()
        if (
            InstrumentationRegistry.getArguments().getString("seedBaseline") == "true" &&
                app.repository.dao.allEntries().isEmpty()
        ) {
            app.repository.log(250_000, "QA upgrade today")
            app.repository.seedHistory(
                500_000,
                "QA upgrade history",
                java.time.LocalDate.now().minusDays(1),
            )
            app.repository.dao.putBottle(
                com.hydri.app.data.Bottle("qa-upgrade", "QA custom bottle", 375_000, 5, 3, false, 3)
            )
            app.repository.preferences.save(
                com.hydri.app.data.Settings(onboarded = true, name = "QA")
            )
        }
        File(context.getExternalFilesDir(null), "upgrade-1.2.1.json")
            .writeText(app.repository.backup())
    }
}
