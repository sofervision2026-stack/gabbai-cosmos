package com.example.data.model

import com.example.ui.settings.L

/** A ready-made finance operation: choosing it fills the title and the matching category. */
data class OperationPreset(private val ka: String, val type: TransactionType, val category: FinanceCategory) {
    val title: String get() = L(ka)
}

object Catalogs {
    val operations = listOf(
        // Income
        OperationPreset("ალია სეფერ თორაზე", TransactionType.INCOME, FinanceCategory.ALIYOT_TORAH),
        OperationPreset("ფასუკის ყიდვა", TransactionType.INCOME, FinanceCategory.ALIYOT_TORAH),
        OperationPreset("ფეთიხათ ჰაარონი", TransactionType.INCOME, FinanceCategory.ALIYOT_TORAH),
        OperationPreset("ნედერი (დაპირება)", TransactionType.INCOME, FinanceCategory.NEDARIM_PLEDGES),
        OperationPreset("შემოწირულობა", TransactionType.INCOME, FinanceCategory.NEDARIM_PLEDGES),
        OperationPreset("ბარ-მიცვის შემოწირულობა", TransactionType.INCOME, FinanceCategory.NEDARIM_PLEDGES),
        OperationPreset("ქორწილის შემოწირულობა", TransactionType.INCOME, FinanceCategory.NEDARIM_PLEDGES),
        OperationPreset("ბრით-მილას შემოწირულობა", TransactionType.INCOME, FinanceCategory.NEDARIM_PLEDGES),
        OperationPreset("ჰილულას შემოწირულობა", TransactionType.INCOME, FinanceCategory.NEDARIM_PLEDGES),
        OperationPreset("წლიური საწევრო", TransactionType.INCOME, FinanceCategory.MEMBERSHIP_DUES),
        OperationPreset("თვიური საწევრო", TransactionType.INCOME, FinanceCategory.MEMBERSHIP_DUES),
        OperationPreset("ადგილის (სკამის) საფასური", TransactionType.INCOME, FinanceCategory.MEMBERSHIP_DUES),
        OperationPreset("ცედაკის ყუთის შემოსავალი", TransactionType.INCOME, FinanceCategory.TZEDAKAH_INCOME),
        OperationPreset("მატანოთ ლაევიონიმი (პურიმი)", TransactionType.INCOME, FinanceCategory.TZEDAKAH_INCOME),
        OperationPreset("კიმხა დეფისხა (ფესახი)", TransactionType.INCOME, FinanceCategory.TZEDAKAH_INCOME),
        OperationPreset("ქიდუშის სპონსორობა", TransactionType.INCOME, FinanceCategory.KIDDUSH_SPONSOR),
        OperationPreset("სეუდათ შაბათის სპონსორობა", TransactionType.INCOME, FinanceCategory.KIDDUSH_SPONSOR),
        OperationPreset("დარბაზის ქირა", TransactionType.INCOME, FinanceCategory.KIDDUSH_SPONSOR),
        OperationPreset("იარცეიტის სანთელი", TransactionType.INCOME, FinanceCategory.YAHRZEIT_LIGHTS),
        OperationPreset("მოხსენიება (ჰაზკარა)", TransactionType.INCOME, FinanceCategory.YAHRZEIT_LIGHTS),
        OperationPreset("მემორიალური დაფა", TransactionType.INCOME, FinanceCategory.YAHRZEIT_LIGHTS),
        // Expense
        OperationPreset("რაბინის ხელფასი", TransactionType.EXPENSE, FinanceCategory.SALARIES_RABBIS),
        OperationPreset("ხაზანის ხელფასი", TransactionType.EXPENSE, FinanceCategory.SALARIES_RABBIS),
        OperationPreset("შამაშის ხელფასი", TransactionType.EXPENSE, FinanceCategory.SALARIES_RABBIS),
        OperationPreset("ელექტროენერგია", TransactionType.EXPENSE, FinanceCategory.UTILITIES_COMMUNAL),
        OperationPreset("ბუნებრივი აირი", TransactionType.EXPENSE, FinanceCategory.UTILITIES_COMMUNAL),
        OperationPreset("წყალი", TransactionType.EXPENSE, FinanceCategory.UTILITIES_COMMUNAL),
        OperationPreset("ინტერნეტი და ტელეფონი", TransactionType.EXPENSE, FinanceCategory.UTILITIES_COMMUNAL),
        OperationPreset("სარემონტო სამუშაოები", TransactionType.EXPENSE, FinanceCategory.REPAIRS_RENOVATION),
        OperationPreset("ავეჯი და აღჭურვილობა", TransactionType.EXPENSE, FinanceCategory.REPAIRS_RENOVATION),
        OperationPreset("მიქვეს მომსახურება", TransactionType.EXPENSE, FinanceCategory.REPAIRS_RENOVATION),
        OperationPreset("სეფერ თორის შემოწმება", TransactionType.EXPENSE, FinanceCategory.SEFER_TORAH_BOOKS),
        OperationPreset("სიდურები და წიგნები", TransactionType.EXPENSE, FinanceCategory.SEFER_TORAH_BOOKS),
        OperationPreset("თეფილინი და მეზუზა", TransactionType.EXPENSE, FinanceCategory.SEFER_TORAH_BOOKS),
        OperationPreset("ქიდუშის ხარჯი", TransactionType.EXPENSE, FinanceCategory.EVENTS_CATERING),
        OperationPreset("დღესასწაულის ხარჯი", TransactionType.EXPENSE, FinanceCategory.EVENTS_CATERING),
        OperationPreset("ქოლელის სტიპენდია", TransactionType.EXPENSE, FinanceCategory.KOLLEL_STIPENDS),
        OperationPreset("ცედაკის გაცემა", TransactionType.EXPENSE, FinanceCategory.TZEDAKAH_DISBURSEMENT),
        OperationPreset("სასურსათო კალათები", TransactionType.EXPENSE, FinanceCategory.TZEDAKAH_DISBURSEMENT),
        OperationPreset("დაცვა", TransactionType.EXPENSE, FinanceCategory.SECURITY_CLEANING),
        OperationPreset("დასუფთავება", TransactionType.EXPENSE, FinanceCategory.SECURITY_CLEANING),
        OperationPreset("საკანცელარიო ხარჯი", TransactionType.EXPENSE, FinanceCategory.SECURITY_CLEANING),
        OperationPreset("ბანკის საკომისიო", TransactionType.EXPENSE, FinanceCategory.SECURITY_CLEANING)
    )

