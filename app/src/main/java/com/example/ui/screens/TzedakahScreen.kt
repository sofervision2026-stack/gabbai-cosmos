@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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
import com.example.data.model.*
import com.example.data.security.CosmicVault
import com.example.ui.components.CosmicGlassCard
import com.example.ui.components.CosmicMetricCard
import com.example.ui.components.AppDropdown
import com.example.ui.theme.*
import com.example.ui.settings.*
import com.example.ui.components.AmountCurrencyField
import com.example.ui.viewmodel.GabbaiViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TzedakahScreen(
    viewModel: GabbaiViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var currentSubTab by remember { mutableStateOf(0) } // 0: გაჭირვებულთა ბაზა, 1: გაცემული შემწეობები

    // Disbursements states
    var showDistributeDialog by remember { mutableStateOf(false) }
    var selectedDisbursementCategoryFilter by remember { mutableStateOf<TzedakahCategory?>(null) }
    var prefilledBeneficiaryCode by remember { mutableStateOf("") }
    var prefilledAmount by remember { mutableStateOf("") }
    var prefilledCategory by remember { mutableStateOf<TzedakahCategory?>(null) }

    // Needy database states (Req 3)
    var showAddNeedyDialog by remember { mutableStateOf(false) }
    var beneficiaryToEdit by remember { mutableStateOf<NeedyBeneficiaryEntity?>(null) }
    var selectedNeedyCategoryFilter by remember { mutableStateOf<TzedakahCategory?>(null) }
    var needySearchQuery by remember { mutableStateOf("") }

    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    val filteredDisbursements = remember(state.tzedakahFunds, selectedDisbursementCategoryFilter) {
        if (selectedDisbursementCategoryFilter == null) state.tzedakahFunds
        else state.tzedakahFunds.filter { it.category == selectedDisbursementCategoryFilter }
    }

    val filteredBeneficiaries = remember(state.needyBeneficiaries, selectedNeedyCategoryFilter, needySearchQuery) {
        state.needyBeneficiaries.filter { b ->
            val matchesCategory = selectedNeedyCategoryFilter == null || b.category == selectedNeedyCategoryFilter
            val matchesSearch = b.fullNameOrPseudonym.contains(needySearchQuery, ignoreCase = true) ||
                    b.identifierCode.contains(needySearchQuery, ignoreCase = true) ||
                    b.phone.contains(needySearchQuery) ||
                    b.address.contains(needySearchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    val totalDistributed = remember(state.tzedakahFunds) {
        state.tzedakahFunds.sumOf { it.amount }
    }
    val emergencyCount = remember(state.tzedakahFunds) {
        state.tzedakahFunds.count { it.isEmergency }
    }
    val totalApprovedMonthlyAid = remember(state.needyBeneficiaries) {
        state.needyBeneficiaries.filter { it.status == BeneficiaryStatus.ACTIVE }.sumOf { it.monthlyApprovedAid }
    }
    val totalEstimatedMonthlyNeed = remember(state.needyBeneficiaries) {
        state.needyBeneficiaries.sumOf { it.monthlyEstimatedNeed }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
        ) {
            // 1. Tab Selector (Req 3: გაჭირვებულთა ბაზა & გაცემები)
            item {
                TabRow(
                    selectedTabIndex = currentSubTab,
                    containerColor = CosmicDarkSurface,
                    contentColor = CosmicAlertRose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = currentSubTab == 0,
                        onClick = { currentSubTab = 0 },
                        text = {
                            Text(
                                Lf("გაჭირვებულთა ბაზა ({0})", state.needyBeneficiaries.size),
                                fontWeight = if (currentSubTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (currentSubTab == 0) CosmicAlertRose else CosmicTextMuted
                            )
                        },
                        icon = { Icon(Icons.Default.VolunteerActivism, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = currentSubTab == 1,
                        onClick = { currentSubTab = 1 },
                        text = {
                            Text(
                                L("გაცემული შემწეობები"),
                                fontWeight = if (currentSubTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (currentSubTab == 1) CosmicAlertRose else CosmicTextMuted
                            )
                        },
                        icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }

            // -------------------------------------------------------------
            // SUB-TAB 0: NEEDY BENEFICIARIES DATABASE (Req 3)
            // -------------------------------------------------------------
            if (currentSubTab == 0) {
                // Overview metrics
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CosmicMetricCard(
                            title = L("მხარდაჭერის ბაზა (נזקקים)"),
                            value = Lf("{0} ოჯახი", state.needyBeneficiaries.size),
                            subtitle = L("აქტიური კუპათ ცედაკა"),
                            icon = Icons.Default.FamilyRestroom,
                            accentColor = CosmicAlertRose,
                            modifier = Modifier.weight(1f)
                        )
                        CosmicMetricCard(
                            title = L("თვიური დახმარება"),
                            value = money(totalApprovedMonthlyAid),
                            subtitle = Lf("საჭიროება: {0}", money(totalEstimatedMonthlyNeed)),
                            icon = Icons.Default.Savings,
                            accentColor = CosmicCelestialGold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Search Box
                item {
                    OutlinedTextField(
                        value = needySearchQuery,
                        onValueChange = { needySearchQuery = it },
                        placeholder = { Text(L("ძიება: კოდი, ოჯახი, ტელეფონი, მისამართი...")) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CosmicTextSecondary) },
                        trailingIcon = {
                            if (needySearchQuery.isNotEmpty()) {
                                IconButton(onClick = { needySearchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = L("გასუფთავება"), tint = CosmicTextMuted)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CosmicAlertRose,
                            unfocusedBorderColor = CosmicBorderGlow
                        )
                    )
                }

                // Category Filter
                item {
                    Text(
                        text = L("საჭიროების მიმართულებები"),
                        style = MaterialTheme.typography.titleSmall,
                        color = CosmicTextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AppDropdown(
                        label = L("კატეგორია / Категория"),
                        value = selectedNeedyCategoryFilter,
                        options = listOf<TzedakahCategory?>(null) + TzedakahCategory.values().toList(),
                        optionLabel = { it?.titleKa ?: Lf("ყველა ({0})", state.needyBeneficiaries.size) },
                        onSelected = { selectedNeedyCategoryFilter = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Header
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = L("გაჭირვებულთა და ბენეფიციართა კარტოტეკა"),
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
                            text = Lf("ნაჩვენებია: {0}", filteredBeneficiaries.size),
                            fontSize = 11.sp,
                            color = LocalGabbaiPalette.current.mutedText,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                items(filteredBeneficiaries, key = { it.id }) { b ->
                    NeedyBeneficiaryCardItem(
                        beneficiary = b,
                        onDistributeAid = {
                            prefilledBeneficiaryCode = b.identifierCode
                            prefilledAmount = if (b.monthlyApprovedAid > 0) b.monthlyApprovedAid.toInt().toString() else ""
                            prefilledCategory = b.category
                            showDistributeDialog = true
                        },
                        onEdit = { beneficiaryToEdit = b },
                        onDelete = { viewModel.deleteNeedyBeneficiary(b.id) }
                    )
                }
            }

            // -------------------------------------------------------------
            // SUB-TAB 1: DISBURSEMENTS HISTORY
            // -------------------------------------------------------------
            if (currentSubTab == 1) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CosmicMetricCard(
                            title = L("სულ გაცემულია (מתן בסתר)"),
                            value = "${com.example.ui.settings.money(totalDistributed)}",
                            subtitle = L("საერთო გაცემები"),
                            icon = Icons.Default.VolunteerActivism,
                            accentColor = CosmicAlertRose,
                            modifier = Modifier.weight(1f)
                        )
                        CosmicMetricCard(
                            title = L("გადაუდებელი ქეისები"),
                            value = "$emergencyCount",
                            subtitle = L("სასწრაფო დახმარება"),
                            icon = Icons.Default.Emergency,
                            accentColor = CosmicCelestialGold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Halachic Discretion Banner
                item {
                    CosmicGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = CosmicAlertRose.copy(alpha = 0.35f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(CosmicAlertRose.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = CosmicAlertRose, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "„מתן בסתר יכפה אף“ (משלי כא:יד)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = CosmicAlertRose
                                )
                                Text(
                                    text = L("ყველა პერსონალური მონაცემი დაშიფრულია AES-256 ალგორითმით ბენეფიციართა ღირსების დასაცავად."),
                                    fontSize = 11.sp,
                                    color = CosmicTextSecondary
                                )
                            }
                        }
                    }
                }

                // Category Filter (Req 4: selected category preserves into Add dialog)
                item {
                    Text(
                        text = L("ცედაკის ფონდის მიმართულებები"),
                        style = MaterialTheme.typography.titleSmall,
                        color = CosmicTextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AppDropdown(
                        label = L("კატეგორია / Категория"),
                        value = selectedDisbursementCategoryFilter,
                        options = listOf<TzedakahCategory?>(null) + TzedakahCategory.values().toList(),
                        optionLabel = { it?.titleKa ?: L("ყველა") },
                        onSelected = { selectedDisbursementCategoryFilter = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Disbursements Header
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = L("გაცემული შემწეობების რეესტრი"),
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
                            text = Lf("სულ: {0}", filteredDisbursements.size),
                            fontSize = 11.sp,
                            color = LocalGabbaiPalette.current.mutedText,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                items(filteredDisbursements, key = { it.id }) { item ->
                    TzedakahDisbursementCardItem(
                        item = item,
                        dateFormatter = dateFormatter,
                        onDelete = { viewModel.deleteTzedakah(item.id) }
                    )
                }
            }
        }

        // Primary Action FAB
        FloatingActionButton(
            onClick = {
                if (currentSubTab == 0) {
                    showAddNeedyDialog = true
                } else {
                    prefilledBeneficiaryCode = "TZ-${(100..999).random()}"
                    prefilledAmount = ""
                    prefilledCategory = selectedDisbursementCategoryFilter ?: TzedakahCategory.FOOD_BASKETS
                    showDistributeDialog = true
                }
            },
            containerColor = CosmicAlertRose,
            contentColor = CosmicTextPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 84.dp, end = 20.dp)
                .testTag("fab_tzedakah_action")
        ) {
            Icon(
                imageVector = if (currentSubTab == 0) Icons.Default.PersonAdd else Icons.Default.Add,
                contentDescription = if (currentSubTab == 0) L("ბენეფიციარის დამატება") else L("ცედაკის გაცემა")
            )
        }

        // Dialog: Distribute Tzedakah (Req 4: prefilled category & Req 5: Dropdown)
        if (showDistributeDialog) {
            DistributeTzedakahDialog(
                initialCategory = prefilledCategory ?: selectedDisbursementCategoryFilter ?: TzedakahCategory.FOOD_BASKETS,
                initialCode = prefilledBeneficiaryCode.ifEmpty { "TZ-${(100..999).random()}" },
                initialAmount = prefilledAmount,
                onDismiss = { showDistributeDialog = false },
                onDistribute = { code, cat, amount, isEmergency, approvedBy, details, currency ->
                    viewModel.distributeTzedakah(code, cat, amount, isEmergency, approvedBy, details, currency)
                    showDistributeDialog = false
                }
            )
        }

        // Dialog: Add Needy Beneficiary (Req 3 & 5)
        if (showAddNeedyDialog) {
            NeedyBeneficiaryFormDialog(
                title = L("ახალი ბენეფიციარი ოჯახის დამატება"),
                initialBeneficiary = null,
                initialCategory = selectedNeedyCategoryFilter ?: TzedakahCategory.FOOD_BASKETS,
                onDismiss = { showAddNeedyDialog = false },
                onSave = { entity ->
                    viewModel.addNeedyEntity(entity)
                    showAddNeedyDialog = false
                }
            )
        }

        // Dialog: Edit Needy Beneficiary (Req 3 & 5)
        beneficiaryToEdit?.let { editing ->
            NeedyBeneficiaryFormDialog(
                title = Lf("ბენეფიციარის რედაქტირება: {0}", editing.fullNameOrPseudonym),
                initialBeneficiary = editing,
                initialCategory = editing.category,
                onDismiss = { beneficiaryToEdit = null },
                onSave = { updated ->
                    viewModel.updateNeedyBeneficiary(updated)
                    beneficiaryToEdit = null
                }
            )
        }
    }
}

/**
 * Card for Needy Beneficiary Family (Req 3)
 */
@Composable
fun NeedyBeneficiaryCardItem(
    beneficiary: NeedyBeneficiaryEntity,
    onDistributeAid: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showNotes by remember { mutableStateOf(false) }
    val notes = remember(beneficiary.encryptedNotes) {
        CosmicVault.decrypt(beneficiary.encryptedNotes)
    }

    CosmicGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        borderColor = when (beneficiary.status) {
            BeneficiaryStatus.ACTIVE -> CosmicAlertRose.copy(alpha = 0.45f)
            BeneficiaryStatus.ONE_TIME -> CosmicCelestialGold.copy(alpha = 0.45f)
            else -> CosmicBorderGlow
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                            .background(CosmicAlertRose.copy(alpha = 0.15f))
                            .border(1.dp, CosmicAlertRose.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (beneficiary.category) {
                                TzedakahCategory.HACHNASAT_KALLAH -> Icons.Default.Favorite
                                TzedakahCategory.MEDICAL_URGENT -> Icons.Default.MedicalServices
                                TzedakahCategory.FOOD_BASKETS, TzedakahCategory.KIMCHA_DE_PISCHA -> Icons.Default.ShoppingBasket
                                else -> Icons.Default.VolunteerActivism
                            },
                            contentDescription = null,
                            tint = CosmicAlertRose,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = beneficiary.fullNameOrPseudonym,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = CosmicTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CosmicAlertRose.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = beneficiary.identifierCode,
                                    fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.Bold,
                                    color = CosmicAlertRose,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CosmicCelestialGold.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = beneficiary.category.titleKa,
                                    fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                                    color = CosmicCelestialGold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when (beneficiary.status) {
                                    BeneficiaryStatus.ACTIVE -> CosmicEmeraldSuccess.copy(alpha = 0.15f)
                                    BeneficiaryStatus.ONE_TIME -> CosmicStardustCyan.copy(alpha = 0.15f)
                                    else -> CosmicTextMuted.copy(alpha = 0.15f)
                                }
                            ) {
                                Text(
                                    text = beneficiary.status.titleKa,
                                    fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                                    color = when (beneficiary.status) {
                                        BeneficiaryStatus.ACTIVE -> CosmicEmeraldSuccess
                                        BeneficiaryStatus.ONE_TIME -> CosmicStardustCyan
                                        else -> CosmicTextMuted
                                    },
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Actions: Edit, Delete
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = L("რედაქტირება"), tint = CosmicAuroraBlue, modifier = Modifier.size(19.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = L("წაშლა"), tint = CosmicTextMuted, modifier = Modifier.size(19.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = CosmicTextPrimary.copy(alpha = 0.06f))
            Spacer(modifier = Modifier.height(8.dp))

            // Details and Monthly Grants
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = Lf("👨‍👩‍👧‍👦 სულადობა: {0} წევრი", beneficiary.familyMembersCount),
                        fontSize = 11.sp,
                        color = CosmicTextSecondary
                    )
                    if (beneficiary.phone.isNotEmpty()) {
                        Text(
                            text = "📞 ${beneficiary.phone}",
                            fontSize = 11.sp,
                            color = CosmicTextSecondary
                        )
                    }
                    if (beneficiary.address.isNotEmpty()) {
                        Text(
                            text = "📍 ${beneficiary.address}",
                            fontSize = 11.sp,
                            color = CosmicTextMuted
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Lf("საჭიროება: {0}/თვე", money(beneficiary.monthlyEstimatedNeed, beneficiary.currency)),
                        fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                        color = CosmicTextMuted
                    )
                    Text(
                        text = Lf("დამტკიცებული: {0}", money(beneficiary.monthlyApprovedAid, beneficiary.currency)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CosmicEmeraldSuccess
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = onDistributeAid,
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicAlertRose),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.VolunteerActivism, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(L("დახმარების გაცემა"), fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Confidential notes toggle
            if (notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showNotes = !showNotes },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (showNotes) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = CosmicTextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showNotes) notes else L("კონფიდენციალური ჩანაწერის ნახვა (დაშიფრულია)"),
                        fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                        color = if (showNotes) CosmicCelestialGold else CosmicTextMuted
                    )
                }
            }
        }
    }
}

/**
 * Card for Tzedakah Disbursement Entry
 */
@Composable
fun TzedakahDisbursementCardItem(
    item: TzedakahEntity,
    dateFormatter: SimpleDateFormat,
    onDelete: () -> Unit
) {
    var showDecrypted by remember { mutableStateOf(false) }
    val details = remember(item.encryptedDetails) {
        CosmicVault.decrypt(item.encryptedDetails)
    }

    CosmicGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        borderColor = if (item.isEmergency) CosmicAlertRose.copy(alpha = 0.6f) else CosmicBorderGlow
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(CosmicAlertRose.copy(alpha = 0.15f))
                            .border(1.dp, CosmicAlertRose.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (item.isEmergency) Icons.Default.Emergency else Icons.Default.VolunteerActivism,
                            contentDescription = null,
                            tint = if (item.isEmergency) CosmicAlertRose else CosmicCelestialGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.recipientPrivacyCode,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = CosmicTextPrimary
                            )
                            if (item.isEmergency) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = CosmicAlertRose.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = L("სასწრაფო"),
                                        fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                                        fontWeight = FontWeight.Bold,
                                        color = CosmicAlertRose,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "${item.category.titleKa} • ${item.category.titleHe}",
                            fontSize = 11.sp,
                            color = CosmicCelestialGold
                        )
                        Text(
                            text = Lf("დაამტკიცა: {0}", item.approvedByRabbi),
                            fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                            color = CosmicTextMuted
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = money(item.amount, item.currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CosmicAlertRose
                    )
                    Text(
                        text = dateFormatter.format(Date(item.dateMillis)),
                        fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                        color = CosmicTextMuted
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = L("წაშლა"),
                        tint = CosmicTextMuted,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            if (details.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDecrypted = !showDecrypted },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (showDecrypted) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = CosmicStardustCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showDecrypted) details else L("კონფიდენციალური დაშიფრული ჩანაწერი"),
                        fontSize = 11.sp,
                        color = if (showDecrypted) CosmicTextSecondary else CosmicTextMuted
                    )
                }
            }
        }
    }
}

/**
 * Dialog to distribute Tzedakah with Dropdowns (Req 4 & 5)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistributeTzedakahDialog(
    initialCategory: TzedakahCategory,
    initialCode: String,
    initialAmount: String,
    onDismiss: () -> Unit,
    onDistribute: (
        code: String,
        category: TzedakahCategory,
        amount: Double,
        emergency: Boolean,
        approvedBy: String,
        details: String,
        currency: String
    ) -> Unit
) {
    var currency by remember { mutableStateOf(I18n.defaultCurrency) }
    var code by remember { mutableStateOf(initialCode) }
    var selectedCategory by remember { mutableStateOf(initialCategory) } // Req 4: initialized with user's selected category!
    var amountText by remember { mutableStateOf(initialAmount) }
    var isEmergency by remember { mutableStateOf(false) }
    var approvedBy by remember { mutableStateOf("הרב הראשי (მთავარი რაბინი)") }
    var details by remember { mutableStateOf("") }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var approverDropdownExpanded by remember { mutableStateOf(false) }

    val approvers = listOf(
        "הרב הראשי (მთავარი რაბინი)",
        "גבאי ראשי (მთავარი გაბაი)",
        "ועד הצדקה (ცედაკის კომიტეტი)",
        "מורשת קהילה (ჯამაათის გამგეობა)"
    )

    AlertDialog(
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.94f),
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = L("ცედაკის გაცემა (מתן בסתר)"),
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
                        value = code,
                        onValueChange = { code = it },
                        label = { Text(L("ბენეფიციარის ანონიმური კოდი (ან სახელი)")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Dropdown for Tzedakah Category (Req 4 & 5)
                item {
                    AppDropdown(
                        label = L("ცედაკის ფონდის მიმართულება"),
                        value = TzedakahCategory.values().first(),
                        options = TzedakahCategory.values().toList(),
                        optionLabel = { cat -> "${cat.titleKa} - ${cat.titleHe}" },
                        onSelected = { cat -> selectedCategory = cat },
                        displayText = "${selectedCategory.titleKa} (${selectedCategory.titleHe})",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    AmountCurrencyField(L("გასაცემი თანხა"), amountText, { amountText = it }, currency, { currency = it })
                }

                // Dropdown for Approving Rabbi/Gabbai (Req 5)
                item {
                    AppDropdown(
                        label = L("ვინ დაამტკიცა გაცემა"),
                        value = approvers.first(),
                        options = approvers.toList(),
                        optionLabel = { app -> app },
                        onSelected = { app -> approvedBy = app },
                        displayText = approvedBy,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(L("სასწრაფო გადაუდებელი დახმარება:"), modifier = Modifier.weight(1f), fontSize = 12.sp, color = CosmicTextSecondary)
                        Switch(
                            checked = isEmergency,
                            onCheckedChange = { isEmergency = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CosmicAlertRose)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = details,
                        onValueChange = { details = it },
                        label = { Text(L("კონფიდენციალური დეტალები (დაშიფრული AES-256)")) },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0 && code.isNotEmpty()) {
                        onDistribute(code, selectedCategory, amount, isEmergency, approvedBy, details, currency)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CosmicAlertRose, contentColor = CosmicOnAccent)
            ) {
                Text(L("ცედაკის გაცემა"), fontWeight = FontWeight.Bold)
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
 * Universal Dialog to Add / Edit Needy Beneficiary Family (Req 3 & 5)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeedyBeneficiaryFormDialog(
    title: String,
    initialBeneficiary: NeedyBeneficiaryEntity?,
    initialCategory: TzedakahCategory,
    onDismiss: () -> Unit,
    onSave: (NeedyBeneficiaryEntity) -> Unit
) {
    var code by remember { mutableStateOf(initialBeneficiary?.identifierCode ?: "ND-${(100..999).random()}") }
    var nameOrPseudonym by remember { mutableStateOf(initialBeneficiary?.fullNameOrPseudonym ?: "") }
    var category by remember { mutableStateOf(initialBeneficiary?.category ?: initialCategory) }
    var familyCountText by remember { mutableStateOf((initialBeneficiary?.familyMembersCount ?: 4).toString()) }
    var phone by remember { mutableStateOf(initialBeneficiary?.phone ?: "") }
    var address by remember { mutableStateOf(initialBeneficiary?.address ?: "") }
    var estimatedNeedText by remember { mutableStateOf((initialBeneficiary?.monthlyEstimatedNeed ?: 600.0).toInt().toString()) }
    var approvedAidText by remember { mutableStateOf((initialBeneficiary?.monthlyApprovedAid ?: 400.0).toInt().toString()) }
    var status by remember { mutableStateOf(initialBeneficiary?.status ?: BeneficiaryStatus.ACTIVE) }
    var currency by remember { mutableStateOf(initialBeneficiary?.currency ?: I18n.defaultCurrency) }
    var notes by remember {
        mutableStateOf(if (initialBeneficiary != null) CosmicVault.decrypt(initialBeneficiary.encryptedNotes) else "")
    }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var statusDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.94f),
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
                        value = nameOrPseudonym,
                        onValueChange = { nameOrPseudonym = it },
                        label = { Text(L("ოჯახის აღწერა / ფსევდონიმი / სახელი")) },
                        placeholder = { Text(L("მაგ. ოჯახი #105 (ობლები) ან დავით კ.")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text(L("კოდი (ID)")) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = familyCountText,
                            onValueChange = { familyCountText = it },
                            label = { Text(L("სულადობა")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Dropdown for Category (Req 5)
                item {
                    AppDropdown(
                        label = L("საჭიროების მიმართულება"),
                        value = TzedakahCategory.values().first(),
                        options = TzedakahCategory.values().toList(),
                        optionLabel = { cat -> "${cat.titleKa} - ${cat.titleHe}" },
                        onSelected = { cat -> category = cat },
                        displayText = "${category.titleKa} (${category.titleHe})",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Dropdown for Status (Req 5)
                item {
                    AppDropdown(
                        label = L("შემწეობის სტატუსი"),
                        value = BeneficiaryStatus.values().first(),
                        options = BeneficiaryStatus.values().toList(),
                        optionLabel = { st -> st.titleKa },
                        onSelected = { st -> status = st },
                        displayText = status.titleKa,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AmountCurrencyField(L("თვიური საჭიროება"), estimatedNeedText, { estimatedNeedText = it }, currency, { currency = it })
                        AmountCurrencyField(L("დამტკიცებული დახმარება"), approvedAidText, { approvedAidText = it }, currency, { currency = it })
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text(L("საკონტაქტო ტელეფონი")) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text(L("უბანი / მისამართი")) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(L("კონფიდენციალური ჩანაწერები (დაშიფრულია)")) },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val need = estimatedNeedText.toDoubleOrNull() ?: 0.0
                    val approved = approvedAidText.toDoubleOrNull() ?: 0.0
                    val count = familyCountText.toIntOrNull() ?: 1
                    if (nameOrPseudonym.isNotEmpty()) {
                        val result = initialBeneficiary?.copy(
                            identifierCode = code,
                            fullNameOrPseudonym = nameOrPseudonym,
                            category = category,
                            familyMembersCount = count,
                            phone = phone,
                            address = address,
                            monthlyEstimatedNeed = need,
                            monthlyApprovedAid = approved,
                            status = status,
                            currency = currency,
                            encryptedNotes = CosmicVault.encrypt(notes)
                        ) ?: NeedyBeneficiaryEntity(
                            currency = currency,
                            identifierCode = code,
                            fullNameOrPseudonym = nameOrPseudonym,
                            category = category,
                            familyMembersCount = count,
                            phone = phone,
                            address = address,
                            monthlyEstimatedNeed = need,
                            monthlyApprovedAid = approved,
                            status = status,
                            encryptedNotes = CosmicVault.encrypt(notes)
                        )
                        onSave(result)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CosmicAlertRose, contentColor = CosmicOnAccent)
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
