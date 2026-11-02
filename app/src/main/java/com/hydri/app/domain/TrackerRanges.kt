// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0
package com.hydri.app.domain

import java.time.LocalDate
import java.time.YearMonth

object TrackerRanges {
    fun period(saved: String) = if (saved == "Month") "Month" else "Week"

    fun dates(saved: String, month: YearMonth, today: LocalDate): List<LocalDate> =
        if (period(saved) == "Month")
            (1..month.lengthOfMonth()).map(month::atDay).filter { it <= today }
        else (6L downTo 0L).map(today::minusDays)

    fun stepMonth(month: YearMonth, direction: Int, today: LocalDate): YearMonth {
        require(direction == -1 || direction == 1)
        return if (direction > 0 && month >= YearMonth.from(today)) month
        else month.plusMonths(direction.toLong())
    }
}
