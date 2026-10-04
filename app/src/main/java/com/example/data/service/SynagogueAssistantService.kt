package com.example.data.service

import com.example.ui.settings.L
import com.example.ui.settings.Lf

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.dao.GabbaiDao
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Result data class for Synagogue AI Assistant queries.
 */
data class AssistantResult(
    val answer: String,
    val matchedMembers: List<MemberEntity> = emptyList(),
    val matchedTransactions: List<TransactionEntity> = emptyList(),
    val matchedNeedy: List<NeedyBeneficiaryEntity> = emptyList(),
    val matchedDocsSection: String? = null,
    val provider: String = "Gemini AI"
)

/**
 * Intelligent Assistant Service for searching synagogue records and answering queries
 * about technical docs, halachic features, and congregational records.
 *
 * Employs Gemini 3.5 Flash with grounding from Room database & technical whitepaper.
 */
class SynagogueAssistantService(
    private val context: Context,
    private val dao: GabbaiDao
) {
    private val TAG = "SynagogueAssistant"
    private val AI_ENDPOINTS = listOf(
        "https://project--48227591-e388-4c7f-9552-01c721986f51.lovable.app/api/public/gabbai-ask",
        "https://project--48227591-e388-4c7f-9552-01c721986f51-dev.lovable.app/api/public/gabbai-ask"
    )

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    /**
     * Answers a natural language query using Gemini AI grounded with real synagogue records
     * and technical whitepaper documentation.
     */
    suspend fun queryAssistant(userPrompt: String): AssistantResult = withContext(Dispatchers.IO) {
        val trimmedPrompt = userPrompt.trim()
        if (trimmedPrompt.isEmpty()) {
            return@withContext AssistantResult(L("გთხოვთ შეიყვანოთ შეკითხვა სინაგოგის ჩანაწერების ან ტექნიკური დოკუმენტაციის შესახებ."))
        }

        // 1. Gather live contextual grounding from Database
        val members = try { dao.getAllMembers().first() } catch (e: Exception) { emptyList() }
        val transactions = try { dao.getAllTransactions().first() } catch (e: Exception) { emptyList() }
        val aliyot = try { dao.getAllAliyot().first() } catch (e: Exception) { emptyList() }
        val needy = try { dao.getAllNeedyBeneficiaries().first() } catch (e: Exception) { emptyList() }
        val tzedakah = try { dao.getAllTzedakah().first() } catch (e: Exception) { emptyList() }
        val kollel = try { dao.getAllKollelStudents().first() } catch (e: Exception) { emptyList() }

        // Local pattern matching for structured entities
        val matchedMembers = members.filter { m ->
            trimmedPrompt.contains(m.fullName, ignoreCase = true) ||
                    (m.hebrewName.isNotEmpty() && trimmedPrompt.contains(m.hebrewName, ignoreCase = true)) ||
                    (trimmedPrompt.contains(L("ქოჰენ"), ignoreCase = true) && m.tribalStatus.contains("כהן")) ||
                    (trimmedPrompt.contains(L("ლევ"), ignoreCase = true) && m.tribalStatus.contains("לוי")) ||
                    (trimmedPrompt.contains(L("წითელ"), ignoreCase = true) && m.complianceCategory == MemberCompliance.RED_LIST) ||
                    (trimmedPrompt.contains(L("შავ"), ignoreCase = true) && m.complianceCategory == MemberCompliance.BLACK_LIST) ||
                    (trimmedPrompt.contains(L("ვალ"), ignoreCase = true) && (m.outstandingDebt > 0 || !m.duesPaid)) ||
                    (trimmedPrompt.contains(L("ისრაელ"), ignoreCase = true) && m.status == MemberStatus.ALIYAH_ISRAEL)
        }.take(8)

        val matchedNeedy = needy.filter { n ->
            trimmedPrompt.contains(n.identifierCode, ignoreCase = true) ||
                    trimmedPrompt.contains(n.fullNameOrPseudonym, ignoreCase = true) ||
                    (trimmedPrompt.contains(L("ობოლ"), ignoreCase = true) && n.fullNameOrPseudonym.contains(L("ობოლ"))) ||
                    (trimmedPrompt.contains(L("სამედიცინო"), ignoreCase = true) && n.category == TzedakahCategory.MEDICAL_URGENT) ||
                    (trimmedPrompt.contains(L("პატარძალ"), ignoreCase = true) && n.category == TzedakahCategory.HACHNASAT_KALLAH)
        }.take(5)

        // 2. Build Grounding Context
        val databaseSummary = buildDatabaseSummary(members, transactions, aliyot, needy, tzedakah, kollel)
        val technicalDocsSummary = buildTechnicalDocsSummary()

        val systemInstruction = """
            თქვენ ხართ Gabbai Cosmos-ის ინტელექტუალური ასისტენტი (სინაგოგის ჭკვიანი მრჩეველი).
            თქვენი მოვალეობაა უპასუხოთ გაბაის, რაბინატს ან ადმინისტრატორს სინაგოგის რეალურ მონაცემებზე,
            ჯამაათის წევრებზე, ალიებზე, ფინანსებზე, ცედაკაზე, გაჭირვებულთა ბაზაზე და ტექნიკურ დოკუმენტაციაზე დაყრდნობით.
            
            წესები:
            1. უპასუხეთ ზუსტად, თავაზიანად და კომპეტენტურად ქართულ ენაზე (საჭიროებისას ებრაული ტერმინების მითითებით).
            2. დაეყრდენით ქვემოთ მოცემულ რეალურ მონაცემებსა და ტექნიკურ დოკუმენტაციას.
            3. თუ შეკითხვა ეხება წევრებს, ვალებს ან წითელ/შავ სიას, დაუსახელეთ კონკრეტული წევრები და თანხები.
            4. თუ შეკითხვა ეხება უსაფრთხოებას, დაშიფვრას (CosmicVault AES-256) ან სისტემის არქიტექტურას, ახსენით ტექნიკურად მკაფიოდ.
        """.trimIndent()

        val fullPromptWithContext = """
            [სინაგოგის მონაცემთა ბაზის მიმდინარე სტატუსი]:
            $databaseSummary
            
            [ტექნიკური დოკუმენტაცია & სისტემური არქიტექტურა]:
            $technicalDocsSummary
            
            [მომხმარებლის შეკითხვა]:
            $trimmedPrompt
        """.trimIndent()

        // 3. Ask the AI service with the full finance / members / tzedakah records
        val lang = if (trimmedPrompt.any { it in '\u0400'..'\u04FF' }) "ru" else "ka"
        val records = buildDetailedRecords(members, transactions, needy, tzedakah)
        for (endpoint in AI_ENDPOINTS) {
            try {
                val answer = callAiService(endpoint, trimmedPrompt, records, lang)
                if (answer.isNotEmpty()) {
                    return@withContext AssistantResult(
                        answer = answer,
                        matchedMembers = matchedMembers,
                        matchedNeedy = matchedNeedy,
                        provider = "Gabbai AI"
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "AI service unavailable at $endpoint: ${e.message}")
            }
        }

        // 4. Intelligent On-Device Semantic Reasoning Fallback
        val localResponse = generateLocalSemanticAnswer(trimmedPrompt, members, transactions, aliyot, needy, tzedakah, kollel)
        AssistantResult(
            answer = localResponse,
            matchedMembers = matchedMembers,
            matchedNeedy = matchedNeedy,
            provider = "Gabbai Local Intelligence Engine"
        )
    }


    private fun callAiService(endpoint: String, question: String, records: String, lang: String): String {
        val body = JSONObject().apply {
            put("question", question)
            put("context", records.take(58000))
            put("language", lang)
        }.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(endpoint).post(body).header("x-gabbai-client", "gabbai-cosmos-android").build()
        okHttpClient.newCall(request).execute().use { response ->
            val text = response.body?.string() ?: ""
            if (!response.isSuccessful) { Log.e(TAG, "AI ${response.code}: $text"); return "" }
            return JSONObject(text).optString("answer", "")
        }
    }

    /** Readable record dump for the AI. Encrypted notes are never included. */
    private fun buildDetailedRecords(
        members: List<MemberEntity>,
        transactions: List<TransactionEntity>,
        needy: List<NeedyBeneficiaryEntity>,
        tzedakah: List<TzedakahEntity>
    ): String = buildString {
        val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        appendLine("TOTALS: income=$income expense=$expense balance=${income - expense} members=${members.size} memberDebt=${members.sumOf { it.outstandingDebt }} tzedakahGiven=${tzedakah.sumOf { it.amount }}")
        appendLine("\nMEMBERS (name | status | role | duesPaid | annualFee | outstandingDebt | lastPayment | list):")
        members.forEach { m ->
            appendLine("${m.fullName} | ${m.status.titleKa} | ${m.role.titleKa} | ${m.duesPaid} | ${m.annualFeeAmount} | ${m.outstandingDebt} | ${m.lastPaymentAmount} ${if (m.lastPaymentDateMillis > 0) dateFormatter.format(Date(m.lastPaymentDateMillis)) else ""} | ${m.complianceCategory.titleKa}")
        }
        appendLine("\nFINANCE TRANSACTIONS (date | type | category | amount | title | member | status):")
        transactions.take(400).forEach { t ->
            appendLine("${dateFormatter.format(Date(t.dateMillis))} | ${t.type.name} | ${t.category.name} | ${t.amount} | ${t.title} | ${t.memberName} | ${t.status.name}")
        }
        appendLine("\nTZEDAKAH DISBURSEMENTS (date | code | category | amount | emergency | approvedBy):")
        tzedakah.take(300).forEach { z ->
            appendLine("${dateFormatter.format(Date(z.dateMillis))} | ${z.recipientPrivacyCode} | ${z.category.titleKa} | ${z.amount} | ${z.isEmergency} | ${z.approvedByRabbi}")
        }
        appendLine("\nNEEDY BENEFICIARIES (code | name | category | family | monthlyNeed | monthlyAid | status):")
        needy.forEach { n ->
            appendLine("${n.identifierCode} | ${n.fullNameOrPseudonym} | ${n.category.titleKa} | ${n.familyMembersCount} | ${n.monthlyEstimatedNeed} | ${n.monthlyApprovedAid} | ${n.status.titleKa}")
        }
    }

    private fun getApiKey(): String {
        return try {
            val keyField = BuildConfig::class.java.getField("GEMINI_API_KEY")
            keyField.get(null) as? String ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Calls Gemini 3.5 Flash REST API endpoint directly with OkHttp.
     */
    private fun callGeminiRestApi(apiKey: String, systemInstruction: String, userText: String): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val rootJson = JSONObject().apply {
            // System instruction
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemInstruction) })
                })
            })
            // Contents
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", userText) })
                    })
                })
            })
            // Generation config
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.3)
                put("topP", 0.9)
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = rootJson.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        val response = okHttpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (!response.isSuccessful) {
            Log.e(TAG, "API call failed (${response.code}): $responseBody")
            return ""
        }

        val jsonResponse = JSONObject(responseBody)
        val candidates = jsonResponse.optJSONArray("candidates")
        val firstCandidate = candidates?.optJSONObject(0)
        val content = firstCandidate?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val text = parts?.optJSONObject(0)?.optString("text")

        return text ?: ""
    }

    /**
     * High-precision on-device semantic rule engine answering database and docs queries
     * when offline or when API key is pending configuration.
     */
    private fun generateLocalSemanticAnswer(
        query: String,
        members: List<MemberEntity>,
        transactions: List<TransactionEntity>,
        aliyot: List<AliyahEntity>,
        needy: List<NeedyBeneficiaryEntity>,
        tzedakah: List<TzedakahEntity>,
        kollel: List<KollelStudentEntity>
    ): String {
        val q = query.lowercase(Locale.getDefault())

        return when {
            // 1. Debtors, Red & Black List queries
            q.has("ვალი") || q.has("მოვალე") || q.has("წითელ") || q.has("შავ") -> {
                val redList = members.filter { it.complianceCategory == MemberCompliance.RED_LIST || (it.outstandingDebt >= 300 && !it.duesPaid) }
                val blackList = members.filter { it.complianceCategory == MemberCompliance.BLACK_LIST }
                val totalDebt = members.sumOf { it.outstandingDebt }

                buildString {
                    append(L("📊 **ჯამაათის ვალებისა და მონიტორინგის ანგარიში:**\n\n"))
                    append(Lf("• სულ ნედერებისა და საწევროს ვალი: **{0}**\n", com.example.ui.settings.money(totalDebt)))
                    append(Lf("• 🚨 **წითელი სია (ვადაგადაცილებული):** {0} წევრი\n", redList.size))
                    redList.forEach {
                        append(Lf("  - {0}: ვალი {1} ({2})\n", it.fullName, com.example.ui.settings.money(it.outstandingDebt), it.blackListReason.ifEmpty { L("ვადაგადაცილებული") }))
                    }
                    append(Lf("\n• ⛔ **შავი სია (სანქცირებული / უარი):** {0} წევრი\n", blackList.size))
                    blackList.forEach {
                        append(Lf("  - {0}: მიზეზი: {1}\n", it.fullName, it.blackListReason.ifEmpty { L("სანქცირებული გაბაის მიერ") }))
                    }
                    append(L("\n💡 *რჩევა: შეგიძლიათ გაუგზავნოთ შეხსენება WhatsApp-ით ან SMS-ით წევრის ბარათიდან.*"))
                }
            }

            // 2. Members, Cohanim, Leviim queries
            q.has("ქოჰენ") || q.has("ლევ") || q.has("წევრ") || q.has("ხალხი") || q.has("სკამ") -> {
                val cohanim = members.filter { it.tribalStatus.contains("כהן") || it.tribalStatus.contains(L("ქოჰენი")) }
                val leviim = members.filter { it.tribalStatus.contains("לוי") || it.tribalStatus.contains(L("ლევი")) }
                val israel = members.filter { it.status == MemberStatus.ALIYAH_ISRAEL }

                buildString {
                    append(L("👥 **ჯამაათის წევრთა რეესტრის მონაცემები:**\n\n"))
                    append(Lf("• სულ რეგისტრირებულია: **{0} წევრი**\n", members.size))
                    append(Lf("• כהנים (ქოჰენები): {0} წევრი ({1})\n", cohanim.size, cohanim.joinToString { Lf("{0} [სკამი: {1}]", it.fullName, it.seatNumber.ifEmpty { "-" }) }))
                    append(Lf("• לויים (ლევიები): {0} წევრი ({1})\n", leviim.size, leviim.joinToString { Lf("{0} [სკამი: {1}]", it.fullName, it.seatNumber.ifEmpty { "-" }) }))
                    append(Lf("• 🇮🇱 ავიდა ისრაელში (ალია): {0} წევრი ({1})\n", israel.size, israel.joinToString { it.fullName }))
                    append(Lf("• გარდაცვლილი (საგვარეულო იარცეიტები): {0} წევრი\n", members.count { it.status == MemberStatus.DECEASED }))
                }
            }

            // 3. Tzedakah and Needy families
            q.has("ცედაკ") || q.has("ღარიბ") || q.has("გაჭირვებულ") || q.has("ნეზაკაკ") || q.has("ქველმოქმედ") -> {
                val totalTzedakah = tzedakah.sumOf { it.amount }
                val activeNeedy = needy.filter { it.status == BeneficiaryStatus.ACTIVE }
                val monthlyApproved = activeNeedy.sumOf { it.monthlyApprovedAid }

                buildString {
                    append(L("🤝 **კუპათ ცედაკა & გაჭირვებულთა ბაზა (מתן בסתר):**\n\n"))
                    append(Lf("• სულ გაცემულია ცედაკა: **{0}**\n", com.example.ui.settings.money(totalTzedakah)))
                    append(Lf("• აღრიცხული ბენეფიციარი ოჯახები: **{0} ოჯახი**\n", needy.size))
                    append(Lf("• დამტკიცებული ყოველთვიური შემწეობა: **{0}/თვე**\n", com.example.ui.settings.money(monthlyApproved)))
                    append(L("• კატეგორიები: პატარძლის შემწეობა, სასწრაფო სამედიცინო, სასურსათო კალათები, ობლები/ქვრივები.\n"))
                    append(L("• ჰალახური პრინციპი: „מתן בסתר יכפה אף“ — ყველა პირადი დეტალი დაშიფრულია AES-256 ალგორითმით.\n"))
                }
            }

            // 4. Financial Ledger & Cash balance
            q.has("ფინანს") || q.has("სალარო") || q.has("ბალანს") || q.has("შემოსავალ") || q.has("გასავალ") -> {
                val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                val balance = income - expense

                buildString {
                    append(L("💰 **სინაგოგის სალარო & ფინანსური ბალანსი:**\n\n"))
                    append(Lf("• სულ შემოსავალი: **+{0}**\n", com.example.ui.settings.money(income)))
                    append(Lf("• სულ გასავალი: **-{0}**\n", com.example.ui.settings.money(expense)))
                    append(Lf("• წმინდა ნაშთი (ბალანსი): **{0}**\n", com.example.ui.settings.money(balance)))
                    append(L("• მთლიანობა: ყველა ტრანზაქცია დაკავშირებულია SHA-256 კრიპტოგრაფიული ჰეშ-ჯაჭვით (ბლოკჩეინ ლეჯერი).\n"))
                }
            }

            // 5. Technical Docs & Cryptography queries
            q.has("ტექნიკურ") || q.has("არქიტექტურ") || q.has("დაშიფვრ") || q.contains("cosmicvault") || q.has("უსაფრთხოებ") || q.has("ჰეშ") -> {
                buildString {
                    append(L("🛡️ **ტექნიკური დოკუმენტაცია & CosmicVault უსაფრთხოება:**\n\n"))
                    append(L("• **არქიტექტურა:** Clean Architecture / MVVM Jetpack Compose & Room SQLite Database.\n"))
                    append(L("• **CosmicVault Cryptography:** AES-256-GCM სიმეტრიული დაშიფვრა PBKDF2-SHA256 გასაღების წარმოებით. საიდუმლო ჩანაწერები და ცედაკის მონაცემები ბაზაში ინახება შიფრატექსტად.\n"))
                    append(L("• **Blockchain Hash Ledger:** თითოეული ფინანსური ტრანზაქცია შეიცავს `prevHash` და `hash` (SHA-256), რაც გამორიცხავს წარსული ჩანაწერების უკანონო მოდიფიკაციას.\n"))
                    append(L("• **Sheets ექსპორტი:** UTF-8 BOM კოდირებული CSV გენერაცია, რომელიც იდეალურად იხსნება Microsoft Excel-სა და Google Sheets-ში ქართული/ებრაული შრიფტების დაუმახინჯებლად.\n"))
                }
            }

            // Default General Overview
            else -> {
                buildString {
                    append(L("✡️ **Gabbai Cosmos ასისტენტი თქვენს სამსახურშია:**\n\n"))
                    append(L("მე შემიძლია გაგიწიოთ კონსულტაცია და მოგაწოდოთ ზუსტი ინფორმაცია:\n"))
                    append(L("1. **ჯამაათის წევრებზე:** ვალები, ქოჰენები/ლევიები, სტატუსები, წითელი/შავი სიები.\n"))
                    append(L("2. **ცედაკის ფონდზე:** გაჭირვებულთა ბაზა (ნეზაკაკიმ), გაცემული დახმარებები.\n"))
                    append(L("3. **სალაროსა და ალიებზე:** შემოსავლები, გასავლები, შაბათის გაყიდვები.\n"))
                    append(L("4. **ტექნიკურ დოკუმენტაციაზე:** CosmicVault AES-256, SHA-256 ლეჯერი, ექსპორტი.\n\n"))
                    append(L("💡 *სცადეთ: „ვის აქვს ვადაგადაცილებული ვალი?“, „რა თანხაა ცედაკის სალაროში?“, ან „როგორ მუშაობს CosmicVault?“*"))
                }
            }
        }
    }

    private fun buildDatabaseSummary(
        members: List<MemberEntity>,
        transactions: List<TransactionEntity>,
        aliyot: List<AliyahEntity>,
        needy: List<NeedyBeneficiaryEntity>,
        tzedakah: List<TzedakahEntity>,
        kollel: List<KollelStudentEntity>
    ): String {
        val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val redListCount = members.count { it.complianceCategory == MemberCompliance.RED_LIST || (it.outstandingDebt >= 300 && !it.duesPaid) }
        val blackListCount = members.count { it.complianceCategory == MemberCompliance.BLACK_LIST }

        return """
            - ჯამაათის წევრები: ${members.size} სული (აქტიური: ${members.count { it.status == MemberStatus.ACTIVE }}, ისრაელში: ${members.count { it.status == MemberStatus.ALIYAH_ISRAEL }})
            - წითელი სია (ვადაგადაცილება): $redListCount წევრი, შავი სია: $blackListCount წევრი
            - წევრთა სრული ვალი: ${members.sumOf { it.outstandingDebt }}
            - ფინანსური სალარო: შემოსავალი +${totalIncome}, გასავალი -${com.example.ui.settings.money((totalExpense).toDouble())}, ნაშთი ${com.example.ui.settings.money((totalIncome - totalExpense).toDouble())}
            - გაჭირვებულთა ბაზა: ${needy.size} ოჯახი, თვიური დამტკიცებული შემწეობა ${needy.filter { it.status == BeneficiaryStatus.ACTIVE }.sumOf { it.monthlyApprovedAid }}
            - გაცემული ცედაკა: ${tzedakah.sumOf { it.amount }} (${tzedakah.size} გაცემა)
            - ქოლელის აბრეხები: ${kollel.size} სტუდენტი
        """.trimIndent()
    }

    private fun buildTechnicalDocsSummary(): String {
        return """
            - სისტემა: Gabbai Cosmos v4.0 (სინაგოგის მართვის კოსმიური პლატფორმა)
            - არქიტექტურა: MVVM, Jetpack Compose, Room SQLite, StateFlow
            - უსაფრთხოება (CosmicVault): AES-256-GCM სიმეტრიული შიფრაცია PBKDF2-SHA256 derivation key-ით
            - სალაროს მთლიანობა: SHA-256 ჰეშ-ჯაჭვი (Blockchain-style Immutable Ledger)
            - ექსპორტი: ცხრილები (Sheets) UTF-8 BOM კოდირებით (Microsoft Excel & Google Sheets თავსებადი)
            - ჰალახური პრინციპები: מתן בסתר (ცედაკის სრული კონფიდენციალობა), ქოჰენების/ლევიების პრიორიტეტი თორაზე.
        """.trimIndent()
    }
}

/** Matches a Georgian keyword stem or its translation in the current app language. */
private fun String.has(ka: String): Boolean = contains(ka) || com.example.ui.settings.L(ka).lowercase().let { it.isNotBlank() && contains(it) }
