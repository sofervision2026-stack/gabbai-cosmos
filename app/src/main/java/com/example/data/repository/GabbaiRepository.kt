package com.example.data.repository

import com.example.data.dao.GabbaiDao
import com.example.data.model.*
import com.example.data.security.CosmicVault
import kotlinx.coroutines.flow.Flow

class GabbaiRepository(private val dao: GabbaiDao) {

    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val allMembers: Flow<List<MemberEntity>> = dao.getAllMembers()
    val allAliyot: Flow<List<AliyahEntity>> = dao.getAllAliyot()
    val allKollelStudents: Flow<List<KollelStudentEntity>> = dao.getAllKollelStudents()
    val allAttendance: Flow<List<KollelAttendanceEntity>> = dao.getAllAttendance()
    val allTzedakah: Flow<List<TzedakahEntity>> = dao.getAllTzedakah()
    val allEvents: Flow<List<SynagogueEventEntity>> = dao.getAllEvents()

    suspend fun addTransaction(
        title: String,
        type: TransactionType,
        category: FinanceCategory,
        amount: Double,
        memberName: String,
        paymentMethod: String,
        status: TransactionStatus,
        privateNote: String,
        currency: String = com.example.ui.settings.I18n.defaultCurrency
    ): Long {
        val lastTx = dao.getLastTransaction()
        val prevHash = lastTx?.hash ?: "0000000000000000"
        val timestamp = System.currentTimeMillis()
        val encryptedNote = CosmicVault.encrypt(privateNote)

        // Compute SHA-256 cryptographic chain hash
        val calculatedHash = CosmicVault.computeLedgerHash(
            id = (lastTx?.id ?: 0) + 1,
            prevHash = prevHash,
            timestamp = timestamp,
            amount = amount,
            type = type.name,
            title = title
        )

        val tx = TransactionEntity(
            title = title,
            type = type,
            category = category,
            amount = amount,
            dateMillis = timestamp,
            memberName = memberName,
            paymentMethod = paymentMethod,
            status = status,
            encryptedNote = encryptedNote,
            currency = currency,
            prevHash = prevHash,
            hash = calculatedHash
        )
        return dao.insertTransaction(tx)
    }

    suspend fun deleteTransaction(id: Long) = dao.deleteTransaction(id)

    suspend fun addMember(
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
    ): Long {
        val entity = MemberEntity(
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
            annualFeeAmount = annualFee,
            duesPaid = duesPaid,
            outstandingDebt = outstandingDebt,
            lastPaymentDateMillis = System.currentTimeMillis() - (86400000L * 14),
            lastPaymentAmount = if (duesPaid) annualFee else 0.0,
            yahrzeitDate = yahrzeit,
            notesEncrypted = CosmicVault.encrypt(privateNotes)
        )
        return dao.insertMember(entity)
    }

    suspend fun updateMember(member: MemberEntity) = dao.updateMember(member)
    suspend fun insertMember(member: MemberEntity) = dao.insertMember(member)
    suspend fun insertNeedy(entity: NeedyBeneficiaryEntity) = dao.insertNeedyBeneficiary(entity)
    suspend fun deleteMember(id: Long) = dao.deleteMember(id)

    suspend fun addAliyah(
        parasha: String,
        type: AliyahType,
        winner: String,
        amount: Double,
        status: TransactionStatus,
        currency: String = com.example.ui.settings.I18n.defaultCurrency,
        paymentMethod: String = "ნაღდი"
    ): Long {
        val aliyah = AliyahEntity(
            currency = currency,
            parashaName = parasha,
            aliyahType = type,
            winnerName = winner,
            amount = amount,
            dateMillis = System.currentTimeMillis(),
            status = status
        )
        val aliyahId = dao.insertAliyah(aliyah)

        // If paid immediately, automatically register in Financial Ledger
        if (status == TransactionStatus.PAID) {
            addTransaction(
                title = "სეფერ თორა - ${type.titleKa} ($parasha)",
                type = TransactionType.INCOME,
                category = FinanceCategory.ALIYOT_TORAH,
                amount = amount,
                memberName = winner,
                paymentMethod = paymentMethod,
                status = TransactionStatus.PAID,
                privateNote = "Aliyah auction winner: $winner",
                currency = currency
            )
        }
        return aliyahId
    }

