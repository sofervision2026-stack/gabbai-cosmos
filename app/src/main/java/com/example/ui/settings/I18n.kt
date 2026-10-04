package com.example.ui.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

/**
 * Four-language text lookup. Georgian source text is the key; translations live in
 * assets/i18n.json as {"<ka>": {"ru": "...", "en": "...", "he": "..."}}.
 * `lang` is snapshot state, so every composable that shows text recomposes on change.
 */
object I18n {
    var lang by mutableStateOf(AppLanguage.KA)
    var defaultCurrency by mutableStateOf("GEL")
    private var dict: Map<String, Array<String>> = emptyMap()
    private val hebrewRun = Regex("[\\u0590-\\u05FF][\\u0590-\\u05FF\"'׳״\\-\\s]*[\\u0590-\\u05FF\"'׳״]")

    fun init(context: Context) {
        if (dict.isNotEmpty()) return
        runCatching {
            val text = context.assets.open("i18n.json").use { it.readBytes().toString(Charsets.UTF_8) }
            val json = JSONObject(text)
            val map = HashMap<String, Array<String>>(json.length() * 2)
            json.keys().forEach { key ->
                val o = json.getJSONObject(key)
                map[key] = arrayOf(o.optString("ru"), o.optString("en"), o.optString("he"))
            }
            dict = map
        }
    }

    fun t(ka: String): String {
        val l = lang
        if (l == AppLanguage.KA) return keepHebrewTogether(ka)
        val entry = dict[ka] ?: return keepHebrewTogether(ka)
        val v = when (l) { AppLanguage.RU -> entry[0]; AppLanguage.EN -> entry[1]; else -> entry[2] }
        return keepHebrewTogether(v.ifBlank { ka })
    }

    /** Hebrew phrases never break in the middle: spaces inside a Hebrew run become non-breaking. */
    fun keepHebrewTogether(s: String): String {
        if (s.none { it in '\u0590'..'\u05FF' }) return s
        return hebrewRun.replace(s) { m -> m.value.replace(' ', '\u00A0') }
    }
}

/** Translate a Georgian source string into the current language. */
fun L(ka: String): String = I18n.t(ka)

/** Translate a template with {0}, {1}… placeholders, then fill them. */
fun Lf(ka: String, vararg args: Any?): String {
    var s = I18n.t(ka)
    args.forEachIndexed { i, a -> s = s.replace("{$i}", a?.toString() ?: "") }
    return s
}

/** Make arbitrary user/data text safe for line breaking (keeps Hebrew phrases whole). */
fun nb(s: String): String = I18n.keepHebrewTogether(s)
