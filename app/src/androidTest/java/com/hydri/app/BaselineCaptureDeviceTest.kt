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
        File(context.getExternalFilesDir(null), "upgrade-1.2.0.json")
            .writeText(app.repository.backup())
    }
}
