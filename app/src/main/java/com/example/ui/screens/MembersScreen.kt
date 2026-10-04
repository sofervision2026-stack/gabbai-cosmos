@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.ui.screens

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

    // Configurable thresholds for Red List (Req 3)
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
            // 1. Members and Overview Metrics
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

            // 2. Action Bar: Sheets Export & Red/Black List Settings (Req 2 & 3)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sheets Export Button (Req 2)
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

                    // Red/Black List Rules Settings (Req 3)
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

            // 3. Search Field
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(L("ძიება: სახელი, ებრაული, სკამი, მიზეზი, ტელეფონი...")) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CosmicTextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = L("გასუფთავება"), tint = CosmicTextMuted)
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

            // 4. Advanced filter dropdown
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

            // 5. Header
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

        // FAB to register new member
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

        // Add Member Dialog
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

        // Edit Member Dialog (Req 1)
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

        // Direct Message / SMS / WhatsApp Dialog (Req 2)
        memberToMessage?.let { target ->
            DirectMessageDialog(
                member = target,
                onDismiss = { memberToMessage = null }
            )
        }

        // Sheets Export Dialog (Req 2)
        if (showSheetsExportDialog) {
            SheetsExportDialog(
                viewModel = viewModel,
                onDismiss = { showSheetsExportDialog = false }
            )
        }

        // Red & Black List Rules Dialog (Req 3)
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
            // Header: avatar + name (takes all free width) + compact actions
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

            // Badges on their own full-width line; they wrap to a new line instead of squeezing
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

/**
 * Universal Dialog for adding and editing Jamaat members with Status, Compliance, and Dropdowns (Req 1, 3, 5).
 */
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
    
    // Status (Req 1)
    var status by remember { mutableStateOf(initialMember?.status ?: MemberStatus.ACTIVE) }
    // Compliance Category (Req 3: Normal, Red List, Black List)
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

    var roleDropdownExpanded by remember { mutableStateOf(false) }
    var tribalDropdownExpanded by remember { mutableStateOf(false) }
    var statusDropdownExpanded by remember { mutableStateOf(false) }
    var complianceDropdownExpanded by remember { mutableStateOf(false) }

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
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text(L("სრული სახელი და გვარი")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = hebrewName,
                        onValueChange = { hebrewName = it },
                        label = { Text(L("ებრაული სახელი თორისთვის (שם לתורה)")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 1. Membership Status Dropdown (Req 1)
                item {
                    AppDropdown(
                        label = L("ჯამაათის წევრის სტატუსი"),
                        value = MemberStatus.values().first(),
                        options = MemberStatus.values().toList(),
                        optionLabel = { st -> "${st.titleKa} (${st.titleHe})" },
                        onSelected = { st -> status = st },
                        displayText = status.titleKa,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 2. Compliance Category Dropdown (Req 3: Red List, Black List)
                item {
                    AppDropdown(
                        label = L("სიის კატეგორია (წითელი/შავი სია)"),
                        value = MemberCompliance.values().first(),
                        options = MemberCompliance.values().toList(),
                        optionLabel = { comp -> comp.titleKa },
                        onSelected = { comp -> complianceCategory = comp },
                        displayText = complianceCategory.titleKa,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Reason for Red/Black List
                if (complianceCategory != MemberCompliance.NORMAL) {
                    item {
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
                }

                // Dropdown for Tribal Status (Req 5)
                item {
                    AppDropdown(
                        label = L("სულიერი ხარისხი (თორისთვის)"),
                        value = tribalOptions.first(),
                        options = tribalOptions.toList(),
                        optionLabel = { opt -> opt },
                        onSelected = { opt -> tribalStatus = opt },
                        displayText = tribalStatus,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Dropdown for Synagogue Role (Req 5)
                item {
                    AppDropdown(
                        label = L("როლი სინაგოგაში"),
                        value = MemberRole.values().first(),
                        options = MemberRole.values().toList(),
                        optionLabel = { r -> "${r.titleKa} - ${r.titleHe}" },
                        onSelected = { r -> role = r },
                        displayText = "${role.titleKa} (${role.titleHe})",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
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
                }

                item {
                    OutlinedTextField(
                        value = familyInfo,
                        onValueChange = { familyInfo = it },
                        label = { Text(L("ოჯახური მდგომარეობა (მეუღლე, შვილები)")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(L("საცხოვრებელი მისამართი")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AmountCurrencyField(L("წლიური საწევრო"), annualFeeText, { annualFeeText = it }, currency, { currency = it })
                        AmountCurrencyField(L("ალიების / ნედერის ვალი"), debtText, { debtText = it }, currency, { currency = it })
                    }
                }

                item {
                    AmountCurrencyField(L("ბოლო გადახდილი თანხა"), lastPaymentAmountText, { lastPaymentAmountText = it }, currency, { currency = it })
                }

                item {
                    OutlinedTextField(
                        value = yahrzeit,
                        onValueChange = { yahrzeit = it },
                        label = { Text(L("იარცეიტი (მოხსენიების თარიღი)")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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

/**
 * Compliance Settings Dialog (Req 3)
 * Allows defining the threshold and conditions for Red and Black lists.
 */
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
        modifier = Modifier.fillMaxWidth(0.94f),
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

/**
 * Direct Message Dialog (Req 2)
 */
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
        modifier = Modifier.fillMaxWidth(0.94f),
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
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Template Selector Dropdown
                AppDropdown(
                    label = L("სწრაფი შაბლონები"),
                    value = templates.first(),
                    options = templates.toList(),
                    optionLabel = { (title, body) -> title },
                    onSelected = { (title, body) -> messageText = body },
                    displayText = L("აირჩიეთ შაბლონი..."),
                    modifier = Modifier.fillMaxWidth()
                )

                // Message Text Field
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text(L("შეტყობინების ტექსტი")) },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )

                // Dispatch Options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // WhatsApp Button
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

                    // SMS Button
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

                    // Phone Call Button
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
