package com.sultonuzdev.netspeed.utils

import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * The language the app runs in, independent of the device language.
 *
 * Two implementations behind one surface, because the platform only grew an API for this in
 * Android 13:
 *
 *  * API 33+ hands the choice to [LocaleManager]. The system stores it, shows it in Settings >
 *    Apps > NetSpeed > Language (which is what res/xml/locales_config.xml is for), and recreates
 *    the activity itself. Nothing to persist on our side.
 *  * Below that there is no platform support, so the tag is kept in SharedPreferences and applied
 *    by wrapping the base context of every component that shows text.
 *
 * SharedPreferences rather than the DataStore the rest of the app uses: [wrap] is called from
 * `attachBaseContext`, before anything can suspend, and blocking a cold start on a coroutine to
 * read one string would be a poor trade.
 *
 * A null tag means "follow the device", which is also the default: Android already resolves the
 * device language against the res/values-* folders without any help from us.
 */
object AppLocale {

    private const val PREFS = "app_locale"
    private const val KEY_TAG = "language_tag"

    /** BCP-47 tag of the chosen language, or null when the app follows the device. */
    fun current(context: Context): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)
                ?.applicationLocales
                ?.takeUnless { it.isEmpty }
                ?.get(0)
                ?.toLanguageTag()
        } else {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_TAG, null)
        }

    /**
     * Switches the app's language. Pass null to go back to following the device.
     *
     * Returns true when the caller has to recreate itself; on API 33+ the system does it.
     */
    fun set(context: Context, tag: String?): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)?.applicationLocales =
                if (tag == null) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
            return false
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .apply { if (tag == null) remove(KEY_TAG) else putString(KEY_TAG, tag) }
            .apply()
        return true
    }

    /**
     * Applies the stored choice to a component's base context.
     *
     * Needed by every component that renders text of its own -- the activity and the monitoring
     * service, whose notification is as user-facing as any screen. A no-op on API 33+, where the
     * system has already resolved the configuration before we see it.
     */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val tag = current(base) ?: return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = base.resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return ContextWrapper(base.createConfigurationContext(config))
    }

    /**
     * The languages offered in the picker, in the order they appear.
     *
     * Endonyms: a language is listed in its own words, because someone looking for Russian is
     * looking for "Русский", not for whatever the current language calls it. This list has to stay
     * in step with the res/values-* folders and res/xml/locales_config.xml.
     */
    val supported: List<Pair<String, String>> = listOf(
        "en" to "English",
        "ar" to "العربية",
        "de" to "Deutsch",
        "es" to "Español",
        "fr" to "Français",
        "hi" to "हिन्दी",
        "id" to "Bahasa Indonesia",
        "ja" to "日本語",
        "ko" to "한국어",
        "pt-BR" to "Português (Brasil)",
        "ru" to "Русский",
        "tr" to "Türkçe",
        "uk" to "Українська",
        "ur" to "اردو",
        "vi" to "Tiếng Việt",
        "zh-CN" to "简体中文",
        "zh-TW" to "繁體中文"
    )

    /** Endonym for the stored tag, or null when following the device. */
    fun displayName(context: Context): String? {
        val tag = current(context) ?: return null
        return supported.firstOrNull { it.first.equals(tag, ignoreCase = true) }?.second
            ?: supported.firstOrNull { tag.startsWith(it.first.substringBefore('-'), true) }?.second
            ?: Locale.forLanguageTag(tag).getDisplayLanguage(Locale.forLanguageTag(tag))
    }
}