    /** Payment methods (Georgian keys; shown translated). */
    val paymentMethodKeys = listOf(
        "ნაღდი", "საბანკო ბარათი", "საბანკო გადარიცხვა", "ჩეკი",
        "ონლაინ გადახდა", "მობილური გადახდა", "ნედერი (დაპირება)", "საქველმოქმედო ფონდი"
    )
    val paymentMethods: List<String> get() = paymentMethodKeys.map { L(it) }

    val eventTypeKeys = listOf(
        "შაბათის ქიდუში", "ბარ-მიცვა", "ბათ-მიცვა", "ბრით-მილა", "ქორწილი", "ჰილულა",
        "მოხსენიება (ჰაზკარა)", "როშ ჰაშანა", "იომ ქიფური", "სუქოთი", "ხანუქა", "პურიმი", "ფესახის სედერი", "შავუოთი", "ლექცია / შიური"
    )
    val eventTypes: List<String> get() = eventTypeKeys.map { L(it) }

    val masechtot = listOf(
        "ברכות (ბრახოთ)", "שבת (შაბათი)", "עירובין (ერუვინი)", "פסחים (ფესახიმ)", "שקלים (შეკალიმ)", "יומא (იომა)",
        "סוכה (სუქა)", "ביצה (ბეიცა)", "ראש השנה (როშ ჰაშანა)", "תענית (თაანით)", "מגילה (მეგილა)", "מועד קטן (მოედ კატან)",
        "חגיגה (ხაგიგა)", "יבמות (იევამოთ)", "כתובות (ქეთუბოთ)", "נדרים (ნედარიმ)", "נזיר (ნაზირ)", "סוטה (სოტა)",
        "גיטין (გიტინ)", "קידושין (კიდუშინ)", "בבא קמא (ბავა კამა)", "בבא מציעא (ბავა მეცია)", "בבא בתרא (ბავა ბათრა)",
        "סנהדרין (სანჰედრინ)", "מכות (მაქოთ)", "שבועות (შევუოთ)", "עבודה זרה (ავოდა ზარა)", "הוריות (ჰორაიოთ)",
        "זבחים (ზევახიმ)", "מנחות (მენახოთ)", "חולין (ხულინ)", "בכורות (ბეხოროთ)", "ערכין (არახინ)", "תמורה (თმურა)",
        "כריתות (ქერითოთ)", "מעילה (მეილა)", "נידה (ნიდა)", "הלכה - שולחן ערוך (ჰალახა)", "חומש ורש״י (ხუმაში)"
    )
}
