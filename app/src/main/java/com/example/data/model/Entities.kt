package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.example.ui.settings.L
import androidx.room.PrimaryKey

enum class TransactionType {
    INCOME,   // შემოსავალი
    EXPENSE   // გასავალი
}

enum class FinanceCategory(private val ka: String, val titleHe: String) {
    // Incomes
    ALIYOT_TORAH("სეფერ თორის ფასუკები/ალიები", "עליות ופתיחת ההיכל"),
    NEDARIM_PLEDGES("ნედერები და შემოწირულობები", "נדרים ונדבות"),
    MEMBERSHIP_DUES("წლიური საწევრო გადასახადი", "דמי חבר שנתיים"),
    TZEDAKAH_INCOME("ცედაკის ყუთის შემოსავალი", "קופת צדקה"),
    KIDDUSH_SPONSOR("ქიდუშის/დარბაზის სპონსორობა", "חסות קידוש ואירועים"),
    YAHRZEIT_LIGHTS("იარცეიტის სანთლები/მოხსენიება", "נרות נשמה ואזכרה"),

    // Expenses
    SALARIES_RABBIS("რაბინატისა და პერსონალის ხელფასები", "משכורות רבנים וצוות"),
    UTILITIES_COMMUNAL("კომუნალური (დენი, გაზი, წყალი)", "חשמל, מים וארנונה"),
    REPAIRS_RENOVATION("სარემონტო და სამშენებლო სამუშაოები", "שיפוצים ואחזקה"),
    SEFER_TORAH_BOOKS("სეფერ თორის შემოწმება და წიგნები", "ספרי קודש ותיקון סת\"ם"),
    EVENTS_CATERING("ღონისძიებებისა და ქიდუშის ხარჯები", "הוצאות אירועים וסעודות"),
    KOLLEL_STIPENDS("ქოლელის აბრეხების სტიპენდიები", "מלגות אברכי הכולל"),
    TZEDAKAH_DISBURSEMENT("ცედაკის გაცემა გაჭირვებულებზე", "חלוקת צדקה לעניים"),
    SECURITY_CLEANING("დაცვა და დასუფთავება", "אבטחה וניקיון");
    val titleKa: String get() = L(ka)
    val keyKa: String get() = ka
}

enum class TransactionStatus {
    PAID,      // გადახდილია
    PENDING,   // მოლოდინშია
    PLEDGED    // დაპირებულია (ნედერი)
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: TransactionType,
    val category: FinanceCategory,
    val amount: Double,
    @ColumnInfo(defaultValue = "GEL") val currency: String = "GEL",
    val dateMillis: Long = System.currentTimeMillis(),
    val memberName: String = "",
    val paymentMethod: String = "ნაღდი / Cash", // Card, Transfer, Cash, Check
    val status: TransactionStatus = TransactionStatus.PAID,
    val encryptedNote: String = "",
    val prevHash: String = "0000000000000000",
    val hash: String = ""
)

enum class MemberRole(private val ka: String, val titleHe: String) {
    SENIOR_RABBI("მთავარი რაბინი", "רב ראשי / מרא דאתרא"),
    GABBAI("გაბაი", "גבאי בית הכנסת"),
    CHAZZAN("ხაზანი (მგალობელი)", "חזן הקהילה"),
    SHAMASH("შამაში (მსახური)", "שמש בית הכנסת"),
    ADMIN("ადმინისტრატორი", "מנהל אדמיניסטרטיבי"),
    CONGREGANT("მრევლის წევრი", "חבר הקהילה / מתפלל");
    val titleKa: String get() = L(ka)
    val keyKa: String get() = ka
}

enum class MemberStatus(private val ka: String, val titleHe: String) {
    ACTIVE("აქტიური წევრი", "חבר פעיל"),
    PASSIVE("პასიური წევრი", "חבר פסיבי"),
    ALIYAH_ISRAEL("ავიდა ისრაელში (ალია)", "עלה לארץ ישראל"),
    OTHER_CITY("ცხოვრობს სხვა ქალაქში", "מתגורר בעיר אחרת"),
    EMIGRATED("აღარ ცხოვრობს ქვეყანაში", "יצא לחו״ל"),
    DECEASED("გარდაიცვალა (ზ''ლ)", "נפטר / ז״ל");
    val titleKa: String get() = L(ka)
    val keyKa: String get() = ka
}

enum class MemberCompliance(private val ka: String, val titleHe: String) {
    NORMAL("ნორმალური (სუფთა)", "תקין"),
    RED_LIST("წითელი სია (ვადაგადაცილებული)", "רשימה אדומה"),
    BLACK_LIST("შავი სია (სანქცირებული / უარი)", "רשימה שחורה");
    val titleKa: String get() = L(ka)
    val keyKa: String get() = ka
}

@Entity(tableName = "members")
data class MemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val hebrewName: String = "",
    val phone: String = "",
    val role: MemberRole = MemberRole.CONGREGANT,
    val tribalStatus: String = "ישראל (ისრაელი)", // כהן, לוי, ישראל
    val status: MemberStatus = MemberStatus.ACTIVE,
    val complianceCategory: MemberCompliance = MemberCompliance.NORMAL,
    val blackListReason: String = "",
    val seatNumber: String = "",                 // მუდმივი ადგილი სინაგოგაში
    val familyInfo: String = "",                 // მეუღლე, შვილები
    val address: String = "",                    // საცხოვრებელი ადგილი
    val annualFeeAmount: Double = 1200.0,
    @ColumnInfo(defaultValue = "GEL") val currency: String = "GEL",
    val duesPaid: Boolean = true,
    val outstandingDebt: Double = 0.0,
    val lastPaymentDateMillis: Long = 0L,
    val lastPaymentAmount: Double = 0.0,
    val yahrzeitDate: String = "",
    val notesEncrypted: String = ""
)

