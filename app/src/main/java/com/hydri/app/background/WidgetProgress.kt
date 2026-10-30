// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0

package com.hydri.app.background

import android.graphics.*
import android.util.LruCache

/** Small shared bitmaps keep native widget rendering inexpensive across responsive sizes. */
object WidgetProgress {
    private val cache = LruCache<String, Bitmap>(12)

    @Synchronized
    fun ring(fraction: Float, accent: Int, track: Int): Bitmap {
        val value = (fraction.coerceIn(0f, 1f) * 1000).toInt()
        val key = "$value:$accent:$track"
        cache.get(key)?.let {
            return it
        }
        val bitmap = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 15f
                strokeCap = Paint.Cap.ROUND
                color = track
            }
        val bounds = RectF(17f, 17f, 175f, 175f)
        canvas.drawArc(bounds, 0f, 360f, false, paint)
        paint.color = accent
        canvas.drawArc(bounds, -90f, value * .36f, false, paint)
        paint.style = Paint.Style.FILL
        val drop =
            Path().apply {
                moveTo(96f, 57f)
                cubicTo(85f, 77f, 70f, 90f, 70f, 107f)
                cubicTo(70f, 140f, 122f, 140f, 122f, 107f)
                cubicTo(122f, 90f, 107f, 77f, 96f, 57f)
                close()
            }
        canvas.drawPath(drop, paint)
        cache.put(key, bitmap)
        return bitmap
    }
}
