package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FinanceCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionType
import com.example.data.security.CosmicVault
import com.example.ui.components.CosmicCryptoBadge
import com.example.ui.components.CosmicGlassCard
import com.example.ui.components.CosmicMetricCard
import com.example.ui.components.AppDropdown
import com.example.ui.components.AmountCurrencyField
import com.example.ui.components.MemberField
import com.example.ui.components.SuggestField
import com.example.ui.settings.*
import com.example.data.model.Catalogs
import com.example.ui.theme.*
import com.example.ui.viewmodel.GabbaiViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancesScreen(
    viewModel: GabbaiViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf<TransactionType?>(null) } // null = All
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf<FinanceCategory?>(null) }
    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    val filteredList = remember(state.transactions, selectedTab, selectedCategoryFilter) {
        state.transactions.filter { tx ->
            val matchTab = selectedTab == null || tx.type == selectedTab
            val matchCategory = selectedCategoryFilter == null || tx.category == selectedCategoryFilter
            matchTab && matchCategory
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
        ) {
            // 1. Finance Overview Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CosmicMetricCard(
                        title = L("შემოსავალი (הכנסות)"),
                        value = signedMoney(state.totalIncome, true),
                        subtitle = L("სულ მიღებული"),
                        icon = Icons.Default.TrendingUp,
                        accentColor = CosmicEmeraldSuccess,
                        modifier = Modifier.weight(1f)
                    )
                    CosmicMetricCard(
                        title = L("გასავალი (הוצאות)"),
                        value = signedMoney(state.totalExpense, false),
                        subtitle = L("ხელფასები, კომუნალური"),
                        icon = Icons.Default.TrendingDown,
                        accentColor = CosmicAlertRose,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2. Compact filter dropdown
            item {
                AppDropdown(label = L("ოპერაციის ტიპი"), value = selectedTab,
                    options = listOf<TransactionType?>(null, TransactionType.INCOME, TransactionType.EXPENSE),
                    optionLabel = { when (it) { null -> Lf("ყველა ({0})", state.transactions.size); TransactionType.INCOME -> L("შემოსავალი (+)"); TransactionType.EXPENSE -> L("გასავალი (-)") } },
                    onSelected = { selectedTab = it }, modifier = Modifier.fillMaxWidth())
            }

            // 3. Category dropdown
            item {
                AppDropdown(
                    label = L("კატეგორია"),
                    value = selectedCategoryFilter,
                    options = listOf<FinanceCategory?>(null) + FinanceCategory.values().toList(),
                    optionLabel = { it?.titleKa ?: L("ყველა კატეგორია") },
                    onSelected = { selectedCategoryFilter = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 4. Transactions List
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = L("სალაროს რეესტრი (יומן פקודות יומן)"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LocalGabbaiPalette.current.text,
                        lineHeight = 25.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    CosmicCryptoBadge(
                        hash = "SHA-256 Ledger Chain",
                        isVerified = true
                    )
                }
            }

            if (filteredList.isEmpty()) {
                item {
                    CosmicGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = CosmicTextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = L("ტრანზაქციები ვერ მოიძებნა"),
                                color = CosmicTextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            items(filteredList, key = { it.id }) { tx ->
                TransactionCardItem(
                    tx = tx,
                    onDelete = { viewModel.deleteTransaction(tx.id) },
                    dateFormatter = dateFormatter
                )
            }
        }

        // Floating Action Button to Add Transaction
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = CosmicStardustCyan,
            contentColor = CosmicOnAccent,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 84.dp, end = 20.dp)
                .testTag("fab_add_transaction")
        ) {
            Icon(Icons.Default.Add, contentDescription = L("დამატება"))
        }

        if (showAddDialog) {
            AddTransactionDialog(
                memberNames = state.members.map { it.fullName },
                onAddMember = { viewModel.quickAddMember(it) },
                onDismiss = { showAddDialog = false },
                onAdd = { title, type, category, amount, member, method, status, note, currency ->
                    viewModel.addTransaction(title, type, category, amount, member, method, status, note, currency)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun TransactionCardItem(
    tx: TransactionEntity,
    onDelete: () -> Unit,
    dateFormatter: SimpleDateFormat
) {
    var expandedNote by remember { mutableStateOf(false) }
    val decryptedNote = remember(tx.encryptedNote) {
        CosmicVault.decrypt(tx.encryptedNote)
    }

    CosmicGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        borderColor = if (tx.type == TransactionType.INCOME) CosmicEmeraldSuccess.copy(alpha = 0.25f) else CosmicAlertRose.copy(alpha = 0.25f)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (tx.type == TransactionType.INCOME) CosmicEmeraldSuccess.copy(alpha = 0.15f)
                                else CosmicAlertRose.copy(alpha = 0.15f)
                            )
                            .border(
                                1.dp,
                                if (tx.type == TransactionType.INCOME) CosmicEmeraldSuccess.copy(alpha = 0.4f)
                                else CosmicAlertRose.copy(alpha = 0.4f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (tx.type == TransactionType.INCOME) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = if (tx.type == TransactionType.INCOME) CosmicEmeraldSuccess else CosmicAlertRose,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tx.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = CosmicTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = nb("${tx.category.titleKa} • ${tx.category.titleHe}"),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = CosmicCelestialGold
                        )
                        if (tx.memberName.isNotEmpty()) {
                            Text(
                                text = Lf("წევრი / პირი: {0}", tx.memberName),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = CosmicTextSecondary
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = signedMoney(tx.amount, tx.type == TransactionType.INCOME, tx.currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (tx.type == TransactionType.INCOME) CosmicEmeraldSuccess else CosmicAlertRose
                    )
                    Text(
                        text = L(tx.paymentMethod),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                        color = CosmicTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = CosmicTextPrimary.copy(alpha = 0.06f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = CosmicTextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = dateFormatter.format(Date(tx.dateMillis)),
                        fontSize = 11.sp,
                        color = CosmicTextMuted
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Hash: ${CosmicVault.formatShortHash(tx.hash)}",
                        fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                        color = CosmicStardustCyan.copy(alpha = 0.8f)
                    )
                    if (decryptedNote.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { expandedNote = !expandedNote },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (expandedNote) Icons.Default.ExpandLess else Icons.Default.Visibility,
                                contentDescription = L("შენიშვნა"),
                                tint = CosmicCelestialGold,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (expandedNote && decryptedNote.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = CosmicDeepSpace.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CosmicStardustCyan.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CosmicStardustCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = Lf("დაშიფრული ჩანაწერი: {0}", decryptedNote),
                            color = CosmicTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    memberNames: List<String>,
    onAddMember: (String) -> Unit,
    onDismiss: () -> Unit,
    onAdd: (
        title: String,
        type: TransactionType,
        category: FinanceCategory,
        amount: Double,
        member: String,
        method: String,
        status: TransactionStatus,
        note: String,
        currency: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.INCOME) }
    var category by remember { mutableStateOf(FinanceCategory.ALIYOT_TORAH) }
    var amountText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(I18n.defaultCurrency) }
    var memberName by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(Catalogs.paymentMethods.first()) }
    var status by remember { mutableStateOf(TransactionStatus.PAID) }
    var note by remember { mutableStateOf("") }
    var showErrors by remember { mutableStateOf(false) }

    val incomeCategories = FinanceCategory.values().take(6)
    val expenseCategories = FinanceCategory.values().drop(6)
    val presets = Catalogs.operations.filter { it.type == type }

    AlertDialog(
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.94f),
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = L("ახალი ფინანსური ოპერაცია"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CosmicTextPrimary
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Income / expense — two solid 3D buttons with guaranteed readable text
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ChoiceTile(
                            text = L("შემოსავალი (+)"),
                            selected = type == TransactionType.INCOME,
                            onClick = { type = TransactionType.INCOME; category = FinanceCategory.ALIYOT_TORAH; title = "" },
                            modifier = Modifier.weight(1f).testTag("btn_type_income"),
                            icon = Icons.Default.TrendingUp
                        )
                        ChoiceTile(
                            text = L("გასავალი (-)"),
                            selected = type == TransactionType.EXPENSE,
                            onClick = { type = TransactionType.EXPENSE; category = FinanceCategory.SALARIES_RABBIS; title = "" },
                            modifier = Modifier.weight(1f).testTag("btn_type_expense"),
                            icon = Icons.Default.TrendingDown
                        )
                    }
                }
                // 2. Operation name: pick from the list or type your own in the same field
                item {
                    SuggestField(
                        label = L("დასახელება"),
                        value = title,
                        onValueChange = { title = it },
                        suggestions = presets.map { it.title },
                        placeholder = L("აირჩიეთ ოპერაცია ან აკრიფეთ"),
                        onSuggestionPicked = { picked -> presets.firstOrNull { it.title == picked }?.let { category = it.category } },
                        modifier = Modifier.testTag("field_title")
                    )
                    if (showErrors && title.isBlank()) Text(L("შეავსეთ დასახელება"), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                // 3. Category (auto-filled from the operation, can be changed)
                item {
                    AppDropdown(
                        label = L("კატეგორია"),
                        value = category,
                        options = if (type == TransactionType.INCOME) incomeCategories else expenseCategories,
                        optionLabel = { it.titleKa },
                        onSelected = { category = it }
                    )
                }
                // 4. Amount + currency
                item {
                    AmountCurrencyField(
                        label = L("თანხა"),
                        amountText = amountText,
                        onAmountChange = { amountText = it },
                        currency = currency,
                        onCurrencyChange = { currency = it }
                    )
                    if (showErrors && (amountText.toDoubleOrNull() ?: 0.0) <= 0.0) Text(L("შეიყვანეთ თანხა"), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                // 5. Member — linked to the members database
                item {
                    MemberField(
                        label = L("წევრი"),
                        value = memberName,
                        onValueChange = { memberName = it },
                        memberNames = memberNames,
                        onAddMember = onAddMember
                    )
                }
                // 6. Payment method
                item {
                    AppDropdown(
                        label = L("გადახდის მეთოდი"),
                        value = paymentMethod,
                        options = Catalogs.paymentMethods,
                        optionLabel = { it },
                        onSelected = { paymentMethod = it }
                    )
                }
                // 7. Status
                item {
                    AppDropdown(
                        label = L("სტატუსი"),
                        value = status,
                        options = TransactionStatus.values().toList(),
                        optionLabel = {
                            when (it) {
                                TransactionStatus.PAID -> L("გადახდილია")
                                TransactionStatus.PENDING -> L("მოლოდინშია")
                                TransactionStatus.PLEDGED -> L("დაპირებულია (ნედერი)")
                            }
                        },
                        onSelected = { status = it }
                    )
                }
                item {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(L("კონფიდენციალური ჩანაწერი"), maxLines = 1) },
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amount > 0) {
                        onAdd(title.trim(), type, category, amount, memberName.trim(), paymentMethod, status, note, currency)
                    } else showErrors = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = CosmicStardustCyan, contentColor = CosmicOnAccent),
                modifier = Modifier.testTag("btn_save_tx")
            ) {
                Text(L("შენახვა"), fontWeight = FontWeight.Bold, maxLines = 1)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(L("გაუქმება"), color = CosmicTextSecondary)
            }
        },
        containerColor = CosmicDarkSurface
    )
}