enum class AliyahType(private val ka: String, val titleHe: String) {
    KOHEN("ქოჰენი", "כהן"),
    LEVI("ლევი", "לוי"),
    SHLISHI("შლიში", "שלישי"),
    REVII("რევიი", "רביעי"),
    CHAMISHI("ხამიში", "חמישי"),
    SHISHI("შიში (საპატიო)", "שישי"),
    SHEVII("შევიი", "שביעי"),
    MAFTIR("მაფტირი", "מפטיר"),
    HAGBAH("ჰაგბაჰა (აღმართვა)", "הגבהה"),
    GELILAH("გელილა (შეხვევა)", "גלילה"),
    PETICHA("ფეთიხათ ჰაარონი (კარადის გახსნა)", "פתיחת הארון"),
    PESUKIM("ფასუკების გაყიდვა", "קניית פסוקים"),
    SERVICE("სამსახური", "כיבוד"),
    MOSIF("დამატებითი / მოსიფ", "מוסיף");
    val titleKa: String get() = L(ka)
    val keyKa: String get() = ka
}

@Entity(tableName = "aliyot")
data class AliyahEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parashaName: String,
    val aliyahType: AliyahType,
    val winnerName: String,
    val amount: Double,
    @ColumnInfo(defaultValue = "GEL") val currency: String = "GEL",
    val dateMillis: Long = System.currentTimeMillis(),
    val status: TransactionStatus = TransactionStatus.PLEDGED
)

@Entity(tableName = "kollel_students")
data class KollelStudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val phone: String = "",
    val currentMasechet: String = "מסכת ברכות (Brakhot)",
    val monthlyStipend: Double = 800.0,
    @ColumnInfo(defaultValue = "GEL") val currency: String = "GEL",
    val stipendPaidThisMonth: Boolean = false,
    val totalSessionsPresent: Int = 22,
    val totalSessionsExpected: Int = 24
)

@Entity(tableName = "kollel_attendance")
data class KollelAttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val studentName: String,
    val dateMillis: Long = System.currentTimeMillis(),
    val sederMorning: Boolean = true,
    val sederAfternoon: Boolean = true,
    val remarks: String = ""
)

enum class TzedakahCategory(private val ka: String, val titleHe: String) {
    MATANOT_LE_EVYONIM("საჩუქარი ღარიბებს", "מתנות לאביונים"),
    KIMCHA_DE_PISCHA("ფქვილი ფესახისთვის", "קמחא דפסחא"),
    MEDICAL_URGENT("სასწრაფო სამედიცინო", "עזרה רפואית דחופה"),
    HACHNASAT_KALLAH("პატარძლის შემწეობა", "הכנסת כלה"),
    FOOD_BASKETS("სასურსათო კალათები", "סלי מזון לשבת"),
    EMERGENCY_AID("გადაუდებელი დახმარება", "עזרה דחופה");
    val titleKa: String get() = L(ka)
    val keyKa: String get() = ka
}

@Entity(tableName = "tzedakah_funds")
data class TzedakahEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipientPrivacyCode: String, // e.g. "TZ-784" for halachic discretion (מתן בסתר)
    val category: TzedakahCategory,
    val amount: Double,
    @ColumnInfo(defaultValue = "GEL") val currency: String = "GEL",
    val dateMillis: Long = System.currentTimeMillis(),
    val isEmergency: Boolean = false,
    val approvedByRabbi: String = "הרב הראשי",
    val encryptedDetails: String = ""
)

enum class BeneficiaryStatus(private val ka: String, val titleHe: String) {
    ACTIVE("აქტიური შემწეობა", "פעיל - קצבה חודשית"),
    ONE_TIME("ერთჯერადი დახმარება", "חד פעמי"),
    PAUSED("შეჩერებული", "מושהה"),
    COMPLETED("დასრულებული", "הסתיים");
    val titleKa: String get() = L(ka)
    val keyKa: String get() = ka
}

@Entity(tableName = "needy_beneficiaries")
data class NeedyBeneficiaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val identifierCode: String, // e.g. "ND-401" or privacy code
    val fullNameOrPseudonym: String,
    val category: TzedakahCategory = TzedakahCategory.FOOD_BASKETS,
    val familyMembersCount: Int = 1,
    val phone: String = "",
    val address: String = "",
    val monthlyEstimatedNeed: Double = 0.0,
    val monthlyApprovedAid: Double = 0.0,
    @ColumnInfo(defaultValue = "GEL") val currency: String = "GEL",
    val status: BeneficiaryStatus = BeneficiaryStatus.ACTIVE,
    val encryptedNotes: String = "",
    val dateRegisteredMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "synagogue_events")
data class SynagogueEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val eventType: String = "Shabbat Kiddush", // Bar Mitzvah, Brit, Wedding, Hillula, etc.
    val dateMillis: Long = System.currentTimeMillis(),
    val sponsorName: String = "",
    val totalCost: Double = 0.0,
    val sponsorContribution: Double = 0.0,
    @ColumnInfo(defaultValue = "GEL") val currency: String = "GEL",
    val expectedGuests: Int = 100,
    val isCompleted: Boolean = false
)
