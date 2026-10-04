data class VoiceCommand(
    val screen: String?,
    val search: String?,
    val messageKa: String
)

object VoiceCommandParser {
    fun parse(transcript: String): VoiceCommand {
        val t = transcript.lowercase()
        return when {
            t.contains("წევრები") || t.contains("members") -> 
                VoiceCommand("members", null, "გადავდივარ წევრების რეესტრში")
            t.contains("ფინანსები") || t.contains("finance") -> 
                VoiceCommand("finances", null, "გადავდივარ ფინანსების მართვაზე")
            t.contains("ძებნა") || t.contains("search") -> {
                val query = t.replace("ძებნა", "").replace("search", "").trim()
                VoiceCommand(null, query, "ვეძებ: $query")
            }
            else -> VoiceCommand(null, null, "ბრძანება ვერ დამუშავდა")
        }
    }
}
