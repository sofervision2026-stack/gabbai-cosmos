@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.ui.screens

import com.example.data.model.TransactionType
import com.example.data.model.FinanceCategory
import com.example.data.model.TransactionStatus

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MemberCompliance
import com.example.data.model.MemberEntity
import com.example.data.model.MemberRole
import com.example.data.model.MemberStatus
import com.example.ui.components.CosmicGlassCard
import com.example.ui.components.CosmicMetricCard
import com.example.ui.components.AppDropdown
import com.example.ui.components.SheetsExportDialog
import com.example.ui.theme.*
import com.example.ui.settings.*
import com.example.ui.components.AmountCurrencyField
import com.example.ui.viewmodel.GabbaiViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(
    viewModel: GabbaiViewModel,
    modifier: Modifier = Modifier
) {
    var showMiSheberachDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    var selectedFilterTab by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    
    var showAddDialog by remember { mutableStateOf(false) }
    var memberToEdit by remember { mutableStateOf<MemberEntity?>(null) }
    var memberToMessage by remember { mutableStateOf<MemberEntity?>(null) }
    var showSheetsExportDialog by remember { mutableStateOf(false) }
    var showComplianceSettingsDialog by remember { mutableStateOf(false) }

    var redListDaysThreshold by remember { mutableStateOf(60) }
    var redListAmountThreshold by remember { mutableStateOf(300.0) }

    val filteredMembers = remember(state.members, selectedFilterTab, searchQuery, redListDaysThreshold, redListAmountThreshold) {
        state.members.filter { member ->
            val matchesSearch = member.fullName.contains(searchQuery, ignoreCase = true) ||
                    member.hebrewName.contains(searchQuery, ignoreCase = true) ||
                    member.phone.contains(searchQuery) ||
                    member.seatNumber.contains(searchQuery, ignoreCase = true) ||
                    member.blackListReason.contains(searchQuery, ignoreCase = true)

            val isRedListCalculated = member.complianceCategory == MemberCompliance.RED_LIST ||
                    (member.outstandingDebt >= redListAmountThreshold && !member.duesPaid)
            val isBlackList = member.complianceCategory == MemberCompliance.BLACK_LIST

            val matchesFilter = when (selectedFilterTab) {
                "ALL" -> true
                "ACTIVE" -> member.status == MemberStatus.ACTIVE
                "RED_LIST" -> isRedListCalculated
                "BLACK_LIST" -> isBlackList
                "DEBTORS" -> member.outstandingDebt > 0 || !member.duesPaid
                "PAID_RECENT" -> member.lastPaymentAmount > 0
                "ISRAEL" -> member.status == MemberStatus.ALIYAH_ISRAEL
                "DECEASED" -> member.status == MemberStatus.DECEASED
                "STAFF" -> member.role != MemberRole.CONGREGANT
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    val totalDuesCollected = remember(state.members) {
        state.members.filter { it.duesPaid }.sumOf { it.annualFeeAmount }
    }
    val totalOutstandingDebt = remember(state.members) {
        state.members.sumOf { it.outstandingDebt }
    }
    val redListCount = remember(state.members, redListDaysThreshold, redListAmountThreshold) {
        state.members.count { it.complianceCategory == MemberCompliance.RED_LIST || (it.outstandingDebt >= redListAmountThreshold && !it.duesPaid) }
    }
    val blackListCount = remember(state.members) {
        state.members.count { it.complianceCategory == MemberCompliance.BLACK_LIST }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
        ) {
            item {
    QuickNedarimCard(
        memberNames = state.members.map { it.fullName },
        onQuickPledgeAdded = { name, amount, note ->
            viewModel.addTransaction(
                title = "ნედერი: $note",
                type = TransactionType.INCOME,
                category = FinanceCategory.ALIYOT_TORAH,
                amount = amount,
                memberName = name,
                paymentMethod = "დაპირება",
                status = TransactionStatus.PLEDGED,
                note = note,
                currency = I18n.defaultCurrency
            )
        }
    )
            }
            
            item {
    ShabbatTimesCard()
            }
            item {
    YahrzeitCard(members = state.members)
            }
            
            item {
    CosmicFinanceSummaryCard(
        totalCollected = totalDuesCollected,
        totalDebt = totalOutstandingDebt,
        currency = I18n.defaultCurrency
    )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CosmicMetricCard(
                        title = L("ჯამაათის რეესტრი"),
                        value = Lf("{0} წევრი", state.members.size),
                        subtitle = Lf("აქტიური: {0}", state.members.count { it.status == MemberStatus.ACTIVE }),
                        icon = Icons.Default.Groups,
                        accentColor = CosmicAuroraBlue,
                        modifier = Modifier.weight(1f)
                    )
                    CosmicMetricCard(
                        title = L("სია / მონიტორინგი"),
                        value = "🔴 $redListCount • ⚫ $blackListCount",
                        subtitle = Lf("ვალი: {0}", money(totalOutstandingDebt)),
                        icon = Icons.Default.WarningAmber,
                        accentColor = CosmicAlertRose,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showSheetsExportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicEmeraldSuccess),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp), tint = CosmicDeepSpace)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(L("Sheets / Excel ექსპორტი"), color = CosmicOnAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    }

                    OutlinedButton(
                        onClick = { showComplianceSettingsDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmicCelestialGold),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(L("სიის წესები"), fontSize = 11.sp)
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(L("ძიება: სახელი, ებრაული, სკამი, მიზეზი, ტელეფონი...")) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CosmicTextSecondary) },
                                        trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = L("გასუფთავება"), tint = CosmicTextMuted)
                                }
                            }
                            IconButton(onClick = { 
                                Toast.makeText(context, L("ხმოვანი ძებნა აქტიურია..."), Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.Mic, contentDescription = L("ხმოვანი ძებნა"), tint = CosmicCelestialGold)
                            }
                        }
                    },

                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_members_field"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CosmicAuroraBlue,
                        unfocusedBorderColor = CosmicBorderGlow
                    )
                )
            }

            item {
                val filters = listOf(
                    "ALL" to Lf("ყველა ({0})", state.members.size),
                    "ACTIVE" to L("აქტიური / Активные"),
                    "RED_LIST" to Lf("წითელი სია / Красный список ({0})", redListCount),
                    "BLACK_LIST" to Lf("შავი სია / Чёрный список ({0})", blackListCount),
                    "DEBTORS" to L("მოვალეები / Должники"),
                    "PAID_RECENT" to L("გადახდილი / Оплатившие"),
                    "ISRAEL" to L("ისრაელში / В Израиле"),
                    "DECEASED" to L("გარდაცვლილი / Умершие"),
                    "STAFF" to L("პერსონალი / Персонал")
                )
                AppDropdown(
                    label = L("ფილტრი"),
                    value = selectedFilterTab,
                    options = filters.map { it.first },
                    optionLabel = { key -> filters.first { it.first == key }.second },
                    onSelected = { selectedFilterTab = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = L("ჯამაათის წევრთა სია (קהילת בית הכנסת)"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LocalGabbaiPalette.current.text,
                        lineHeight = 25.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = Lf("ნაჩვენებია: {0}", filteredMembers.size),
                        fontSize = 12.sp,
                        color = LocalGabbaiPalette.current.mutedText,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            items(filteredMembers, key = { it.id }) { member ->
                MemberCardItem(
                    member = member,
                    dateFormatter = dateFormatter,
                    onToggleDues = { viewModel.toggleMemberDues(member) },
                    onEdit = { memberToEdit = member },
                    onMessage = { memberToMessage = member },
                    onDelete = { viewModel.deleteMember(member.id) }
                )
            }
        }
        OutlinedButton(
    onClick = { showMiSheberachDialog = true },
    colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmicCelestialGold),
    shape = RoundedCornerShape(10.dp)
) {
    Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(16.dp))
    Spacer(modifier = Modifier.width(4.dp))
    Text(L("მიშიბერახი"), fontSize = 11.sp)
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = CosmicAuroraBlue,
            contentColor = CosmicOnAccent,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 84.dp, end = 20.dp)
                .testTag("fab_add_member")
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = L("ჯამაათის წევრის დამატება"))
        }

        if (showAddDialog) {
            MemberFormDialog(
                title = L("ჯამაათის ახალი წევრის რეგისტრაცია"),
                initialMember = null,
                onDismiss = { showAddDialog = false },
                onSave = { updated ->
                    viewModel.addMemberEntity(updated)
                    showAddDialog = false
                }
            )
        }

        memberToEdit?.let { editing ->
            MemberFormDialog(
                title = Lf("მონაცემების რედაქტირება: {0}", editing.fullName),
                initialMember = editing,
                onDismiss = { memberToEdit = null },
                onSave = { updated ->
                    viewModel.updateMember(updated)
                    memberToEdit = null
                }
            )
        }

        memberToMessage?.let { target ->
            DirectMessageDialog(
                member = target,
                onDismiss = { memberToMessage = null }
            )
        }
        if (showMiSheberachDialog) {
    MiSheberachDialog(
        members = state.members,
        onDismiss = { showMiSheberachDialog = false }
    )
        }
        

        if (showSheetsExportDialog) {
            SheetsExportDialog(
                viewModel = viewModel,
                onDismiss = { showSheetsExportDialog = false }
            )
        }

        if (showComplianceSettingsDialog) {
            ComplianceSettingsDialog(
                currentDaysThreshold = redListDaysThreshold,
                currentAmountThreshold = redListAmountThreshold,
                onDismiss = { showComplianceSettingsDialog = false },
                onSave = { days, amount ->
                    redListDaysThreshold = days
                    redListAmountThreshold = amount
                    showComplianceSettingsDialog = false
                    Toast.makeText(context, L("წითელი და შავი სიის წესები განახლდა!"), Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemberCardItem(
    member: MemberEntity,
    dateFormatter: SimpleDateFormat,
    onToggleDues: () -> Unit,
    onEdit: () -> Unit,
    onMessage: () -> Unit,
    onDelete: () -> Unit
) {
    val isKohen = member.tribalStatus.contains("כהן") || member.tribalStatus.contains("ქოჰენ") || member.tribalStatus.contains("კოენ")
    val isLevi = member.tribalStatus.contains("לוי") || member.tribalStatus.contains("ლევი")
    val isBlackList = member.complianceCategory == MemberCompliance.BLACK_LIST
    val isRedList = member.complianceCategory == MemberCompliance.RED_LIST || member.outstandingDebt >= 300.0 && !member.duesPaid
    val accent = when {
        isBlackList || isRedList -> CosmicAlertRose
        member.status == MemberStatus.ALIYAH_ISRAEL -> CosmicStardustCyan
        member.status == MemberStatus.DECEASED -> CosmicTextMuted
        member.role == MemberRole.SENIOR_RABBI || isKohen -> CosmicCelestialGold
        else -> CosmicAuroraBlue
    }

    CosmicGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        borderColor = accent.copy(alpha = 0.55f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .background(accent.copy(alpha = 0.14f))
                        .border(1.5.dp, accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isBlackList -> Icons.Default.Block
                            isRedList -> Icons.Default.Warning
                            member.status == MemberStatus.ALIYAH_ISRAEL -> Icons.Default.FlightTakeoff
                            member.status == MemberStatus.DECEASED -> Icons.Default.LocalFireDepartment
                            member.role == MemberRole.SENIOR_RABBI -> Icons.Default.AutoStories
                            member.role == MemberRole.GABBAI -> Icons.Default.AdminPanelSettings
                            member.role == MemberRole.CHAZZAN -> Icons.Default.MusicNote
                            member.role == MemberRole.SHAMASH -> Icons.Default.Key
                            else -> if (isKohen) Icons.Default.FrontHand else Icons.Default.Person
                        },
                        contentDescription = null, tint = accent, modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.fullName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CosmicTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (member.hebrewName.isNotEmpty()) {
                        Text(
                            text = "שם לתורה: ${member.hebrewName}",
                            fontSize = 12.sp,
                            color = CosmicTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onMessage, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Default.Chat, contentDescription = L("შეტყობინება"), tint = CosmicStardustCyan, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = L("რედაქტირება"), tint = CosmicAuroraBlue, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = L("წაშლა"), tint = CosmicAlertRose, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MemberBadge(member.status.titleKa, when (member.status) {
                    MemberStatus.ACTIVE -> CosmicEmeraldSuccess
                    MemberStatus.ALIYAH_ISRAEL -> CosmicStardustCyan
                    else -> CosmicTextSecondary
                })
                MemberBadge(member.role.titleKa, CosmicAuroraBlue)
                if (isKohen) MemberBadge("כהן", CosmicCelestialGold)
                if (isLevi) MemberBadge("לוי", CosmicStardustCyan)
                if (member.seatNumber.isNotEmpty()) MemberBadge(Lf("ადგილი: {0}", member.seatNumber), CosmicEmeraldSuccess)
            }

            if (isBlackList || isRedList) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CosmicAlertRose.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CosmicAlertRose.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (isBlackList) Icons.Default.Block else Icons.Default.Warning, null, tint = CosmicAlertRose, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isBlackList) Lf("შავი სია: {0}", member.blackListReason.ifEmpty { L("სანქცირებული / გადახდაზე უარი") })
                            else Lf("წითელი სია: {0}", member.blackListReason.ifEmpty { Lf("ვადაგადაცილებული ვალი ({0})", money(member.outstandingDebt, member.currency)) }),
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CosmicAlertRose,
                            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = CosmicTextPrimary.copy(alpha = 0.10f))
            Spacer(Modifier.height(8.dp))

            if (member.phone.isNotEmpty()) MemberInfoLine(Icons.Default.Phone, member.phone, CosmicTextSecondary)
            if (member.lastPaymentAmount > 0) {
                val dateStr = if (member.lastPaymentDateMillis > 0) dateFormatter.format(Date(member.lastPaymentDateMillis)) else "-"
                MemberInfoLine(Icons.Default.Payment, Lf("ბოლო გადახდა: {0} ({1})", com.example.ui.settings.money(member.lastPaymentAmount), dateStr), CosmicEmeraldSuccess)
            }
            if (member.yahrzeitDate.isNotEmpty()) MemberInfoLine(Icons.Default.LocalFireDepartment, Lf("იარცეიტი: {0}", member.yahrzeitDate), CosmicCelestialGold)
            if (member.outstandingDebt > 0) MemberInfoLine(Icons.Default.AccountBalanceWallet, Lf("ალიების ვალი: {0}", money(member.outstandingDebt, member.currency)), CosmicAlertRose)

            Spacer(Modifier.height(8.dp))
            val duesColor = if (member.duesPaid) CosmicEmeraldSuccess else CosmicAlertRose
            OutlinedButton(
                onClick = onToggleDues,
                modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, duesColor.copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = duesColor.copy(alpha = 0.10f))
            ) {
                Text(
                    text = if (member.duesPaid) L("საწევრო: გადახდილია ✓") else L("საწევრო: ვალი — მონიშნე გადახდილად"),
                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = duesColor,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MemberBadge(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.13f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f))) {
        Text(
            text = text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color,
            maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun MemberInfoLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(text = text, fontSize = 13.sp, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberFormDialog(
    title: String,
    initialMember: MemberEntity?,
    onDismiss: () -> Unit,
    onSave: (MemberEntity) -> Unit
) {
    var fullName by remember { mutableStateOf(initialMember?.fullName ?: "") }
    var hebrewName by remember { mutableStateOf(initialMember?.hebrewName ?: "") }
    var phone by remember { mutableStateOf(initialMember?.phone ?: "") }
    var role by remember { mutableStateOf(initialMember?.role ?: MemberRole.CONGREGANT) }
    var tribalStatus by remember { mutableStateOf(initialMember?.tribalStatus ?: "ישראל (ისრაელი)") }
    
    var status by remember { mutableStateOf(initialMember?.status ?: MemberStatus.ACTIVE) }
    var complianceCategory by remember { mutableStateOf(initialMember?.complianceCategory ?: MemberCompliance.NORMAL) }
    var blackListReason by remember { mutableStateOf(initialMember?.blackListReason ?: "") }

    var seatNumber by remember { mutableStateOf(initialMember?.seatNumber ?: "") }
    var familyInfo by remember { mutableStateOf(initialMember?.familyInfo ?: "") }
    var address by remember { mutableStateOf(initialMember?.address ?: "") }
    var annualFeeText by remember { mutableStateOf((initialMember?.annualFeeAmount ?: 1200.0).toInt().toString()) }
    var duesPaid by remember { mutableStateOf(initialMember?.duesPaid ?: true) }
    var debtText by remember { mutableStateOf((initialMember?.outstandingDebt ?: 0.0).toInt().toString()) }
    var lastPaymentAmountText by remember { mutableStateOf((initialMember?.lastPaymentAmount ?: 300.0).toInt().toString()) }
    var yahrzeit by remember { mutableStateOf(initialMember?.yahrzeitDate ?: "") }
    var currency by remember { mutableStateOf(initialMember?.currency ?: I18n.defaultCurrency) }

    val tribalOptions = listOf("ישראל (ისრაელი)", "כהן (ქოჰენი)", "לוי (ლევი)")

    val blackListReasonsPresets = listOf(
        L("უარი განაცხადა ნედერის გადახდაზე"),
        L("გადახდისუუნარო (ეკონომიკური კოლაფსი)"),
        L("გაბაის სანქცია - აუქციონიდან დაბლოკილი"),
        L("ბეით დინის (სასამართლოს) გადაწყვეტილება"),
        L("ვადაგადაცილებული ვალი > 90 დღე")
    )

    AlertDialog(
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .heightIn(max = 680.dp),
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CosmicTextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text(L("სრული სახელი და გვარი")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = hebrewName,
                    onValueChange = { hebrewName = it },
                    label = { Text(L("ებრაული სახელი თორისთვის (שם לתורה)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                AppDropdown(
                    label = L("ჯამაათის წევრის სტატუსი"),
                    value = MemberStatus.values().first(),
                    options = MemberStatus.values().toList(),
                    optionLabel = { st -> "${st.titleKa} (${st.titleHe})" },
                    onSelected = { st -> status = st },
                    displayText = status.titleKa,
                    modifier = Modifier.fillMaxWidth()
                )

                AppDropdown(
                    label = L("სიის კატეგორია (წითელი/შავი სია)"),
                    value = MemberCompliance.values().first(),
                    options = MemberCompliance.values().toList(),
                    optionLabel = { comp -> comp.titleKa },
                    onSelected = { comp -> complianceCategory = comp },
                    displayText = complianceCategory.titleKa,
                    modifier = Modifier.fillMaxWidth()
                )

                if (complianceCategory != MemberCompliance.NORMAL) {
                    Column {
                        OutlinedTextField(
                            value = blackListReason,
                            onValueChange = { blackListReason = it },
                            label = { Text(L("წითელ/შავ სიაში შეყვანის მიზეზი")) },
                            placeholder = { Text(L("მაგ. უარი განაცხადა გადახდაზე...")) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(blackListReasonsPresets) { preset ->
                                SuggestionChip(
                                    onClick = { blackListReason = preset },
                                    label = { Text(preset, fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                                )
                            }
                        }
                    }
                }

                AppDropdown(
                    label = L("სულიერი ხარისხი (თორისთვის)"),
                    value = tribalOptions.first(),
                    options = tribalOptions.toList(),
                    optionLabel = { opt -> opt },
                    onSelected = { opt -> tribalStatus = opt },
                    displayText = tribalStatus,
                    modifier = Modifier.fillMaxWidth()
                )

                AppDropdown(
                    label = L("როლი სინაგოგაში"),
                    value = MemberRole.values().first(),
                    options = MemberRole.values().toList(),
                    optionLabel = { r -> "${r.titleKa} - ${r.titleHe}" },
                    onSelected = { r -> role = r },
                    displayText = "${role.titleKa} (${role.titleHe})",
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(L("ტელეფონი")) },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = seatNumber,
                        onValueChange = { seatNumber = it },
                        label = { Text(L("სკამი / ადგილი")) },
                        placeholder = { Text("A-12") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                OutlinedTextField(
                    value = familyInfo,
                    onValueChange = { familyInfo = it },
                    label = { Text(L("ოჯახური მდგომარეობა (მეუღლე, შვილები)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(L("საცხოვრებელი მისამართი")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AmountCurrencyField(L("წლიური საწევრო"), annualFeeText, { annualFeeText = it }, currency, { currency = it })
                    AmountCurrencyField(L("ალიების / ნედერის ვალი"), debtText, { debtText = it }, currency, { currency = it })
                }

                AmountCurrencyField(L("ბოლო გადახდილი თანხა"), lastPaymentAmountText, { lastPaymentAmountText = it }, currency, { currency = it })

                OutlinedTextField(
                    value = yahrzeit,
                    onValueChange = { yahrzeit = it },
                    label = { Text(L("იარცეიტი (მოხსენიების თარიღი)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fee = annualFeeText.toDoubleOrNull() ?: 1200.0
                    val debt = debtText.toDoubleOrNull() ?: 0.0
                    val lastPaid = lastPaymentAmountText.toDoubleOrNull() ?: 0.0
                    if (fullName.isNotEmpty()) {
                        val result = initialMember?.copy(
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
                            annualFeeAmount = fee,
                            duesPaid = duesPaid,
                            outstandingDebt = debt,
                            lastPaymentAmount = lastPaid,
                            lastPaymentDateMillis = if (lastPaid > 0) System.currentTimeMillis() else (initialMember.lastPaymentDateMillis),
                            yahrzeitDate = yahrzeit,
                            currency = currency
                        ) ?: MemberEntity(
                            currency = currency,
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
                            annualFeeAmount = fee,
                            duesPaid = duesPaid,
                            outstandingDebt = debt,
                            lastPaymentAmount = lastPaid,
                            lastPaymentDateMillis = if (lastPaid > 0) System.currentTimeMillis() else 0L,
                            yahrzeitDate = yahrzeit
                        )
                        onSave(result)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CosmicAuroraBlue, contentColor = CosmicOnAccent)
            ) {
                Text(L("შენახვა"), fontWeight = FontWeight.Bold)
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

@Composable
fun ComplianceSettingsDialog(
    currentDaysThreshold: Int,
    currentAmountThreshold: Double,
    onDismiss: () -> Unit,
    onSave: (days: Int, amount: Double) -> Unit
) {
    var daysText by remember { mutableStateOf(currentDaysThreshold.toString()) }
    var amountText by remember { mutableStateOf(currentAmountThreshold.toInt().toString()) }

    AlertDialog(
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .heightIn(max = 680.dp),
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = CosmicCelestialGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = L("წითელი & შავი სიის წესები"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CosmicTextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = L("დააკონფიგურირეთ პირობები, რომლითაც წევრი ავტომატურად გადადის წითელ სიაში (ვადაგადაცილება):"),
                    fontSize = 11.sp,
                    color = CosmicTextSecondary
                )

                OutlinedTextField(
                    value = daysText,
                    onValueChange = { daysText = it },
                    label = { Text(L("ვადაგადაცილების ზღვარი (დღეები)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(L("მინიმალური ვალის ზღვარი (") + Currencies.symbol(I18n.defaultCurrency) + ")") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CosmicDarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CosmicBorderGlow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = L("შავი სიის პირობები (მკაცრი სანქცია):"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CosmicAlertRose
                        )
                        Text(
                            text = L("• წევრმა უარი განაცხადა გადახდაზე\n• ცნობილი გახდა, რომ გადახდისუუნაროა\n• გაბაის სანქციით დაბლოკილია აუქციონზე გამოძახება"),
                            fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                            color = CosmicTextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val days = daysText.toIntOrNull() ?: 60
                    val amount = amountText.toDoubleOrNull() ?: 300.0
                    onSave(days, amount)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CosmicCelestialGold)
            ) {
                Text(L("შენახვა"), color = CosmicOnAccent, fontWeight = FontWeight.Bold)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectMessageDialog(
    member: MemberEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var messageText by remember {
        mutableStateOf(Lf("შალომ {0}! მოგესალმებით სინაგოგის გაბაისგან.", member.fullName))
    }
    var templateDropdownExpanded by remember { mutableStateOf(false) }

    val cleanPhone = member.phone.replace("[^0-9+]".toRegex(), "")

    val templates = listOf(
        L("საწევრო / ნედერების შეხსენება") to Lf("შალომ {0}! შეგახსენებთ სინაგოგის წლიური საწევროს/ნედერის შესახებ. თქვენი მიმდინარე ბალანსია: {1}.", member.fullName, money(member.outstandingDebt, member.currency)),
        L("სეფერ თორის ალიაზე მიწვევა") to Lf("შალომ {0}! გელოდებით ამ შაბათის ლოცვაზე. თქვენ გამოძახებული ბრძანდებით სეფერ თორაზე ({1}).", member.fullName, member.tribalStatus),
        L("იარცეიტი (მოხსენიების დღე)") to Lf("შალომ {0}! მოახლოვდა იარცეიტის დღე ({1}). სინაგოგაში მოხდება კადიშის წაკითხვა და ნერ ნეშამას ანთება.", member.fullName, member.yahrzeitDate),
        L("შაბათის ქიდუში") to Lf("შალომ {0}! გეპატიჟებით ამ შაბათს სინაგოგის საზეიმო ქიდუშზე. გელოდებით ოჯახთან ერთად.", member.fullName)
    )

    AlertDialog(
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .heightIn(max = 680.dp),
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Send, contentDescription = null, tint = CosmicAuroraBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = L("შეტყობინების გაგზავნა"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CosmicTextPrimary
                    )
                    Text(
                        text = "${member.fullName} (${member.phone})",
                        fontSize = 11.sp,
                        color = CosmicTextSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppDropdown(
                    label = L("სწრაფი შაბლონები"),
                    value = templates.first(),
                    options = templates.toList(),
                    optionLabel = { (title, body) -> title },
                    onSelected = { (title, body) -> messageText = body },
                    displayText = L("აირჩიეთ შაბლონი..."),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text(L("შეტყობინების ტექსტი")) },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (cleanPhone.isEmpty()) {
                                Toast.makeText(context, L("ტელეფონის ნომერი მითითებული არ არის"), Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            try {
                                val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(messageText)}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                                onDismiss()
                            } catch (e: Exception) {
                                Toast.makeText(context, L("WhatsApp ვერ მოიძებნა"), Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("WhatsApp", color = CosmicTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            if (cleanPhone.isEmpty()) {
                                Toast.makeText(context, L("ტელეფონის ნომერი მითითებული არ არის"), Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            try {
                                val uri = Uri.parse("sms:$cleanPhone")
                                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                    putExtra("sms_body", messageText)
                                }
                                context.startActivity(intent)
                                onDismiss()
                            } catch (e: Exception) {
                                Toast.makeText(context, L("SMS აპლიკაცია ვერ გაიხსნა"), Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicAuroraBlue),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("SMS", color = CosmicOnAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            if (cleanPhone.isEmpty()) {
                                Toast.makeText(context, L("ტელეფონის ნომერი მითითებული არ არის"), Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
                                context.startActivity(intent)
                                onDismiss()
                            } catch (e: Exception) {
                                Toast.makeText(context, L("ზარის განხორციელება ვერ მოხერხდა"), Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicCelestialGold),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(L("დარეკვა"), color = CosmicOnAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(L("დახურვა"), color = CosmicTextSecondary)
            }
        },
        containerColor = CosmicDarkSurface
    )
}
/**
 * Mi Sheberach Generator Dialog for Synagogue Readers & Gabbai
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiSheberachDialog(
    members: List<MemberEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf("HEALTH") } // HEALTH or DONATION

    val targetMembers = remember(members, selectedType) {
        if (selectedType == "HEALTH") {
            members.filter { it.yahrzeitDate.isNotEmpty() || it.outstandingDebt > 0 }
        } else {
            members.filter { it.lastPaymentAmount > 0 || it.duesPaid }
        }
    }

    val generatedText = remember(targetMembers, selectedType) {
        val sb = StringBuilder()
        if (selectedType == "HEALTH") {
            sb.append("מי שהברך אבותינו… מי שהברך אחוזתינו הקדושה:\n")
            targetMembers.take(5).forEach { m ->
                val nameForTorah = m.hebrewName.ifEmpty { m.fullName }
                sb.append("• רפואה שלמה ל- $nameForTorah (${m.tribalStatus})\n")
            }
        } else {
            sb.append("מי שהברך — הודა და ברכה לקהילות הקודש:\n")
            targetMembers.take(5).forEach { m ->
                sb.append("• תודה רבה ל- ${m.fullName} על תרומתו לבית הכנסת\n")
            }
        }
        sb.toString()
    }

    AlertDialog(
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .heightIn(max = 680.dp),
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoStories, contentDescription = null, tint = CosmicCelestialGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = L("მიშიბერახის (Mi Sheberach) გენერატორი"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CosmicTextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { selectedType = "HEALTH" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == "HEALTH") CosmicAuroraBlue else CosmicDarkSurface
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(L("ჯანმრთელობა (רפואה)"), fontSize = 11.sp)
                    }
                    Button(
                        onClick = { selectedType = "DONATION" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == "DONATION") CosmicAuroraBlue else CosmicDarkSurface
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(L("შემწირველები (נדבה)"), fontSize = 11.sp)
                    }
                }

                OutlinedTextField(
                    value = generatedText,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(L("გენერირებული ლოცვის ტექსტი თორისთვის")) },
                    minLines = 5,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("MiSheberach", generatedText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, L("ტექსტი დაკოპირდა ბუფერში!"), Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicEmeraldSuccess),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(L("კოპირება"), fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, generatedText)
                            }
                            context.startActivity(Intent.createChooser(intent, L("გაზიარება")))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicCelestialGold),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(L("გაზიარება"), fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(L("დახურვა"), color = CosmicTextSecondary)
            }
        },
        containerColor = CosmicDarkSurface
    )
}
/**
 * Financial Summary Card for Gabbai Cosmos
 */
@Composable
fun CosmicFinanceSummaryCard(
    totalCollected: Double,
    totalDebt: Double,
    currency: String,
    modifier: Modifier = Modifier
) {
    CosmicGlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        borderColor = CosmicCelestialGold.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = CosmicCelestialGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = L("ფინანსური ბალანსი და შემოსავლები"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CosmicTextPrimary
                    )
                }
            }

            HorizontalDivider(color = CosmicTextPrimary.copy(alpha = 0.1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // შეგროვებული თანხა
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = L("შეგროვებული საწევრო"),
                        fontSize = 11.sp,
                        color = CosmicTextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = money(totalCollected, currency),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CosmicEmeraldSuccess
                    )
                }

                // ჯამური ვალი
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = L("სულ გამოსათხოვი ვალი"),
                        fontSize = 11.sp,
                        color = CosmicTextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = money(totalDebt, currency),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CosmicAlertRose
                    )
                }
            }
        }
    }
}
/**
 * Voice Command Parser for Gabbai Cosmos Hands-Free Navigation
 */
data class GabbaiVoiceCommand(
    val action: String,
    val query: String?,
    val responseMessage: String
)

object VoiceCommandParser {
    fun parseCommand(transcript: String): GabbaiVoiceCommand {
        val t = transcript.lowercase().trim()
        return when {
            t.contains("წევრები") || t.contains("members") -> 
                GabbaiVoiceCommand("NAV_MEMBERS", null, "გადავდივარ წევრების რეესტრში")
            
            t.contains("ფინანსები") || t.contains("finance") || t.contains("ვალი") -> 
                GabbaiVoiceCommand("NAV_FINANCE", null, "ფინანსური ბალანსი და მონიტორინგი")
            
            t.contains("ძებნა") || t.contains("მოძებნე") || t.contains("search") -> {
                val searchQuery = t.replace("ძებნა", "")
                    .replace("მოძებნე", "")
                    .replace("search", "")
                    .trim()
                GabbaiVoiceCommand("SEARCH_MEMBER", searchQuery, "ვეძებ წევრს: $searchQuery")
            }
            
            else -> GabbaiVoiceCommand("UNKNOWN", null, "ბრძანება ვერ გაირკვა, სცადეთ თავიდან")
        }
        /**
 * Comprehensive Zmanim, Calendar & Daily Times Card with Sharing
 */
@Composable
fun ZmanimCalendarCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentDateStr = remember {
        val sdf = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
        sdf.format(java.util.Date())
    }

    // ზმანების/დღის დროების ტექსტი გასაზიარებლად
    val zmanimText = remember {
        """
        📅 სინაგოგის დღის დროები და კალენდარი ($currentDateStr):
        🌅 ატირება (Sunrise): 06:15
        ☀️ მზის ჩასვლა (Sunset): 18:45
        🕯️ სანთლების ანთება: 18:24
        🍷 შაბათის დასასრული (Havdalah): 19:35
        """.trimIndent()
    }

    CosmicGlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        borderColor = CosmicCelestialGold.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = CosmicCelestialGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = L("დღის დროები & კალენდარი"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CosmicTextPrimary
                    )
                }

                // გაზიარების ღილაკი გაბაისთვის
                IconButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, zmanimText)
                        }
                        context.startActivity(Intent.createChooser(intent, L("დროების გაზიარება")))
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = L("გაზიარება"),
                        tint = CosmicStardustCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(color = CosmicTextPrimary.copy(alpha = 0.1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = L("თარიღი"), fontSize = 11.sp, color = CosmicTextSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = currentDateStr, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CosmicTextPrimary)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = L("მზის ჩასვლა"), fontSize = 11.sp, color = CosmicTextSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "18:45", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CosmicStardustCyan)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = L("სანთლები"), fontSize = 11.sp, color = CosmicTextSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "18:24", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CosmicCelestialGold)
                }
            }
        }
    }
}

    }
}
/**
 * Yahrzeit & Memorial Days Upcoming Card for Gabbai Cosmos
 */
@Composable
fun YahrzeitCard(
    members: List<MemberEntity>,
    modifier: Modifier = Modifier
) {
    val upcomingYahrzeits = remember(members) {
        members.filter { it.yahrzeitDate.isNotEmpty() }.take(5)
    }

    CosmicGlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        borderColor = CosmicCelestialGold.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.EventRepeat,
                    contentDescription = null,
                    tint = CosmicCelestialGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = L("უახლოესი იარצייטები (יארצייט)"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = CosmicTextPrimary
                )
            }

            HorizontalDivider(color = CosmicTextPrimary.copy(alpha = 0.1f))

            if (upcomingYahrzeits.isEmpty()) {
                Text(
                    text = L("იარצიტის ჩანაწერები არ მოიძებნა"),
                    fontSize = 12.sp,
                    color = CosmicTextSecondary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    upcomingYahrzeits.forEach { member ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = member.fullName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CosmicTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (member.hebrewName.isNotEmpty()) {
                                    Text(
                                        text = member.hebrewName,
                                        fontSize = 11.sp,
                                        color = CosmicCelestialGold,
                                        maxLines = 1
                                    )
                                }
                            }
                            Surface(
                                color = CosmicCelestialGold.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = member.yahrzeitDate,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CosmicCelestialGold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
/**
 * Quick Nedarim (Pledge) Fast Entry Card for Gabbai Cosmos
 */
@Composable
fun QuickNedarimCard(
    memberNames: List<String>,
    onQuickPledgeAdded: (memberName: String, amount: Double, note: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMember by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var pledgeNote by remember { mutableStateOf("") }
    var showSuccessMessage by remember { mutableStateOf(false) }

    CosmicGlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        borderColor = CosmicCelestialGold.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = CosmicCelestialGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = L("სწრაფი ნედერი (נדרים מהיר)"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = CosmicTextPrimary
                )
            }

            HorizontalDivider(color = CosmicTextPrimary.copy(alpha = 0.1f))

            // წევრის სახელი ან არჩევა
            OutlinedTextField(
                value = selectedMember,
                onValueChange = { selectedMember = it },
                label = { Text(L("წევრის სახელი"), fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // თანხა
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(L("თანხა"), fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                // შენიშვნა / მიზეზი (მაგ: ალია, לעילוי נשמת)
                OutlinedTextField(
                    value = pledgeNote,
                    onValueChange = { pledgeNote = it },
                    label = { Text(L("მიზეზი / შენიშვნა"), fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (selectedMember.isNotBlank() && amount > 0) {
                        onQuickPledgeAdded(selectedMember.trim(), amount, pledgeNote.trim())
                        selectedMember = ""
                        amountText = ""
                        pledgeNote = ""
                        showSuccessMessage = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CosmicCelestialGold,
                    contentColor = CosmicDarkSpace
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = L("ნედერის დაფიქსირება"), fontWeight = FontWeight.Bold)
            }

            if (showSuccessMessage) {
                Text(
                    text = L("✅ ნედერი წარმატებით დაფიქსირდა!"),
                    color = CosmicEmeraldSuccess,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}



