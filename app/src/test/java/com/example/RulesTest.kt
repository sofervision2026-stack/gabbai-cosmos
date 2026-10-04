package com.example

import com.example.data.model.AliyahType
import com.example.ui.settings.AppLanguage
import com.example.ui.settings.I18n
import com.example.ui.settings.Lf
import com.example.ui.settings.money
import com.example.ui.settings.signedMoney
import com.example.ui.voice.VoiceCommandParser
import com.example.ui.viewmodel.GabbaiScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RulesTest {
    @Before fun reset() { I18n.lang = AppLanguage.KA; I18n.defaultCurrency = "GEL" }

    @Test fun `lari is written after the amount`() = assertEquals("1,200 ₾", money(1200.0, "GEL"))

    @Test fun `dollar is written before the amount`() = assertEquals("$1,200", money(1200.0, "USD"))

    @Test fun `amount without currency uses default currency from settings`() {
        I18n.defaultCurrency = "ILS"
        assertEquals("500 ₪", money(500.0))
    }

    @Test fun `expense line is signed minus`() = assertEquals("-300 ₾", signedMoney(300.0, false, "GEL"))

    @Test fun `zero expense has no minus sign`() = assertEquals("0 ₾", signedMoney(0.0, false, "GEL"))

    @Test fun `screen title follows language switch`() {
        I18n.lang = AppLanguage.EN
        GabbaiScreen.FINANCES.title
        I18n.lang = AppLanguage.KA
        assertEquals("ფინანსები", GabbaiScreen.FINANCES.title)
    }

    @Test fun `hebrew phrase never breaks inside`() {
        val s = I18n.keepHebrewTogether("ფეთიხათ ჰაარონი פתיחת הארון")
        assertTrue(s.contains("פתיחת\u00A0הארון"))
        assertTrue(s.startsWith("ფეთიხათ ჰაარონი "))
    }

    @Test fun `template placeholders are filled`() = assertEquals("ყველა (54)", Lf("ყველა ({0})", 54))

    @Test fun `aliyah honors include service and mosif`() {
        val names = AliyahType.values().map { it.name }
        assertTrue("SERVICE" in names)
        assertTrue("MOSIF" in names)
    }

    @Test fun `terminology uses kohen and petichat spelling`() {
        val all = AliyahType.values().joinToString { it.titleKa }
        assertFalse(all.contains("კოენ"))
        assertFalse(all.contains("ფეტიხათ"))
        assertFalse(all.contains("პეტიხა"))
    }

    @Test fun `voice opens finances in english and hebrew`() {
        assertEquals(GabbaiScreen.FINANCES, VoiceCommandParser.parse("open finances").screen)
        assertEquals(GabbaiScreen.FINANCES, VoiceCommandParser.parse("פתח כספים").screen)
    }
}
