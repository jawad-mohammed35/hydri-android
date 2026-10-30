// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0

package com.hydri.app

import android.graphics.*
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.*
import org.junit.Assert.*

class LauncherIconDeviceTest {
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun isolated() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("hydriQa") == "true")
    }

    @Test
    fun installedIconHasAdaptiveLayersAndSafeMonochrome() {
        val app = context.packageManager.getApplicationInfo(context.packageName, 0)
        val icon = app.loadIcon(context.packageManager)
        assertTrue(icon is AdaptiveIconDrawable)
        icon as AdaptiveIconDrawable
        assertNotNull(icon.background)
        assertNotNull(icon.foreground)
        if (android.os.Build.VERSION.SDK_INT >= 33) assertNotNull(icon.monochrome)
        listOf(R.drawable.ic_launcher_foreground, R.drawable.ic_launcher_monochrome).forEach { id ->
            val bitmap = Bitmap.createBitmap(108, 108, Bitmap.Config.ARGB_8888)
            context.getDrawable(id)!!.apply {
                setBounds(0, 0, 108, 108)
                draw(Canvas(bitmap))
            }
            var visible = 0
            for (y in 0..107) for (x in 0..107) {
                if (Color.alpha(bitmap.getPixel(x, y)) > 0) {
                    visible++
                    assertTrue(
                        "Foreground leaves circular safe zone at $x,$y",
                        (x - 53.5f) * (x - 53.5f) + (y - 53.5f) * (y - 53.5f) <= 33f * 33f,
                    )
                }
            }
            assertTrue(visible > 1500)
        }
        listOf(24, 48, 96, 192).forEach { size ->
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            icon.setBounds(0, 0, size, size)
            icon.draw(Canvas(bitmap))
            assertTrue(
                "Readable mark at $size px",
                Color.red(bitmap.getPixel(size / 2, size / 2)) > 220,
            )
        }
    }

    @Test
    fun renderColorAndThemedLayersThroughCommonMasks() {
        val size = 180
        val bitmap = Bitmap.createBitmap(840, 740, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(237, 241, 247))
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 20f
                color = Color.DKGRAY
            }
        val labels = listOf("Circle", "Squircle", "Rounded square", "Teardrop")
        val rows = listOf("Original", "Light themed", "Dark themed")
        rows.forEachIndexed { row, title ->
            canvas.drawText(title, 20f, row * 240f + 28, paint)
            labels.forEachIndexed { column, label ->
                canvas.save()
                canvas.translate(column * 210f + 15, row * 240f + 40)
                val path = Path()
                when (column) {
                    0 -> path.addCircle(90f, 90f, 90f, Path.Direction.CW)
                    1 -> {
                        path.moveTo(90f, 0f)
                        path.cubicTo(174f, 0f, 180f, 6f, 180f, 90f)
                        path.cubicTo(180f, 174f, 174f, 180f, 90f, 180f)
                        path.cubicTo(6f, 180f, 0f, 174f, 0f, 90f)
                        path.cubicTo(0f, 6f, 6f, 0f, 90f, 0f)
                        path.close()
                    }
                    2 -> path.addRoundRect(0f, 0f, 180f, 180f, 40f, 40f, Path.Direction.CW)
                    else ->
                        path.addRoundRect(
                            RectF(0f, 0f, 180f, 180f),
                            floatArrayOf(90f, 90f, 90f, 90f, 12f, 12f, 90f, 90f),
                            Path.Direction.CW,
                        )
                }
                canvas.clipPath(path)
                canvas.drawColor(
                    when (row) {
                        0 -> Color.rgb(22, 119, 232)
                        1 -> Color.rgb(211, 228, 255)
                        else -> Color.rgb(20, 40, 66)
                    }
                )
                val foreground =
                    context
                        .getDrawable(
                            if (row == 0) R.drawable.ic_launcher_foreground
                            else R.drawable.ic_launcher_monochrome
                        )!!
                        .mutate()
                if (row != 0)
                    foreground.setTint(
                        if (row == 1) Color.rgb(20, 57, 101) else Color.rgb(174, 203, 250)
                    )
                // AdaptiveIconDrawable expands the 108dp layers around the masked 72dp viewport.
                foreground.setBounds(-size / 4, -size / 4, size * 5 / 4, size * 5 / 4)
                foreground.draw(canvas)
                canvas.restore()
                canvas.drawText(label, column * 210f + 15, row * 240f + 238, paint)
            }
        }
        File(context.getExternalFilesDir(null), "v123-icon-masks.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        assertTrue(File(context.getExternalFilesDir(null), "v123-icon-masks.png").length() > 5000)
    }
}
