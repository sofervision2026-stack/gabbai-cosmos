package com.example.ui.voice

import com.example.ui.viewmodel.GabbaiScreen

data class VoiceCommandResult(
    val screen: GabbaiScreen? = null,
    val search: String? = null,
    val openAdd: Boolean = false,
    val messageKa: String,
    val messageRu: String
)

object VoiceCommandParser {
    fun parse(raw: String): VoiceCommandResult {
        val text = raw.lowercase().trim()
        val screen = when {
            listOf("მთავარ", "главн", "панел", "home", "dashboard", "ראשי", "בית").any(text::contains) -> GabbaiScreen.DASHBOARD
            listOf("ფინანს", "финанс", "касс", "financ", "money", "כספ", "קופה").any(text::contains) -> GabbaiScreen.FINANCES
            listOf("ალია", "თორ", "алия", "тора", "aliya", "aliyah", "torah", "עלי", "תורה").any(text::contains) -> GabbaiScreen.ALIYOT
            listOf("წევრ", "მრევლ", "член", "прихож", "member", "people", "מתפלל", "חבר").any(text::contains) -> GabbaiScreen.MEMBERS
            listOf("ქოლელ", "коллел", "kollel", "כולל").any(text::contains) -> GabbaiScreen.KOLLEL
            listOf("ცედაკ", "благотвор", "цдака", "tzedak", "charity", "צדקה").any(text::contains) -> GabbaiScreen.TZEDAKAH
            listOf("ღონისძი", "событ", "мероприят", "event", "אירוע").any(text::contains) -> GabbaiScreen.EVENTS
            listOf("უსაფრთხ", "безопас", "secur", "אבטח").any(text::contains) -> GabbaiScreen.SECURITY
            listOf("პარამეტრ", "настрой", "setting", "הגדר").any(text::contains) -> GabbaiScreen.SETTINGS
            listOf("დოკუმენტ", "документ", "document", "תיעוד").any(text::contains) -> GabbaiScreen.TECH_DOCS
            else -> null
        }
        val openAdd = listOf("დაამატ", "შექმენ", "добав", "созда", "add", "create", "new", "הוסף", "חדש").any(text::contains)
        val searchMarker = listOf("მოძებნე", "იპოვე", "найди", "поиск", "search", "find", "חפש").firstOrNull(text::contains)
        val search = searchMarker?.let { text.substringAfter(it).trim().takeIf(String::isNotEmpty) }
        return when {
            screen != null -> VoiceCommandResult(screen, search, openAdd, "გავხსენი შესაბამისი განყოფილება", "Открыт нужный раздел")
            search != null -> VoiceCommandResult(search = search, messageKa = "ვიწყებ ძებნას: $search", messageRu = "Ищу: $search")
            else -> VoiceCommandResult(messageKa = "ბრძანება ვერ ამოვიცანი. თქვით განყოფილება ან მოქმედება.", messageRu = "Команда не распознана. Назовите раздел или действие.")
        }
    }
}
