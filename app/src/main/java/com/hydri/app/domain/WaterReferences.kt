// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0

package com.hydri.app.domain

import com.hydri.app.R
import com.hydri.app.i18n.*

data class WaterReference(
    val age: Int,
    val male: Long,
    val female: Long,
    val maleUpper: Long = male,
    val femaleUpper: Long = female,
)

object WaterReferences {
    // Canonical microlitres. US: IOM/National Academies 2005; Nordic: NNR 2023, adopting EFSA 2010.
    val us =
        listOf(
            WaterReference(R.string.label_1_3_years, 1_300_000, 1_300_000),
            WaterReference(R.string.label_4_8_years, 1_700_000, 1_700_000),
            WaterReference(R.string.label_9_13_years, 2_400_000, 2_100_000),
            WaterReference(R.string.label_14_18_years, 3_300_000, 2_300_000),
            WaterReference(R.string.label_19_years, 3_700_000, 2_700_000),
        )
    val nordic =
        listOf(
            WaterReference(R.string.label_1_2_years, 1_100_000, 1_100_000, 1_200_000, 1_200_000),
            WaterReference(R.string.label_2_3_years, 1_300_000, 1_300_000),
            WaterReference(R.string.label_4_8_years, 1_600_000, 1_600_000),
            WaterReference(R.string.label_9_13_years, 2_100_000, 1_900_000),
            WaterReference(R.string.label_14_years, 2_500_000, 2_000_000),
        )

    fun amount(low: Long, high: Long, unit: String): String =
        if (low == high) Volume.format(low, unit)
        else "${Volume.format(low,unit)}–${Volume.format(high,unit)}"
}
