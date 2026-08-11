package com.example.workman.utils

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Central helper for the app's per-app language (localization) feature.
 *
 * Uses the AndroidX [AppCompatDelegate] per-app locale APIs, which are the modern,
 * Google-recommended way to change the app language independently of the device
 * language. It works on all API levels the app supports (minSdk 24):
 *  - Android 13+ (API 33): handled natively by the platform + system settings.
 *  - Android 12 and below: handled by AppCompat, with automatic persistence via the
 *    `AppLocalesMetadataHolderService` declared in the manifest (autoStoreLocales).
 *
 * Because persistence is delegated to AppCompat/framework, no SharedPreferences code
 * is required and the selection survives process death and app restarts.
 */
object LocaleManager {

    /**
     * The languages the app is translated into. [tag] is the BCP-47 language tag that
     * matches the `values-<tag>` resource folders and the `locales_config.xml` entries.
     * [nativeName] is shown in the picker in the language's own script for clarity.
     */
    enum class AppLanguage(val tag: String, val nativeName: String, val englishName: String) {
        ENGLISH("en", "English", "English"),
        HINDI("hi", "हिन्दी", "Hindi"),
        BENGALI("bn", "বাংলা", "Bengali"),
        TELUGU("te", "తెలుగు", "Telugu"),
        MARATHI("mr", "मराठी", "Marathi"),
        TAMIL("ta", "தமிழ்", "Tamil");

        companion object {
            fun fromTag(tag: String?): AppLanguage {
                if (tag.isNullOrBlank()) return ENGLISH
                val primary = tag.substringBefore('-').lowercase()
                return entries.firstOrNull { it.tag == primary } ?: ENGLISH
            }
        }
    }

    /** All supported languages, in the order they should appear in the picker. */
    val supportedLanguages: List<AppLanguage> = AppLanguage.entries.toList()

    /**
     * The currently applied app language. Falls back to [AppLanguage.ENGLISH] when the
     * user hasn't picked one (i.e. the app follows the system default and no override
     * is stored).
     */
    fun current(): AppLanguage {
        val locales = AppCompatDelegate.getApplicationLocales()
        val tag = if (!locales.isEmpty) locales[0]?.language else null
        return AppLanguage.fromTag(tag)
    }

    /**
     * Applies [language] as the app language immediately. AppCompat recreates the
     * running activities so the whole UI re-renders in the new language, and persists
     * the choice for future launches.
     */
    fun setLanguage(language: AppLanguage) {
        val localeList = LocaleListCompat.forLanguageTags(language.tag)
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}

