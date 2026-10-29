package com.hydri.app.domain

object TabPosition {
    fun indexAt(x: Float, width: Float, rtl: Boolean = false): Int {
        val physical = if (width <= 0f) 0 else ((x / width) * 4).toInt().coerceIn(0, 3)
        return if (rtl) 3 - physical else physical
    }
}
