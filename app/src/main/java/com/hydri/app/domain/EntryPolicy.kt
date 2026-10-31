// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0

package com.hydri.app.domain

import com.hydri.app.data.WaterEntry
import java.time.LocalDate

object EntryPolicy {
    fun canModify(date: String, today: LocalDate): Boolean = date == today.toString()

    fun recentHistory(entries: List<WaterEntry>, today: LocalDate): Map<String, List<WaterEntry>> {
        val first = today.minusDays(6).toString()
        val last = today.toString()
        return entries
            .filter { it.date in first..last }
            .groupBy { it.date }
            .toSortedMap(reverseOrder())
    }
}
