// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0
package com.hydri.app

import android.appwidget.*
import android.content.ComponentName
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.platform.app.InstrumentationRegistry
import com.hydri.app.background.*
import java.io.File
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*

/** A separate host preserves real provider IDs/options across an external update and reboot. */
class WidgetRestartProbeTest {
    @Test
    fun widgetIdentityAndLauncherDimensionsSurviveUpdateAndReboot() =
        runBlocking<Unit> {
            val args = InstrumentationRegistry.getArguments()
            Assume.assumeTrue(
                args.getString("hydriQa") == "true" && args.getString("widgetProbe") == "true"
            )
            val inst = InstrumentationRegistry.getInstrumentation()
            val context = inst.targetContext
            val manager = AppWidgetManager.getInstance(context)
            val host = AppWidgetHost(context, 9877)
            val file = File(context.getExternalFilesDir(null), "v150-widget-restart.txt")
            fun shell(command: String) {
                inst.uiAutomation.executeShellCommand(command).use {
                    java.io.FileInputStream(it.fileDescriptor).readBytes()
                }
            }
            fun texts(view: View): List<String> =
                when (view) {
                    is TextView -> listOf(view.text.toString())
                    is ViewGroup -> (0 until view.childCount).flatMap { texts(view.getChildAt(it)) }
                    else -> emptyList()
                }
            val specs =
                listOf(
                    Triple(SmallWidgetReceiver::class.java, 180, 340),
                    Triple(MediumWidgetReceiver::class.java, 360, 360),
                )
            if (args.getString("phase") == "prepare") {
                shell("appwidget grantbind --package com.hydri.app --user 0")
                host.deleteHost()
                val ids =
                    specs.map { (type, w, h) ->
                        val id = host.allocateAppWidgetId()
                        val options =
                            Bundle().apply {
                                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, w)
                                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, w)
                                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, h)
                                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, h)
                                putParcelableArrayList(
                                    AppWidgetManager.OPTION_APPWIDGET_SIZES,
                                    arrayListOf(SizeF(w.toFloat(), h.toFloat())),
                                )
                            }
                        assertTrue(
                            manager.bindAppWidgetIdIfAllowed(
                                id,
                                ComponentName(context, type),
                                options,
                            )
                        )
                        id
                    }
                file.writeText(ids.joinToString(","))
                HydriWidgets.update(context)
            } else {
                assertTrue("Run prepare before the external update/reboot", file.exists())
                val ids = file.readText().split(",").map { it.toInt() }
                inst.runOnMainSync { host.startListening() }
                try {
                    specs.forEachIndexed { index, (type, w, h) ->
                        val id = ids[index]
                        assertEquals(
                            ComponentName(context, type),
                            manager.getAppWidgetInfo(id).provider,
                        )
                        val options = manager.getAppWidgetOptions(id)
                        assertEquals(w, options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH))
                        assertEquals(
                            h,
                            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT),
                        )
                        lateinit var view: AppWidgetHostView
                        inst.runOnMainSync {
                            view = host.createView(context, id, manager.getAppWidgetInfo(id))
                        }
                        HydriWidgets.update(context)
                        var rendered = false
                        repeat(150) {
                            if (!rendered) {
                                inst.runOnMainSync { rendered = texts(view).contains("HYDRI") }
                                if (!rendered) delay(100)
                            }
                        }
                        assertTrue("Restored provider did not render: ${type.simpleName}", rendered)
                    }
                } finally {
                    inst.runOnMainSync {
                        host.stopListening()
                        host.deleteHost()
                    }
                    shell("appwidget revokebind --package com.hydri.app --user 0")
                }
            }
        }
}
