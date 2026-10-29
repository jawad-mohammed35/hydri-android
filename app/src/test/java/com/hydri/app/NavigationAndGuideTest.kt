package com.hydri.app

import com.hydri.app.domain.*
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class NavigationAndGuideTest {
    @Test
    fun dragCrossesAllFourTabsAndClampsOutsideBounds() {
        assertEquals(
            listOf(0, 1, 2, 3),
            listOf(40f, 120f, 200f, 280f).map { TabPosition.indexAt(it, 320f) },
        )
        assertEquals(0, TabPosition.indexAt(-50f, 320f))
        assertEquals(3, TabPosition.indexAt(400f, 320f))
        assertEquals(0, TabPosition.indexAt(4f, 0f))
    }

    @Test
    fun dragMirrorsWithRightToLeftLayout() {
        assertEquals(
            listOf(3, 2, 1, 0),
            listOf(40f, 120f, 200f, 280f).map { TabPosition.indexAt(it, 320f, true) },
        )
    }

    @Test
    fun guideKeepsAuthorityValuesDistinct() {
        assertEquals(3_700_000L, WaterReferences.us.last().male)
        assertEquals(2_700_000L, WaterReferences.us.last().female)
        assertEquals(2_500_000L, WaterReferences.nordic.last().male)
        assertEquals(2_000_000L, WaterReferences.nordic.last().female)
        assertEquals(1_200_000L, WaterReferences.nordic.first().maleUpper)
    }

    @Test
    fun guideFormatsCanonicalVolumesInAllSupportedUnits() {
        val original = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            assertEquals("3,700 mL", WaterReferences.amount(3_700_000, 3_700_000, "mL"))
            assertEquals("3.70 L", WaterReferences.amount(3_700_000, 3_700_000, "L"))
            assertEquals("125.1 oz", WaterReferences.amount(3_700_000, 3_700_000, "oz"))
            assertEquals("1.10 L–1.20 L", WaterReferences.amount(1_100_000, 1_200_000, "L"))
        } finally {
            Locale.setDefault(original)
        }
    }
}
