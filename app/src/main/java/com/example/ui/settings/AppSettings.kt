package com.example.ui.settings

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import java.util.Locale

enum class AppLanguage(val tag: String, val nativeName: String, val short: String) {
    KA("ka-GE", "ქართული", "KA"),
    RU("ru-RU", "Русский", "RU"),
    EN("en-US", "English", "EN"),
    HE("he-IL", "עברית", "HE")
}
enum class LightingMode { DAY, NIGHT }

data class AppSettings(
    val language: AppLanguage = AppLanguage.KA,
    val lighting: LightingMode = LightingMode.NIGHT,
    val currency: String = "GEL"
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("gabbai_settings", Context.MODE_PRIVATE)
    fun load(): AppSettings {
        val automatic = when (Locale.getDefault().language) {
            "ru" -> "RU"; "en" -> "EN"; "he", "iw" -> "HE"; else -> "KA"
        }
        return AppSettings(
            language = runCatching { AppLanguage.valueOf(prefs.getString("language", automatic) ?: automatic) }.getOrDefault(AppLanguage.KA),
            lighting = runCatching { LightingMode.valueOf(prefs.getString("lighting", "NIGHT") ?: "NIGHT") }.getOrDefault(LightingMode.NIGHT),
            currency = prefs.getString("currency", "GEL")?.takeIf { code -> Currencies.all.any { it.code == code } } ?: "GEL"
        )
    }
    fun save(settings: AppSettings) {
        prefs.edit()
            .putString("language", settings.language.name)
            .putString("lighting", settings.lighting.name)
            .putString("currency", settings.currency)
            .apply()
    }
}

val LocalAppSettings = compositionLocalOf { AppSettings() }

/** Legacy two-language helper; Georgian text is the dictionary key for EN/HE. */
@Composable
fun tr(ka: String, ru: String): String = when (I18n.lang) {
    AppLanguage.KA -> I18n.keepHebrewTogether(ka)
    AppLanguage.RU -> I18n.keepHebrewTogether(ru)
    else -> L(ka)
}