    suspend fun markAliyahPaid(aliyah: AliyahEntity) {
        dao.updateAliyahStatus(aliyah.id, TransactionStatus.PAID)
        addTransaction(
            title = "სეფერ თორა - ${aliyah.aliyahType.titleKa} (${aliyah.parashaName})",
            type = TransactionType.INCOME,
            category = FinanceCategory.ALIYOT_TORAH,
            amount = aliyah.amount,
            memberName = aliyah.winnerName,
            paymentMethod = "დავალიანების დაფარვა",
            status = TransactionStatus.PAID,
            privateNote = "Pledged aliyah payment completed",
            currency = aliyah.currency
        )
    }

    suspend fun deleteAliyah(id: Long) = dao.deleteAliyah(id)

    // Kollel
    suspend fun addKollelStudent(student: KollelStudentEntity) = dao.insertKollelStudent(student)
    suspend fun updateKollelStudent(student: KollelStudentEntity) = dao.updateKollelStudent(student)
    suspend fun recordKollelAttendance(attendance: KollelAttendanceEntity) = dao.insertAttendance(attendance)

    // Tzedakah
    suspend fun distributeTzedakah(
        code: String,
        category: TzedakahCategory,
        amount: Double,
        isEmergency: Boolean,
        approvedBy: String,
        details: String,
        currency: String = com.example.ui.settings.I18n.defaultCurrency
    ): Long {
        val tzedakah = TzedakahEntity(
            currency = currency,
            recipientPrivacyCode = code,
            category = category,
            amount = amount,
            dateMillis = System.currentTimeMillis(),
            isEmergency = isEmergency,
            approvedByRabbi = approvedBy,
            encryptedDetails = CosmicVault.encrypt(details)
        )
        val id = dao.insertTzedakah(tzedakah)

        // Also record as Synagogue expense in ledger
        addTransaction(
            title = "ცედაკის გაცემა: ${category.titleKa} ($code)",
            type = TransactionType.EXPENSE,
            category = FinanceCategory.TZEDAKAH_DISBURSEMENT,
            amount = amount,
            memberName = "კუპათ ცედაკა / $code",
            paymentMethod = "საქველმოქმედო ფონდი",
            status = TransactionStatus.PAID,
            privateNote = "Discreet Tzedakah disbursement: $details",
            currency = currency
        )
        return id
    }

    suspend fun deleteTzedakah(id: Long) = dao.deleteTzedakah(id)

    // Needy Beneficiaries (ნეზაკაკიმ / გაჭირვებულთა ბაზა)
    fun getAllNeedyBeneficiaries(): Flow<List<NeedyBeneficiaryEntity>> = dao.getAllNeedyBeneficiaries()

    suspend fun addNeedyBeneficiary(
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
    ): Long {
        val entity = NeedyBeneficiaryEntity(
            identifierCode = identifierCode,
            fullNameOrPseudonym = fullNameOrPseudonym,
            category = category,
            familyMembersCount = familyMembersCount,
            phone = phone,
            address = address,
            monthlyEstimatedNeed = monthlyEstimatedNeed,
            monthlyApprovedAid = monthlyApprovedAid,
            status = status,
            encryptedNotes = CosmicVault.encrypt(urgentNotes)
        )
        return dao.insertNeedyBeneficiary(entity)
    }

    suspend fun updateNeedyBeneficiary(beneficiary: NeedyBeneficiaryEntity) =
        dao.updateNeedyBeneficiary(beneficiary)

    suspend fun deleteNeedyBeneficiary(id: Long) = dao.deleteNeedyBeneficiary(id)

    // Events
    suspend fun addEvent(event: SynagogueEventEntity) = dao.insertEvent(event)
    suspend fun updateEvent(event: SynagogueEventEntity) = dao.updateEvent(event)
    suspend fun deleteEvent(id: Long) = dao.deleteEvent(id)

