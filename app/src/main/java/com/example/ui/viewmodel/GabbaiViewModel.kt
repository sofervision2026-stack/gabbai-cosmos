package com.example.ui.viewmodel

import com.example.ui.settings.L
import com.example.ui.settings.Lf

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.data.repository.GabbaiRepository
import com.example.data.security.CosmicVault
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class GabbaiScreen(private val ka: String, val titleRu: String, val titleHe: String) {
    DASHBOARD("მთავარი", "Главная", "לוח בקרה"),
    FINANCES("ფინანსები", "Финансы", "כספים וחשבונות"),
    ALIYOT("ალიები და ფასუკები", "Алиёт и псукей", "עליות לספר תורה"),
    MEMBERS("პერსონალი და წევრები", "Персонал и прихожане", "מתפללים וצוות"),
    KOLLEL("ქოლელი", "Коллель", "כולל אברכים"),
    TZEDAKAH("ცედაკა", "Цдака", "קופת צדקה"),
    EVENTS("ღონისძიებები", "События", "אירועים ושמחות"),
    SECURITY("უსაფრთხოება", "Безопасность", "אבטחה והצפנה"),
    TECH_DOCS("დოკუმენტაცია", "Документация", "תיעוד טכני"),
    SETTINGS("პარამეტრები", "Настройки", "הגדרות");
    /** Title in the current app language. */
    val title: String get() = com.example.ui.settings.L(ka)
    val titleKa: String get() = title
}

data class GabbaiUiState(
    val currentScreen: GabbaiScreen = GabbaiScreen.DASHBOARD,
    val transactions: List<TransactionEntity> = emptyList(),
    val members: List<MemberEntity> = emptyList(),
    val aliyot: List<AliyahEntity> = emptyList(),
    val kollelStudents: List<KollelStudentEntity> = emptyList(),
    val kollelAttendance: List<KollelAttendanceEntity> = emptyList(),
    val tzedakahFunds: List<TzedakahEntity> = emptyList(),
    val needyBeneficiaries: List<NeedyBeneficiaryEntity> = emptyList(),
    val events: List<SynagogueEventEntity> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netBalance: Double = 0.0,
    val totalOutstandingPledges: Double = 0.0,
    val isVaultUnlocked: Boolean = true,
    val vaultPin: String = "7700",
    val integrityVerified: Boolean = true,
    val searchQuery: String = "",
    val activeFinanceFilter: TransactionType? = null,
    val statusMessage: String? = null
)

class GabbaiViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GabbaiRepository
    val assistantService: com.example.data.service.SynagogueAssistantService

    private val _uiState = MutableStateFlow(GabbaiUiState())
    val uiState: StateFlow<GabbaiUiState> = _uiState.asStateFlow()

    private val _assistantResult = MutableStateFlow<com.example.data.service.AssistantResult?>(null)
    val assistantResult: StateFlow<com.example.data.service.AssistantResult?> = _assistantResult.asStateFlow()

    private val _isAssistantLoading = MutableStateFlow(false)
    val isAssistantLoading: StateFlow<Boolean> = _isAssistantLoading.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = GabbaiRepository(db.gabbaiDao())
        assistantService = com.example.data.service.SynagogueAssistantService(application, db.gabbaiDao())

        viewModelScope.launch {
            if (!com.example.data.backup.BackupManager.isSeeded(application)) {
                repository.checkAndSeedInitialData()
                com.example.data.backup.BackupManager.markSeeded(application)
            }
        }

        observeData()
    }

    fun askAssistant(prompt: String) {
        viewModelScope.launch {
            _isAssistantLoading.value = true
            try {
                val result = assistantService.queryAssistant(prompt)
                _assistantResult.value = result
            } catch (e: Exception) {
                _assistantResult.value = com.example.data.service.AssistantResult(Lf("შეცდომა ასისტენტის გამოძახებისას: {0}", e.message))
            } finally {
                _isAssistantLoading.value = false
            }
        }
    }

    fun clearAssistantResult() {
        _assistantResult.value = null
    }

    private fun recalculateMetrics() {
        val txs = _uiState.value.transactions
        val aliyot = _uiState.value.aliyot
        val members = _uiState.value.members

        val income = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val balance = income - expense

        val pledges = aliyot.filter { it.status == TransactionStatus.PLEDGED }.sumOf { it.amount } +
                members.sumOf { it.outstandingDebt }

        _uiState.update { current ->
            current.copy(
                totalIncome = income,
                totalExpense = expense,
                netBalance = balance,
                totalOutstandingPledges = pledges,
                integrityVerified = verifyLedgerIntegrity(txs)
            )
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            repository.allTransactions.collect { list ->
                _uiState.update { it.copy(transactions = list) }
                recalculateMetrics()
            }
        }
        viewModelScope.launch {
            repository.allMembers.collect { list ->
                _uiState.update { it.copy(members = list) }
                recalculateMetrics()
            }
        }
        viewModelScope.launch {
            repository.allAliyot.collect { list ->
                _uiState.update { it.copy(aliyot = list) }
                recalculateMetrics()
            }
        }
        viewModelScope.launch {
            repository.allKollelStudents.collect { list ->
                _uiState.update { it.copy(kollelStudents = list) }
            }
        }
        viewModelScope.launch {
            repository.allAttendance.collect { list ->
                _uiState.update { it.copy(kollelAttendance = list) }
            }
        }
        viewModelScope.launch {
            repository.allTzedakah.collect { list ->
                _uiState.update { it.copy(tzedakahFunds = list) }
            }
        }
        viewModelScope.launch {
            repository.getAllNeedyBeneficiaries().collect { list ->
                _uiState.update { it.copy(needyBeneficiaries = list) }
            }
        }
        viewModelScope.launch {
            repository.allEvents.collect { list ->
                _uiState.update { it.copy(events = list) }
            }
        }
    }

    private fun verifyLedgerIntegrity(txs: List<TransactionEntity>): Boolean {
        if (txs.isEmpty()) return true
        for (i in 0 until txs.size - 1) {
            val curr = txs[i]
            val prev = txs[i + 1]
            if (curr.prevHash.isNotEmpty() && prev.hash.isNotEmpty() && curr.prevHash != prev.hash) {
                return false
            }
        }
        return true
    }

    fun setScreen(screen: GabbaiScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setFinanceFilter(filter: TransactionType?) {
        _uiState.update { it.copy(activeFinanceFilter = filter) }
    }

    fun toggleVaultLock() {
        _uiState.update { it.copy(isVaultUnlocked = !it.isVaultUnlocked) }
    }

    fun checkPinAndUnlock(pin: String): Boolean {
        return if (pin == _uiState.value.vaultPin) {
            _uiState.update { it.copy(isVaultUnlocked = true) }
            true
        } else false
    }

    fun addTransaction(
        title: String,
        type: TransactionType,
        category: FinanceCategory,
        amount: Double,
        memberName: String,
        paymentMethod: String,
        status: TransactionStatus,
        note: String,
        currency: String = com.example.ui.settings.I18n.defaultCurrency
    ) {
        viewModelScope.launch {
            repository.addTransaction(
                currency = currency,
                title = title,
                type = type,
                category = category,
                amount = amount,
                memberName = memberName,
                paymentMethod = paymentMethod,
                status = status,
                privateNote = note
            )
            showMessage(L("ტრანზაქცია წარმატებით დაემატა და დაშიფრულია SHA-256!"))
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            showMessage(L("ტრანზაქცია წაშლილია"))
        }
    }

    fun addMember(
        fullName: String,
        hebrewName: String,
        phone: String,
        role: MemberRole,
        tribalStatus: String = "ישראל (ისრაელი)",
        status: MemberStatus = MemberStatus.ACTIVE,
        complianceCategory: MemberCompliance = MemberCompliance.NORMAL,
        blackListReason: String = "",
        seatNumber: String = "",
        familyInfo: String = "",
        address: String = "",
        annualFee: Double,
        duesPaid: Boolean,
        outstandingDebt: Double,
        yahrzeit: String,
        privateNotes: String
    ) {
        viewModelScope.launch {
            repository.addMember(
                fullName = fullName,
                hebrewName = hebrewName,
                phone = phone,
                role = role,
                tribalStatus = tribalStatus,
                status = status,
                complianceCategory = complianceCategory,
                blackListReason = blackListReason,
                seatNumber = seatNumber,
                familyInfo = familyInfo,
                address = address,
                annualFee = annualFee,
                duesPaid = duesPaid,
                outstandingDebt = outstandingDebt,
                yahrzeit = yahrzeit,
                privateNotes = privateNotes
            )
            showMessage(L("ჯამაათის ახალი წევრი წარმატებით დარეგისტრირდა!"))
        }
    }

    /** Saves a fully built member (used by the member form). */
    fun addMemberEntity(member: MemberEntity) {
        viewModelScope.launch {
            repository.insertMember(member)
            showMessage(L("ჯამაათის ახალი წევრი წარმატებით დარეგისტრირდა!"))
        }
    }

    /** Adds a person by name only — used when a name typed in any form is not in the members list yet. */
    fun quickAddMember(fullName: String) {
        val name = fullName.trim()
        if (name.isEmpty() || _uiState.value.members.any { it.fullName.equals(name, ignoreCase = true) }) return
        viewModelScope.launch {
            repository.insertMember(MemberEntity(fullName = name, annualFeeAmount = 0.0, currency = com.example.ui.settings.I18n.defaultCurrency))
            showMessage(com.example.ui.settings.Lf(L("«{0}» დაემატა წევრების ბაზაში"), name))
        }
    }

    fun addNeedyEntity(entity: NeedyBeneficiaryEntity) {
        viewModelScope.launch {
            repository.insertNeedy(entity)
            showMessage(L("ბენეფიციარი ოჯახი წარმატებით დაემატა გაჭირვებულთა ბაზას!"))
        }
    }

    fun updateEvent(event: SynagogueEventEntity) {
        viewModelScope.launch { repository.updateEvent(event) }
    }

    fun updateMember(member: MemberEntity) {
        viewModelScope.launch {
            repository.updateMember(member)
            showMessage(L("ჯამაათის წევრის მონაცემები განახლდა!"))
        }
    }

    fun toggleMemberDues(member: MemberEntity) {
        viewModelScope.launch {
            repository.updateMember(member.copy(duesPaid = !member.duesPaid))
            showMessage(L("საწევრო გადასახადის სტატუსი განახლდა!"))
        }
    }

    fun deleteMember(id: Long) {
        viewModelScope.launch {
            repository.deleteMember(id)
            showMessage(L("წევრი წაშლილია"))
        }
    }

    fun addAliyah(
        parasha: String,
        type: AliyahType,
        winner: String,
        amount: Double,
        status: TransactionStatus,
        currency: String = com.example.ui.settings.I18n.defaultCurrency,
        paymentMethod: String = L("ნაღდი")
    ) {
        viewModelScope.launch {
            repository.addAliyah(parasha, type, winner, amount, status, currency, paymentMethod)
            showMessage(L("ალია / ფასუკი წარმატებით ჩაიწერა!"))
        }
    }

    fun markAliyahPaid(aliyah: AliyahEntity) {
        viewModelScope.launch {
            repository.markAliyahPaid(aliyah)
            showMessage(L("ალიის საფასური მიღებულია და გატარებულია შემოსავალში!"))
        }
    }

    fun deleteAliyah(id: Long) {
        viewModelScope.launch {
            repository.deleteAliyah(id)
            showMessage(L("ალიის ჩანაწერი წაშლილია"))
        }
    }

    fun addKollelStudent(fullName: String, phone: String, masechet: String, stipend: Double, currency: String = com.example.ui.settings.I18n.defaultCurrency) {
        viewModelScope.launch {
            val student = KollelStudentEntity(
                fullName = fullName,
                phone = phone,
                currentMasechet = masechet,
                monthlyStipend = stipend,
                currency = currency,
                stipendPaidThisMonth = false
            )
            repository.addKollelStudent(student)
            showMessage(L("ქოლელის აბრეხი დამატებულია!"))
        }
    }

    fun recordAttendance(studentId: Long, studentName: String, morning: Boolean, afternoon: Boolean) {
        viewModelScope.launch {
            val record = KollelAttendanceEntity(
                studentId = studentId,
                studentName = studentName,
                sederMorning = morning,
                sederAfternoon = afternoon,
                remarks = L("სედერი დადასტურებულია")
            )
            repository.recordKollelAttendance(record)
            showMessage(Lf("დასწრება აღრიცხულია: {0}", studentName))
        }
    }

    fun toggleStipendPaid(student: KollelStudentEntity) {
        viewModelScope.launch {
            val updated = student.copy(stipendPaidThisMonth = !student.stipendPaidThisMonth)
            repository.updateKollelStudent(updated)

            if (updated.stipendPaidThisMonth) {
                repository.addTransaction(
                    title = Lf("ქოლელის სტიპენდია: {0}", student.fullName),
                    type = TransactionType.EXPENSE,
                    category = FinanceCategory.KOLLEL_STIPENDS,
                    amount = student.monthlyStipend,
                    memberName = student.fullName,
                    paymentMethod = L("საბანკო გადარიცხვა"),
                    status = TransactionStatus.PAID,
                    privateNote = "Monthly stipend disbursement for ${student.currentMasechet}",
                    currency = student.currency
                )
            }
            showMessage(L("სტიპენდიის სტატუსი განახლდა!"))
        }
    }

    fun distributeTzedakah(
        code: String,
        category: TzedakahCategory,
        amount: Double,
        isEmergency: Boolean,
        approvedBy: String,
        details: String,
        currency: String = com.example.ui.settings.I18n.defaultCurrency
    ) {
        viewModelScope.launch {
            repository.distributeTzedakah(code, category, amount, isEmergency, approvedBy, details, currency)
            showMessage(L("ცედაკა გაცემულია და საიდუმლოდ დაშიფრულია (מתן בסתר)!"))
        }
    }

    fun deleteTzedakah(id: Long) {
        viewModelScope.launch {
            repository.deleteTzedakah(id)
            showMessage(L("ცედაკის ჩანაწერი წაშლილია"))
        }
    }

    fun addNeedyBeneficiary(
        identifierCode: String,
        fullNameOrPseudonym: String,
        category: TzedakahCategory,
        familyMembersCount: Int,
        phone: String,
        address: String,
        monthlyEstimatedNeed: Double,
        monthlyApprovedAid: Double,
        status: BeneficiaryStatus,
        urgentNotes: String
    ) {
        viewModelScope.launch {
            repository.addNeedyBeneficiary(
                identifierCode = identifierCode,
                fullNameOrPseudonym = fullNameOrPseudonym,
                category = category,
                familyMembersCount = familyMembersCount,
                phone = phone,
                address = address,
                monthlyEstimatedNeed = monthlyEstimatedNeed,
                monthlyApprovedAid = monthlyApprovedAid,
                status = status,
                urgentNotes = urgentNotes
            )
            showMessage(L("ბენეფიციარი ოჯახი წარმატებით დაემატა გაჭირვებულთა ბაზას!"))
        }
    }

    fun updateNeedyBeneficiary(beneficiary: NeedyBeneficiaryEntity) {
        viewModelScope.launch {
            repository.updateNeedyBeneficiary(beneficiary)
            showMessage(L("ბენეფიციარის მონაცემები განახლდა!"))
        }
    }

    fun deleteNeedyBeneficiary(id: Long) {
        viewModelScope.launch {
            repository.deleteNeedyBeneficiary(id)
            showMessage(L("ბენეფიციარი წაშლილია ბაზიდან"))
        }
    }

    fun addEvent(
        title: String,
        type: String,
        sponsor: String,
        cost: Double,
        sponsorDonation: Double,
        guests: Int,
        currency: String = com.example.ui.settings.I18n.defaultCurrency
    ) {
        viewModelScope.launch {
            val event = SynagogueEventEntity(
                title = title,
                eventType = type,
                sponsorName = sponsor,
                totalCost = cost,
                sponsorContribution = sponsorDonation,
                expectedGuests = guests,
                currency = currency,
                isCompleted = false
            )
            repository.addEvent(event)
            showMessage(L("ღონისძიება წარმატებით ჩაინიშნა!"))
        }
    }

    fun deleteEvent(id: Long) {
        viewModelScope.launch {
            repository.deleteEvent(id)
            showMessage(L("ღონისძიება წაშლილია"))
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    private fun showMessage(msg: String) {
        _uiState.update { it.copy(statusMessage = msg) }
    }

    fun backupTo(uri: android.net.Uri, onDone: (Result<Int>) -> Unit) {
        viewModelScope.launch { onDone(runCatching { com.example.data.backup.BackupManager.export(getApplication(), uri) }) }
    }
    fun restoreFrom(uri: android.net.Uri, onDone: (Result<Int>) -> Unit) {
        viewModelScope.launch { onDone(runCatching { com.example.data.backup.BackupManager.restore(getApplication(), uri) }) }
    }
    fun wipeDatabase(onDone: (Result<Unit>) -> Unit) {
        viewModelScope.launch { onDone(runCatching { com.example.data.backup.BackupManager.wipeAll(getApplication()) }) }
    }
}
