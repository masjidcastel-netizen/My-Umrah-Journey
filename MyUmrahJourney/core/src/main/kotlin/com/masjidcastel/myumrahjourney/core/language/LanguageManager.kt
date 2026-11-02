package com.masjidcastel.myumrahjourney.core.language

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * The four languages supported by My Umrah Journey.
 *
 * [code] is the BCP-47 language tag and also the Android resource qualifier
 * (`values`, `values-fr`, `values-b+mfe`, `values-ar`). Mauritian Creole uses the
 * ISO 639-3 code `mfe`.
 */
enum class AppLanguage(
    val code: String,
    /** Name of the language written in that language. Never translated. */
    val nativeName: String,
    val isRtl: Boolean,
) {
    ENGLISH(code = "en", nativeName = "English", isRtl = false),
    FRENCH(code = "fr", nativeName = "Français", isRtl = false),
    MAURITIAN_CREOLE(code = "mfe", nativeName = "Kreol Morisien", isRtl = false),
    ARABIC(code = "ar", nativeName = "العربية", isRtl = true);

    val locale: Locale
        get() = Locale.forLanguageTag(code)

    companion object {
        val DEFAULT: AppLanguage = ENGLISH

        fun fromCode(code: String?): AppLanguage? =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}

/**
 * Language helpers shared by every module.
 *
 * The app switches language in-process: [localizedContext] returns a context whose resources
 * use the chosen language, and the UI layer provides it (plus the matching layout direction)
 * to Compose. This works the same way on every supported Android version.
 */
object LanguageManager {

    val supportedLanguages: List<AppLanguage> = AppLanguage.entries

    /** First-run language: the device language when supported, English otherwise. */
    fun resolveSystemDefault(locale: Locale = Locale.getDefault()): AppLanguage =
        AppLanguage.fromCode(locale.language) ?: AppLanguage.DEFAULT

    /**
     * Wraps [base] so that `getResources()` (and therefore `getString`, `stringResource`, ...)
     * resolves in [language]. The wrapper still reaches the original Activity through
     * `baseContext`, so `startActivity` and friends keep working.
     */
    fun localizedContext(base: Context, language: AppLanguage): Context {
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(language.locale)
        configuration.setLayoutDirection(language.locale)
        val localizedResources = base.createConfigurationContext(configuration).resources
        return LocalizedContext(base, localizedResources)
    }

    fun formatTimestamp(epochMillis: Long, language: AppLanguage): String =
        DateFormat
            .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, language.locale)
            .format(Date(epochMillis))

    private class LocalizedContext(
        base: Context,
        private val localizedResources: Resources,
    ) : ContextWrapper(base) {
        override fun getResources(): Resources = localizedResources
    }
}
