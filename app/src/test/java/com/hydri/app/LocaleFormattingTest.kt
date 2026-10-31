// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0

package com.hydri.app

import com.hydri.app.domain.Volume
import com.hydri.app.i18n.AppText
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class LocaleFormattingTest {
    @Test
    fun decimalAndGroupingSeparatorsFollowTheLocale() {
        assertEquals(1_500_000L, Volume.parse("1,5", "L", Locale.FRANCE))
        assertEquals(1_234_500L, Volume.parse("1\u202F234,5", "mL", Locale.FRANCE))
        assertEquals(1_250_500L, Volume.parse("1,250.5", "mL", Locale.US))
        assertEquals(1_250_500L, Volume.parse("1.250,5", "mL", Locale.forLanguageTag("id")))
        assertNull(Volume.parse("1,2,3", "mL", Locale.FRANCE))
        assertNull(Volume.parse("1 2", "mL", Locale.FRANCE))
        assertEquals(250_000L, Volume.parse("２５０", "mL", Locale.JAPANESE))
    }

    @Test
    fun editableAmountsRoundTripInAllSevenLanguages() {
        listOf("en", "es", "fr", "zh-Hans", "ko", "ja", "id").forEach { tag ->
            val locale = Locale.forLanguageTag(tag)
            listOf("mL", "L").forEach { unit ->
                assertEquals(
                    "$tag $unit",
                    1_250_000L,
                    Volume.parse(Volume.input(1_250_000, unit, locale), unit, locale),
                )
            }
        }
    }

    @Test
    fun systemFallbackUsesTheFirstSupportedLanguageThenEnglish() {
        assertEquals("fr", AppText.supportedLocale(listOf(Locale.GERMAN, Locale.FRANCE)).language)
        assertEquals("en", AppText.supportedLocale(listOf(Locale.GERMAN)).language)
        assertEquals("en", AppText.supportedLocale(listOf(Locale.TAIWAN, Locale.ENGLISH)).language)
        assertEquals("zh", AppText.supportedLocale(listOf(Locale.CHINA)).language)
    }

    @Test
    fun backupLanguageTagsValidateWithoutDiscardingSupportedRegions() {
        assertTrue(com.hydri.app.i18n.AppLocales.supports("fr-CA,en"))
        assertTrue(com.hydri.app.i18n.AppLocales.supports("zh-Hans-CN"))
        assertFalse(com.hydri.app.i18n.AppLocales.supports("zh-Hant-TW"))
        assertFalse(com.hydri.app.i18n.AppLocales.supports("de-DE"))
        assertFalse(com.hydri.app.i18n.AppLocales.supports("fr_?"))
    }
}
