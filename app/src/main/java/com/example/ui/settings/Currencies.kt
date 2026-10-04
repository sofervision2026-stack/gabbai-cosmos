package com.example.ui.settings

import java.util.Locale

data class CurrencyInfo(val code: String, val symbol: String, val nameKa: String, val prefix: Boolean)

object Currencies {
    val all = listOf(
        CurrencyInfo("GEL", "₾", "ქართული ლარი", false),
        CurrencyInfo("USD", "$", "აშშ დოლარი", true),
        CurrencyInfo("EUR", "€", "ევრო", true),
        CurrencyInfo("ILS", "₪", "ისრაელის შეკელი", false),
        CurrencyInfo("RUB", "₽", "რუსული რუბლი", false),
        CurrencyInfo("GBP", "£", "ბრიტანული ფუნტი", true),
        CurrencyInfo("TRY", "₺", "თურქული ლირა", false),
        CurrencyInfo("AMD", "֏", "სომხური დრამი", false),
        CurrencyInfo("AZN", "₼", "აზერბაიჯანული მანათი", false),
        CurrencyInfo("UAH", "₴", "უკრაინული გრივნა", false)
    )
    fun find(code: String?): CurrencyInfo = all.firstOrNull { it.code == code } ?: all.first()
    fun symbol(code: String?): String = find(code).symbol
}

/** Formats an amount with its currency (record currency, or the default from Settings). */
fun money(amount: Double, currency: String? = null): String {
    val c = Currencies.find(currency ?: I18n.defaultCurrency)
    val number = String.format(Locale.US, "%,.0f", kotlin.math.abs(amount))
    val sign = if (amount < 0) "-" else ""
    val text = if (c.prefix) "$sign${c.symbol}$number" else "$sign$number ${c.symbol}"
    // In Hebrew (right-to-left) keep "-300 ₾" in reading order instead of "₾ 300-".
    return if (I18n.lang == AppLanguage.HE) "\u200E$text\u200E" else text
}

/** Signed variant used for income/expense lines: "+1,200 ₾" / "-300 ₾". */
fun signedMoney(amount: Double, positive: Boolean, currency: String? = null): String {
    if (amount == 0.0) return money(0.0, currency)
    val m = money(kotlin.math.abs(amount), currency).trim('\u200E')
    val text = (if (positive) "+" else "-") + m
    return if (I18n.lang == AppLanguage.HE) "\u200E$text\u200E" else text
}
