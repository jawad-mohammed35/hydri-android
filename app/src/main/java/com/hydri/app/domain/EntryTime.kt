// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0

package com.hydri.app.domain

import java.time.*

/** A time edit is confined to the entry's fixed calendar date and original zone. */
object EntryTime {
    fun resolve(date: LocalDate, time: LocalTime, zone: ZoneId, original: Long? = null): Long? {
        val local = date.atTime(time)
        val offsets = zone.rules.getValidOffsets(local)
        if (offsets.isEmpty())
            return null // A DST gap must not silently change the chosen time/day.
        if (
            original != null &&
                Instant.ofEpochMilli(original).atZone(zone).toLocalDateTime() == local
        )
            return original
        return local.toInstant(offsets.first()).toEpochMilli()
    }
}
