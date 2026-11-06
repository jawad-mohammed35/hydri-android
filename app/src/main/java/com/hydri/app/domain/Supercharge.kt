// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0
package com.hydri.app.domain

object Supercharge {
    /** Continuous bounded intensity: zero at the goal, full at twice the goal. */
    fun intensity(total: Long, goal: Long): Float =
        if (goal <= 0 || total <= goal) 0f
        else ((total.toDouble() / goal - 1.0).coerceIn(0.0, 1.0)).toFloat()
}
