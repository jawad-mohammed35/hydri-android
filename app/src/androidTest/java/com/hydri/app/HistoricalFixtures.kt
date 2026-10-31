// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0
package com.hydri.app

import com.hydri.app.data.*
import java.time.*
import java.util.UUID

/** Test-only fixture for records created by older releases; never uses interactive logging. */
suspend fun Repository.seedHistory(volume: Long, name: String, date: LocalDate): WaterEntry {
    require(date < LocalDate.now())
    val zone = ZoneId.systemDefault()
    val entry =
        WaterEntry(
            UUID.randomUUID().toString(),
            volume,
            date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli(),
            date.toString(),
            name,
            zone.id,
        )
    if (dao.allGoals().none { it.date <= date.toString() }) {
        dao.putGoal(Goal(date.toString(), Repository.goalFor(dao.allGoals(), date)))
    }
    dao.putEntry(entry)
    return entry
}
