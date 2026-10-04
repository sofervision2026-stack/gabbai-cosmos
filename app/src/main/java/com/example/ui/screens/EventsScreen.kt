package com.example.ui.screens

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.model.SynagogueEventEntity
import com.example.ui.components.CosmicGlassCard
import com.example.ui.components.AmountCurrencyField
import com.example.ui.components.MemberField
import com.example.ui.components.SuggestField
import com.example.ui.settings.*
import com.example.data.model.Catalogs
import com.example.ui.components.CosmicMetricCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.GabbaiViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    viewModel: GabbaiViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val totalCost = remember(state.events) { state.events.sumOf { it.totalCost } }
    val totalSponsorship = remember(state.events) { state.events.sumOf { it.sponsorContribution } }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
        ) {
            // 1. Events Accounting Overview
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CosmicMetricCard(
                        title = L("სპონსორობა (חסויות)"),
                        value = money(totalSponsorship),
                        subtitle = L("მიღებული შემოსავალი"),
                        icon = Icons.Default.Celebration,
                        accentColor = CosmicEmeraldSuccess,
                        modifier = Modifier.weight(1f)
                    )
                    CosmicMetricCard(
                        title = L("ხარჯები (עלויות)"),
                        value = money(totalCost),
                        subtitle = L("სუფრა & მომსახურება"),
                        icon = Icons.Default.Restaurant,
                        accentColor = CosmicAlertRose,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2. Events Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = L("სადღესასწაულო ღონისძიებები & ქიდუშები"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CosmicTextPrimary
                    )
                    Text(
                        text = Lf("სულ: {0}", state.events.size),
                        fontSize = 11.sp,
                        color = CosmicTextMuted
                    )
                }
            }

            items(state.events, key = { it.id }) { ev ->
                EventCardItem(
                    event = ev,
                    onDelete = { viewModel.deleteEvent(ev.id) }
                )
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = CosmicCelestialGold,
            contentColor = CosmicOnAccent,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 84.dp, end = 20.dp)
                .testTag("fab_add_event")
        ) {
            Icon(Icons.Default.Add, contentDescription = L("ღონისძიების დამატება"))
        }

        if (showAddDialog) {
            AddEventDialog(
                memberNames = state.members.map { it.fullName },
                onAddMember = { viewModel.quickAddMember(it) },
                onDismiss = { showAddDialog = false },
                onAdd = { title, type, sponsor, cost, sponsorDonation, guests, currency ->
                    viewModel.addEvent(title, type, sponsor, cost, sponsorDonation, guests, currency)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun EventCardItem(
    event: SynagogueEventEntity,
    onDelete: () -> Unit
) {
    CosmicGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        borderColor = CosmicCelestialGold.copy(alpha = 0.35f)
    ) {
        Column {
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
                            imageVector = Icons.Default.Celebration,
                            contentDescription = null,
                            tint = CosmicCelestialGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            text = nb(event.title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = CosmicTextPrimary,
                            maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = nb(L(event.eventType) + " • " + Lf("სპონსორი: {0}", event.sponsorName)),
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmicTextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = Lf("მოსალოდნელი სტუმრები: {0}", event.expectedGuests),
                            fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                            color = CosmicTextMuted
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = money(event.sponsorContribution, event.currency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = CosmicEmeraldSuccess
                    )
                    Text(
                        text = "-" + money(event.totalCost, event.currency),
                        fontSize = 11.sp,
                        color = CosmicAlertRose
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
    memberNames: List<String>,
    onAddMember: (String) -> Unit,
    onDismiss: () -> Unit,
    onAdd: (title: String, type: String, sponsor: String, cost: Double, donation: Double, guests: Int, currency: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(Catalogs.eventTypes.first()) }
    var sponsor by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("") }
    var donationText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(I18n.defaultCurrency) }
    var guestsText by remember { mutableStateOf("100") }

    AlertDialog(
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.94f),
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = L("სინაგოგის ღონისძიების დაგეგმვა"),
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
                    SuggestField(
                        label = L("ღონისძიების ტიპი"),
                        value = type,
                        onValueChange = { type = it },
                        suggestions = Catalogs.eventTypes
                    )
                }
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(L("ღონისძიების დასახელება"), maxLines = 1) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    MemberField(
                        label = L("სპონსორი"),
                        value = sponsor,
                        onValueChange = { sponsor = it },
                        memberNames = memberNames,
                        onAddMember = onAddMember
                    )
                }
                item {
                    AmountCurrencyField(L("სპონსორის შემოწირულობა"), donationText, { donationText = it }, currency, { currency = it })
                }
                item {
                    AmountCurrencyField(L("სავარაუდო ხარჯი"), costText, { costText = it }, currency, { currency = it })
                }
                item {
                    OutlinedTextField(
                        value = guestsText,
                        onValueChange = { guestsText = it.filter(Char::isDigit) },
                        label = { Text(L("სტუმრების რაოდენობა"), maxLines = 1) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cost = costText.toDoubleOrNull() ?: 0.0
                    val donation = donationText.toDoubleOrNull() ?: 0.0
                    val guests = guestsText.toIntOrNull() ?: 100
                    val finalTitle = title.ifBlank { type }
                    if (finalTitle.isNotBlank()) {
                        onAdd(finalTitle.trim(), type, sponsor.trim(), cost, donation, guests, currency)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CosmicCelestialGold, contentColor = CosmicOnAccent)
            ) {
                Text(L("დაგეგმვა"), fontWeight = FontWeight.Bold)
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
