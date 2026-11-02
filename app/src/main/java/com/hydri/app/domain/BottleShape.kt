// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0
package com.hydri.app.domain

/** Serialized IDs: never reorder, reuse or renumber existing shapes. */
enum class BottleShape(val id: Int) {
    GLASS(0),
    BOTTLE(1),
    LARGE_BOTTLE(2),
    TUMBLER(3),
    SPORTS_BOTTLE(4),
    MUG(5),
    CUSTOM(6),
    TRAVEL_MUG(7),
    CAN(8),
    MASON_JAR(9),
    TALL_GLASS(10),
    INSULATED_FLASK(11),
    STRAW_TUMBLER(12),
    SQUEEZE_BOTTLE(13);

    companion object {
        fun isValid(id: Int) = entries.any { it.id == id }
    }
}
