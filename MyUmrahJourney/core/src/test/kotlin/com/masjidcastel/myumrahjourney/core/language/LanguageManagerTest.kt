package com.masjidcastel.myumrahjourney.core.language

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageManagerTest {

    @Test
    fun exactlyFourLanguagesAreSupported() {
        assertEquals(
            listOf("en", "fr", "mfe", "ar"),
            LanguageManager.supportedLanguages.map { it.code },
        )
    }

    @Test
    fun onlyArabicIsRightToLeft() {
        assertEquals(listOf(AppLanguage.ARABIC), AppLanguage.entries.filter { it.isRtl })
        assertTrue(AppLanguage.ARABIC.isRtl)
        assertFalse(AppLanguage.MAURITIAN_CREOLE.isRtl)
    }

    @Test
    fun fromCodeIsCaseInsensitiveAndRejectsUnknownCodes() {
        assertEquals(AppLanguage.FRENCH, AppLanguage.fromCode("FR"))
        assertEquals(AppLanguage.MAURITIAN_CREOLE, AppLanguage.fromCode("mfe"))
        assertNull(AppLanguage.fromCode("de"))
        assertNull(AppLanguage.fromCode(null))
    }

    @Test
    fun systemLanguageMapsToSupportedLanguageOrEnglish() {
        assertEquals(AppLanguage.FRENCH, LanguageManager.resolveSystemDefault(Locale("fr", "FR")))
        assertEquals(AppLanguage.ARABIC, LanguageManager.resolveSystemDefault(Locale("ar", "SA")))
        assertEquals(
            AppLanguage.MAURITIAN_CREOLE,
            LanguageManager.resolveSystemDefault(Locale.forLanguageTag("mfe-MU")),
        )
        assertEquals(AppLanguage.ENGLISH, LanguageManager.resolveSystemDefault(Locale("de", "DE")))
    }

    @Test
    fun localesUseTheExpectedLanguageSubtag() {
        assertEquals("mfe", AppLanguage.MAURITIAN_CREOLE.locale.language)
        assertEquals("ar", AppLanguage.ARABIC.locale.language)
    }
}