    /**
     * Seeds initial realistic synagogue data if database is empty.
     */
    suspend fun checkAndSeedInitialData() {
        if (dao.getTransactionCount() > 0) return

        // 1. Members & Staff
        val members = listOf(
            MemberEntity(
                fullName = "הרב דוד בן-ציון (დავით ბენ-ციონი)",
                hebrewName = "דוד בן ציון",
                phone = "+995 599 123 456",
                role = MemberRole.SENIOR_RABBI,
                annualFeeAmount = 0.0,
                duesPaid = true,
                outstandingDebt = 0.0,
                yahrzeitDate = "חשוון טו (15 Cheshvan)"
            ),
            MemberEntity(
                fullName = "מיכאל גבאי (მიხეილ გაბაი)",
                hebrewName = "מיכאל גבאי",
                phone = "+995 599 223 344",
                role = MemberRole.GABBAI,
                annualFeeAmount = 1500.0,
                duesPaid = true,
                outstandingDebt = 0.0,
                yahrzeitDate = "תשרי כ (20 Tishrei)"
            ),
            MemberEntity(
                fullName = "יצחק לוין (იცხაკ ლევინი)",
                hebrewName = "יצחק לוין",
                phone = "+995 599 334 455",
                role = MemberRole.CHAZZAN,
                annualFeeAmount = 0.0,
                duesPaid = true,
                outstandingDebt = 0.0
            ),
            MemberEntity(
                fullName = "אברהם שמש (აბრაამ შამაში)",
                hebrewName = "אברהם שמש",
                phone = "+995 599 445 566",
                role = MemberRole.SHAMASH,
                annualFeeAmount = 0.0,
                duesPaid = true,
                outstandingDebt = 0.0
            ),
            MemberEntity(
                fullName = "שלמה ממיסטვალוב (შალვა მამისთვალოვი)",
                hebrewName = "שלמה ממיסטבלוב",
                phone = "+995 599 556 677",
                role = MemberRole.CONGREGANT,
                annualFeeAmount = 1800.0,
                duesPaid = true,
                outstandingDebt = 0.0,
                yahrzeitDate = "כסלו ב (2 Kislev)"
            ),
            MemberEntity(
                fullName = "דניאל קריחלי (დანიელ კრიხელი)",
                hebrewName = "דניאל קריחלי",
                phone = "+995 599 667 788",
                role = MemberRole.CONGREGANT,
                status = MemberStatus.ACTIVE,
                complianceCategory = MemberCompliance.RED_LIST,
                blackListReason = "ვადაგადაცილებული ვალი წინა კვირის ალიიდან (>60 დღე)",
                annualFeeAmount = 1200.0,
                duesPaid = true,
                outstandingDebt = 400.0,
                lastPaymentDateMillis = System.currentTimeMillis() - (86400000L * 65),
                lastPaymentAmount = 200.0,
                yahrzeitDate = "שבט ח (8 Shevat)"
            ),
            MemberEntity(
                fullName = "אליהו בעזוב (ელიას ბააზოვი)",
                hebrewName = "אליהו בעזוב",
                phone = "+995 599 778 899",
                role = MemberRole.CONGREGANT,
                status = MemberStatus.ALIYAH_ISRAEL,
                complianceCategory = MemberCompliance.NORMAL,
                seatNumber = "A-15 (შენახულია)",
                annualFeeAmount = 1200.0,
                duesPaid = false,
                outstandingDebt = 750.0,
                lastPaymentDateMillis = System.currentTimeMillis() - (86400000L * 90),
                lastPaymentAmount = 450.0
            ),
            MemberEntity(
                fullName = "შოთა გ. (სანქცირებული)",
                hebrewName = "שבתאי בן גבריאל",
                phone = "+995 599 990 011",
                role = MemberRole.CONGREGANT,
                status = MemberStatus.PASSIVE,
                complianceCategory = MemberCompliance.BLACK_LIST,
                blackListReason = "უარი განაცხადა აუქციონის ნედერის გადახდაზე (გაბაის სანქცია)",
                annualFeeAmount = 1200.0,
                duesPaid = false,
                outstandingDebt = 1500.0,
                lastPaymentDateMillis = System.currentTimeMillis() - (86400000L * 180),
                lastPaymentAmount = 0.0
            ),
            MemberEntity(
                fullName = "יוסף ספיאשווילי (იოსებ სეფიაშვილი)",
                hebrewName = "יוסף ספיאשווילי",
                phone = "+995 599 889 900",
                role = MemberRole.CONGREGANT,
                status = MemberStatus.ACTIVE,
                complianceCategory = MemberCompliance.NORMAL,
                annualFeeAmount = 2400.0,
                duesPaid = true,
                outstandingDebt = 0.0,
                lastPaymentDateMillis = System.currentTimeMillis() - (86400000L * 5),
                lastPaymentAmount = 2400.0
            ),
            MemberEntity(
                fullName = "იოსებ ენოხი ზ''ლ (נלב״ע)",
                hebrewName = "יוסף בן אליהו ז״ל",
                phone = "-",
                role = MemberRole.CONGREGANT,
                status = MemberStatus.DECEASED,
                complianceCategory = MemberCompliance.NORMAL,
                annualFeeAmount = 0.0,
                duesPaid = true,
                outstandingDebt = 0.0,
                yahrzeitDate = "אדר יב (12 Adar)"
            )
        )
        members.forEach { dao.insertMember(it) }

        // 2. Financial Ledger initial transactions with SHA-256 chain
        addTransaction(
            title = "წლიური საწევრო გადასახადები (8 წევრი)",
            type = TransactionType.INCOME,
            category = FinanceCategory.MEMBERSHIP_DUES,
            amount = 10800.0,
            memberName = "სინაგოგის საბჭო",
            paymentMethod = "საბანკო გადარიცხვა",
            status = TransactionStatus.PAID,
            privateNote = "Annual membership bundle collected for 5786"
        )

        addTransaction(
            title = "რაბინატისა და პერსონალის თვიური ხელფასები",
            type = TransactionType.EXPENSE,
            category = FinanceCategory.SALARIES_RABBIS,
            amount = 4500.0,
            memberName = "რაბინი, ხაზანი, შამაში",
            paymentMethod = "საბანკო გადარიცხვა",
            status = TransactionStatus.PAID,
            privateNote = "Monthly institutional payroll"
        )

        addTransaction(
            title = "სეფერ თორის ფასუკები & ალიები (შაბათი ბერეშით)",
            type = TransactionType.INCOME,
            category = FinanceCategory.ALIYOT_TORAH,
            amount = 3250.0,
            memberName = "შალვა მამისთვალოვი & იოსებ სეფიაშვილი",
            paymentMethod = "ნაღდი / Cash",
            status = TransactionStatus.PAID,
            privateNote = "Auction proceeds for Parashat Bereshit"
        )

        addTransaction(
            title = "სინაგოგის კომუნალური (დენი, გათბობა, კონდიცირება)",
            type = TransactionType.EXPENSE,
            category = FinanceCategory.UTILITIES_COMMUNAL,
            amount = 980.0,
            memberName = "ენერგო კომპანია",
            paymentMethod = "საბანკო გადარიცხვა",
            status = TransactionStatus.PAID,
            privateNote = "Main hall HVAC and high-ceiling illumination"
        )

        addTransaction(
            title = "სეფერ თორის შემოწმება & სოფერის აღდგენითი სამუშაოები",
            type = TransactionType.EXPENSE,
            category = FinanceCategory.SEFER_TORAH_BOOKS,
            amount = 1200.0,
            memberName = "სოფერ STAM იერუშალაიმი",
            paymentMethod = "საბანკო გადარიცხვა",
            status = TransactionStatus.PAID,
            privateNote = "Restoration of ancient Ashkenazi Torah parchment scroll"
        )

        addTransaction(
            title = "ქოლელის აბრეხების თვიური სტიპენდიები (მილგა)",
            type = TransactionType.EXPENSE,
            category = FinanceCategory.KOLLEL_STIPENDS,
            amount = 3200.0,
            memberName = "ქოლელ თორა ვადაათ",
            paymentMethod = "პირადი საბანკო",
            status = TransactionStatus.PAID,
            privateNote = "Monthly Kollel stipends for 4 full-time Avreichim"
        )

        addTransaction(
            title = "სადღესასწაულო ქიდუშის სპონსორობა",
            type = TransactionType.INCOME,
            category = FinanceCategory.KIDDUSH_SPONSOR,
            amount = 1100.0,
            memberName = "დანიელ კრიხელი",
            paymentMethod = "ბარათი / POS",
            status = TransactionStatus.PAID,
            privateNote = "Grand Kiddush sponsorship in honor of family simcha"
        )

        // 3. Aliyot Records
        dao.insertAliyah(
            AliyahEntity(
                parashaName = "פרשת בראשית (ბერეშით)",
                aliyahType = AliyahType.KOHEN,
                winnerName = "דניאל קריחלי (დანიელ კრიხელი)",
                amount = 350.0,
                status = TransactionStatus.PAID
            )
        )
        dao.insertAliyah(
            AliyahEntity(
                parashaName = "פרשת בראשית (ბერეშით)",
                aliyahType = AliyahType.LEVI,
                winnerName = "יצחק לוין (იცხაკ ლევინი)",
                amount = 300.0,
                status = TransactionStatus.PAID
            )
        )
        dao.insertAliyah(
            AliyahEntity(
                parashaName = "פרשת בראשית (ბერეშით)",
                aliyahType = AliyahType.SHLISHI,
                winnerName = "שלמה ממיסטვალוב (შალვა მამისთვალოვი)",
                amount = 1200.0,
                status = TransactionStatus.PAID
            )
        )
        dao.insertAliyah(
            AliyahEntity(
                parashaName = "פרשת בראשית (ბერეშით)",
                aliyahType = AliyahType.SHISHI,
                winnerName = "יוסף ספיאשווילי (იოსებ სეფიაშვილი)",
                amount = 1400.0,
                status = TransactionStatus.PAID
            )
        )
        dao.insertAliyah(
            AliyahEntity(
                parashaName = "פרשת נח (ნოახი)",
                aliyahType = AliyahType.PETICHA,
                winnerName = "אליהו בעזוב (ელიას ბააზოვი)",
                amount = 500.0,
                status = TransactionStatus.PLEDGED
            )
        )
        dao.insertAliyah(
            AliyahEntity(
                parashaName = "פרשת נח (ნოახი)",
                aliyahType = AliyahType.PESUKIM,
                winnerName = "דניאל קრიხელი (დანიელ კრიხელი)",
                amount = 250.0,
                status = TransactionStatus.PLEDGED
            )
        )

        // 4. Kollel Students
        val kollelStudents = listOf(
            KollelStudentEntity(
                fullName = "הרב משה קליין (მოშე კლაინი)",
                phone = "+995 599 111 222",
                currentMasechet = "מסכת שבת (შაბათი - ჰალახა)",
                monthlyStipend = 850.0,
                stipendPaidThisMonth = true,
                totalSessionsPresent = 24,
                totalSessionsExpected = 24
            ),
            KollelStudentEntity(
                fullName = "ר' ישראל פרידמן (ისრაელ ფრიდმანი)",
                phone = "+995 599 222 333",
                currentMasechet = "מסכת סנהדרין (სანჰედრინი)",
                monthlyStipend = 800.0,
                stipendPaidThisMonth = true,
                totalSessionsPresent = 22,
                totalSessionsExpected = 24
            ),
            KollelStudentEntity(
                fullName = "ר' רפאל אברהמי (რაფაელ აბრაამი)",
                phone = "+995 599 333 444",
                currentMasechet = "הלכות שבת - שולחן ערוך",
                monthlyStipend = 800.0,
                stipendPaidThisMonth = false,
                totalSessionsPresent = 21,
                totalSessionsExpected = 24
            ),
            KollelStudentEntity(
                fullName = "ר' יעקב גולדברג (იაკობ გოლდბერგი)",
                phone = "+995 599 444 555",
                currentMasechet = "מסכת פסחים (ფესახიმი)",
                monthlyStipend = 750.0,
                stipendPaidThisMonth = false,
                totalSessionsPresent = 23,
                totalSessionsExpected = 24
            )
        )
        kollelStudents.forEach { dao.insertKollelStudent(it) }

        // Kollel Attendance Sample
        dao.insertAttendance(
            KollelAttendanceEntity(
                studentId = 1,
                studentName = "הרב משה קליין",
                sederMorning = true,
                sederAfternoon = true,
                remarks = "სრული დასწრება - შესანიშნავი ქიდუში"
            )
        )
        dao.insertAttendance(
            KollelAttendanceEntity(
                studentId = 2,
                studentName = "ר' ישראל פרידמן",
                sederMorning = true,
                sederAfternoon = true,
                remarks = "სედერი ა და ბ ჩატარდა"
            )
        )

        // 5. Tzedakah Funds
        dao.insertTzedakah(
            TzedakahEntity(
                recipientPrivacyCode = "TZ-702 (ფარული ქველმოქმედება)",
                category = TzedakahCategory.MEDICAL_URGENT,
                amount = 650.0,
                isEmergency = true,
                approvedByRabbi = "הרב הראשי דוד בן-ציון",
                encryptedDetails = CosmicVault.encrypt("Urgent pediatric medicine assistance for congregant family")
            )
        )
        dao.insertTzedakah(
            TzedakahEntity(
                recipientPrivacyCode = "TZ-814 (შაბათის კალათა)",
                category = TzedakahCategory.FOOD_BASKETS,
                amount = 400.0,
                isEmergency = false,
                approvedByRabbi = "גבאי ראשי מיכאל",
                encryptedDetails = CosmicVault.encrypt("Shabbat food supplies for elderly congregant")
            )
        )

        // 6. Needy Beneficiaries (გაჭირვებულთა ბაზა)
        dao.insertNeedyBeneficiary(
            NeedyBeneficiaryEntity(
                identifierCode = "ND-101",
                fullNameOrPseudonym = "ოჯახი #101 (მრავალშვილიანი / ობლები)",
                category = TzedakahCategory.FOOD_BASKETS,
                familyMembersCount = 6,
                phone = "+995 599 112 233",
                address = "ძველი თბილისი",
                monthlyEstimatedNeed = 800.0,
                monthlyApprovedAid = 500.0,
                status = BeneficiaryStatus.ACTIVE,
                encryptedNotes = CosmicVault.encrypt("ქვრივი 5 მცირეწლოვანი შვილით. ყოველთვიური სასურსათო კალათა")
            )
        )
        dao.insertNeedyBeneficiary(
            NeedyBeneficiaryEntity(
                identifierCode = "ND-102",
                fullNameOrPseudonym = "პატარძალი მ. (შემწეობა ქორწილისთვის)",
                category = TzedakahCategory.HACHNASAT_KALLAH,
                familyMembersCount = 2,
                phone = "+995 599 223 344",
                address = "ავლაბარი",
                monthlyEstimatedNeed = 1500.0,
                monthlyApprovedAid = 1200.0,
                status = BeneficiaryStatus.ONE_TIME,
                encryptedNotes = CosmicVault.encrypt("ქორწილის ხარჯებისა და საყოფაცხოვრებო ნივთების დახმარება")
            )
        )
        dao.insertNeedyBeneficiary(
            NeedyBeneficiaryEntity(
                identifierCode = "ND-103",
                fullNameOrPseudonym = "ოჯახი #103 (სასწრაფო სამედიცინო)",
                category = TzedakahCategory.MEDICAL_URGENT,
                familyMembersCount = 3,
                phone = "+995 599 334 455",
                address = "სოლოლაკი",
                monthlyEstimatedNeed = 1200.0,
                monthlyApprovedAid = 800.0,
                status = BeneficiaryStatus.ACTIVE,
                encryptedNotes = CosmicVault.encrypt("ონკოლოგიური რეაბილიტაციის მედიკამენტების დაფარვა")
            )
        )

        // 7. Events
        dao.insertEvent(
            SynagogueEventEntity(
                title = "შაბათ ბერეშითის საზეიმო ქიდუში",
                eventType = "Shabbat Gala Kiddush",
                sponsorName = "მამისთვალოვების საგვარეულო",
                totalCost = 1200.0,
                sponsorContribution = 1200.0,
                expectedGuests = 150,
                isCompleted = true
            )
        )
        dao.insertEvent(
            SynagogueEventEntity(
                title = "ბარ-მიცვა - დანიელ კრიხელის ვაჟი",
                eventType = "Bar Mitzvah / עלייה לתורה",
                sponsorName = "დ. კრიხელი",
                totalCost = 900.0,
                sponsorContribution = 1000.0,
                expectedGuests = 120,
                isCompleted = false
            )
        )
    }
}
