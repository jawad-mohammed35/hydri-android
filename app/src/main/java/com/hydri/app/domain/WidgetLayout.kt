// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0
package com.hydri.app.domain

/** Pure sizing policy in launcher content dp. Both widget identities keep their own purpose. */
data class WidgetLayout(
    val compact: Boolean,
    val ring: Boolean,
    val verticalRing: Boolean,
    val ringSize: Float,
    val buttonCount: Int,
    val columns: Int,
) {
    companion object {
        fun forSize(width: Float, height: Float, quickAdd: Boolean): WidgetLayout {
            val compact = width < 150 || height < 110
            val columns = if (width >= 250) 2 else 1
            val buttons =
                if (width < 100 || height < 160 || (width < 150 && height < 240)) 0
                else if (quickAdd && height >= 285) columns * 2 else columns
            val ring = width >= 240 && height >= (if (buttons > 0 && quickAdd) 175 else 135)
            val vertical = !ring && width >= 100 && height >= (if (quickAdd) 300 else 200)
            val buttonHeight = ((buttons + columns - 1) / columns) * 56
            val available =
                if (vertical) height - 130 - buttonHeight else height - 95 - buttonHeight
            val diameter =
                available
                    .coerceIn(48f, if (quickAdd) 100f else 150f)
                    .coerceAtMost(width * if (vertical) .72f else .38f)
            return WidgetLayout(compact, ring, vertical, diameter, buttons, columns)
        }
    }
}
