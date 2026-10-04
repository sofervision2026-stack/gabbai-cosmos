package com.example.ui.screens

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AliyahEntity
import com.example.data.model.AliyahType
import com.example.data.model.TransactionStatus
import com.example.ui.components.CosmicGlassCard
import com.example.ui.components.CosmicMetricCard
import com.example.ui.components.AppDropdown
import com.example.ui.components.AmountCurrencyField
import com.example.ui.components.MemberField
import com.example.ui.settings.*
import com.example.data.model.Catalogs
import com.example.ui.theme.*
import com.example.ui.viewmodel.GabbaiViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AliyotScreen(
    viewModel: GabbaiViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedParashaFilter by remember { mutableStateOf<String?>(null) }

    val parashot = listOf( // noi18n

        "פרשת בראשית (ბერეშით)",
        "פרשת נח (ნოახი)",
        "פרשת לך לך (ლეხ-ლეხა)",
        "פרשת וירא (ვაიერა)",
        "פרשת חיי שרה (ხაიე სარა)",
        "פרשת תולדות (თოლდოთ)", "פרשת ויצא (ვაიეცე)", "פרשת וישלח (ვაიიშლახ)",
        "פרשת וישב (ვაიეშევ)", "פרשת מקץ (მიკეც)", "פרשת ויגש (ვაიიგაშ)", "פרשת ויחי (ვაიეხი)",
        "פרשת שמות (შემოთ)", "פרשת וארא (ვაერა)", "פרשת בא (ბო)", "פרשת בשלח (ბეშალახ)",
        "פרשת יתרו (ითრო)", "פרשת משפטים (მიშფატიმ)", "פרשת תרומה (თრუმა)", "פרשת תצוה (თეცავე)",
        "פרשת כי תשא (ქი თისა)", "פרשת ויקהל (ვაიაკჰელ)", "פרשת פקודי (ფეკუდეი)", "פרשת ויקרא (ვაიკრა)",
        "פרשת צו (ცავ)", "פרשת שמיני (შემინი)", "פרשת תזריע (თაზრია)", "פרשת מצורע (მეცორა)",
        "פרשת אחרי מות (ახარეი მოთ)", "פרשת קדושים (კედოშიმ)", "פרשת אמור (ემორ)", "פרשת בהר (ბეჰარ)",
        "פרשת בחוקותי (ბეხუკოთაი)", "פרשת במדבר (ბემიდბარ)", "פרשת נשא (ნასო)", "פרשת בהעלותך (ბეჰაალოთხა)",
        "פרשת שלח (შელახ)", "פרשת קרח (კორახ)", "פרשת חקת (ხუკათ)", "פרשת בלק (ბალაკ)",
        "פרשת פינחס (ფინხას)", "פרשת מטות (მატოთ)", "פרשת מסעי (მასეი)", "פרשת דברים (დევარიმ)",
        "פרשת ואתחנן (ვაეთხანან)", "פרשת עקב (ეკევ)", "פרשת ראה (რეე)", "פרשת שופטים (შოფტიმ)",
        "פרשת כי תצא (ქი თეცე)", "פרשת כי תבוא (ქი თავო)", "פרשת נצבים (ნიცავიმ)", "פרשת וילך (ვაიელეხ)",
        "פרשת האזינו (ჰააზინუ)", "פרשת וזאת הברכה (ვეზოთ ჰაბრახა)"
    )

    val filteredAliyot = remember(state.aliyot, selectedParashaFilter) {
        if (selectedParashaFilter == null) state.aliyot
        else state.aliyot.filter { it.parashaName.substringBefore(" (") == selectedParashaFilter!!.substringBefore(" (") }
    }

    val totalAuctionAmount = remember(filteredAliyot) {
        filteredAliyot.sumOf { it.amount }
    }
    val paidAuctionAmount = remember(filteredAliyot) {
        filteredAliyot.filter { it.status == TransactionStatus.PAID }.sumOf { it.amount }
    }
    val pledgedAuctionAmount = remember(filteredAliyot) {
        filteredAliyot.filter { it.status == TransactionStatus.PLEDGED }.sumOf { it.amount }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
        ) {
            // 1. Aliyot Summary Card
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CosmicMetricCard(
                        title = L("ალიების ჯამი (עליות)"),
                        value = money(totalAuctionAmount),
                        subtitle = L("სულ შემოწირულობა"),
                        icon = Icons.Default.MenuBook,
                        accentColor = CosmicCelestialGold,
                        modifier = Modifier.weight(1f)
                    )
                    CosmicMetricCard(
                        title = L("დაპირებული (נדרים)"),
                        value = money(pledgedAuctionAmount),
                        subtitle = L("დასაფარი ნედერები"),
                        icon = Icons.Default.HourglassTop,
                        accentColor = CosmicAlertRose,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2. Parasha dropdown
            item {
                AppDropdown(
                    label = L("კვირის ფარაშა"),
                    value = selectedParashaFilter,
                    options = listOf<String?>(null) + parashot,
                    optionLabel = { it ?: L("ყველა ფარაშა (54)") },
                    onSelected = { selectedParashaFilter = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 3. Aliyot List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = L("სეფერ თორის აუქციონი & პატივები"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CosmicTextPrimary
                    )
                    Text(
                        text = Lf("ჩანაწერი: {0}", filteredAliyot.size),
                        fontSize = 12.sp,
                        color = CosmicTextMuted
                    )
                }
            }

            items(filteredAliyot, key = { it.id }) { aliyah ->
                AliyahCardItem(
                    aliyah = aliyah,
                    onMarkPaid = { viewModel.markAliyahPaid(aliyah) },
                    onDelete = { viewModel.deleteAliyah(aliyah.id) }
                )
            }
        }

        // Add Aliyah Auction FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = CosmicCelestialGold,
            contentColor = CosmicOnAccent,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 84.dp, end = 20.dp)
                .testTag("fab_add_aliyah")
        ) {
            Icon(Icons.Default.Add, contentDescription = L("ალიის დამატება"))
        }

        if (showAddDialog) {
            AddAliyahDialog(
                parashot = parashot,
                initialParasha = selectedParashaFilter,
                memberNames = state.members.map { it.fullName },
                onAddMember = { viewModel.quickAddMember(it) },
                onDismiss = { showAddDialog = false },
                onAdd = { parasha, type, winner, amount, status, currency, method ->
                    viewModel.addAliyah(parasha, type, winner, amount, status, currency, method)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun AliyahCardItem(
    aliyah: AliyahEntity,
    onMarkPaid: () -> Unit,
    onDelete: () -> Unit
) {
    CosmicGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        borderColor = if (aliyah.status == TransactionStatus.PAID) CosmicEmeraldSuccess.copy(alpha = 0.3f) else CosmicCelestialGold.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CosmicCelestialGold.copy(alpha = 0.15f))
                        .border(1.dp, CosmicCelestialGold.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = CosmicCelestialGold,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = aliyah.aliyahType.titleKa, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = CosmicTextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(text = "(${aliyah.aliyahType.titleHe})", fontSize = 11.sp, color = CosmicCelestialGold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)

                    Text(
                        text = Lf("მყიდველი: {0}", aliyah.winnerName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = CosmicTextSecondary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = nb(aliyah.parashaName),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = CosmicTextMuted,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = money(aliyah.amount, aliyah.currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CosmicStardustCyan
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (aliyah.status == TransactionStatus.PAID) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CosmicEmeraldSuccess.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CosmicEmeraldSuccess.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = L("გადახდილია ✓"),
                            fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                            color = CosmicEmeraldSuccess,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = onMarkPaid,
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicCelestialGold.copy(alpha = 0.25f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = L("დაფარვა"),
                            fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                            color = CosmicCelestialGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAliyahDialog(
    parashot: List<String>,
    initialParasha: String?,
    memberNames: List<String>,
    onAddMember: (String) -> Unit,
    onDismiss: () -> Unit,
    onAdd: (
        parasha: String,
        type: AliyahType,
        winner: String,
        amount: Double,
        status: TransactionStatus,
        currency: String,
        paymentMethod: String
    ) -> Unit
) {
    var selectedParasha by remember { mutableStateOf(initialParasha ?: parashot.first()) }
    var selectedType by remember { mutableStateOf(AliyahType.KOHEN) }
    var winnerName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(I18n.defaultCurrency) }
    var isPaid by remember { mutableStateOf(false) }
    var paymentMethod by remember { mutableStateOf(Catalogs.paymentMethods.first()) }
    var showErrors by remember { mutableStateOf(false) }

    AlertDialog(
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.94f),
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = L("სეფერ თორის ალიის / ფასუკის აღრიცხვა"),
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
                item {
                    AppDropdown(
                        label = L("კვირის ფარაშა"),
                        value = selectedParasha,
                        options = parashot,
                        optionLabel = { it },
                        onSelected = { selectedParasha = it }
                    )
                }
                item {
                    AppDropdown(
                        label = L("ალია / ფასუკი"),
                        value = selectedType,
                        options = AliyahType.values().toList(),
                        optionLabel = { "${it.titleKa} — ${it.titleHe}" },
                        displayText = selectedType.titleKa,
                        onSelected = { selectedType = it }
                    )
                }
                item {
                    MemberField(
                        label = L("მყიდველის სახელი"),
                        value = winnerName,
                        onValueChange = { winnerName = it },
                        memberNames = memberNames,
                        onAddMember = onAddMember
                    )
                    if (showErrors && winnerName.isBlank()) Text(L("აირჩიეთ მყიდველი"), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                item {
                    AmountCurrencyField(
                        label = L("შემოწირულობის თანხა"),
                        amountText = amountText,
                        onAmountChange = { amountText = it },
                        currency = currency,
                        onCurrencyChange = { currency = it }
                    )
                    if (showErrors && (amountText.toDoubleOrNull() ?: 0.0) <= 0.0) Text(L("შეიყვანეთ თანხა"), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isPaid,
                            onCheckedChange = { isPaid = it },
                            colors = CheckboxDefaults.colors(checkedColor = CosmicEmeraldSuccess)
                        )
                        Text(L("გადახდილია ახლავე (სალაროში ასახვა)"), color = CosmicTextPrimary, modifier = Modifier.weight(1f))
                    }
                }
                if (isPaid) {
                    item {
                        AppDropdown(
                            label = L("გადახდის მეთოდი"),
                            value = paymentMethod,
                            options = Catalogs.paymentMethods,
                            optionLabel = { it },
                            onSelected = { paymentMethod = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (winnerName.isNotBlank() && amount > 0) {
                        onAdd(
                            selectedParasha,
                            selectedType,
                            winnerName.trim(),
                            amount,
                            if (isPaid) TransactionStatus.PAID else TransactionStatus.PLEDGED,
                            currency,
                            paymentMethod
                        )
                    } else showErrors = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = CosmicCelestialGold, contentColor = CosmicOnAccent)
            ) {
                Text(L("ჩაწერა"), fontWeight = FontWeight.Bold)
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
