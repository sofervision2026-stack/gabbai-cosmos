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
import com.example.data.model.KollelStudentEntity
import com.example.ui.components.CosmicGlassCard
import com.example.ui.components.CosmicMetricCard
import com.example.ui.theme.*
import com.example.ui.settings.*
import com.example.ui.components.AmountCurrencyField
import com.example.ui.components.SuggestField
import com.example.data.model.Catalogs
import com.example.ui.viewmodel.GabbaiViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KollelScreen(
    viewModel: GabbaiViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showAddStudentDialog by remember { mutableStateOf(false) }

    val totalStipendBudget = remember(state.kollelStudents) {
        state.kollelStudents.sumOf { it.monthlyStipend }
    }
    val totalPaidStipends = remember(state.kollelStudents) {
        state.kollelStudents.filter { it.stipendPaidThisMonth }.sumOf { it.monthlyStipend }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
        ) {
            // 1. Kollel Overview Metrics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CosmicMetricCard(
                        title = L("აბრეხების რაოდენობა (אברכים)"),
                        value = "${state.kollelStudents.size}",
                        subtitle = L("სწავლობს თორას"),
                        icon = Icons.Default.School,
                        accentColor = CosmicNebulaPurple,
                        modifier = Modifier.weight(1f)
                    )
                    CosmicMetricCard(
                        title = L("თვიური მილგა (מלגות)"),
                        value = money(totalStipendBudget),
                        subtitle = Lf("გაცემულია: {0}", money(totalPaidStipends)),
                        icon = Icons.Default.AutoStories,
                        accentColor = CosmicCelestialGold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2. Kollel Study Sessions Card
            item {
                CosmicGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = CosmicNebulaPurple.copy(alpha = 0.35f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = L("ქოლელის დღის განრიგი (סדרי הכולל)"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = CosmicTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = L("სედერი ა (09:00 - 13:00) • სედერი ბ (15:30 - 19:00)"),
                                fontSize = 11.sp,
                                color = CosmicCelestialGold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CosmicEmeraldSuccess.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = L("აქტიურია"),
                                color = CosmicEmeraldSuccess,
                                fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // 3. Students & Attendance Registry Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = L("აბრეხები & დასწრების აღრიცხვა"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CosmicTextPrimary
                    )
                    Text(
                        text = L("დასწრება: 95%"),
                        fontSize = 11.sp,
                        color = CosmicStardustCyan
                    )
                }
            }

            items(state.kollelStudents, key = { it.id }) { student ->
                KollelStudentCard(
                    student = student,
                    onToggleStipend = { viewModel.toggleStipendPaid(student) },
                    onCheckIn = { morning, afternoon ->
                        viewModel.recordAttendance(student.id, student.fullName, morning, afternoon)
                    }
                )
            }
        }

        // FAB to add Kollel Student
        FloatingActionButton(
            onClick = { showAddStudentDialog = true },
            containerColor = CosmicNebulaPurple,
            contentColor = CosmicTextPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 84.dp, end = 20.dp)
                .testTag("fab_add_kollel")
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = L("აბრეხის დამატება"))
        }

        if (showAddStudentDialog) {
            AddKollelStudentDialog(
                onDismiss = { showAddStudentDialog = false },
                onAdd = { name, phone, masechet, stipend, currency ->
                    viewModel.addKollelStudent(name, phone, masechet, stipend, currency)
                    showAddStudentDialog = false
                }
            )
        }
    }
}

@Composable
fun KollelStudentCard(
    student: KollelStudentEntity,
    onToggleStipend: () -> Unit,
    onCheckIn: (morning: Boolean, afternoon: Boolean) -> Unit
) {
    var morningChecked by remember { mutableStateOf(true) }
    var afternoonChecked by remember { mutableStateOf(true) }

    CosmicGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        borderColor = CosmicNebulaPurple.copy(alpha = 0.35f)
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
                            .background(CosmicNebulaPurple.copy(alpha = 0.15f))
                            .border(1.dp, CosmicNebulaPurple.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = CosmicNebulaPurple,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            text = student.fullName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = CosmicTextPrimary
                        )
                        Text(
                            text = nb(student.currentMasechet), maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmicCelestialGold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = Lf("დასწრება: {0} / {1} სესია", student.totalSessionsPresent, student.totalSessionsExpected),
                            fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                            color = CosmicTextMuted
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = money(student.monthlyStipend, student.currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CosmicStardustCyan
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = onToggleStipend,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (student.stipendPaidThisMonth) CosmicEmeraldSuccess.copy(alpha = 0.2f)
                            else CosmicAlertRose.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = if (student.stipendPaidThisMonth) L("მილგა გაცემულია ✓") else L("გასაცემია"),
                            fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                            color = if (student.stipendPaidThisMonth) CosmicEmeraldSuccess else CosmicAlertRose,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = CosmicTextPrimary.copy(alpha = 0.06f))
            Spacer(modifier = Modifier.height(8.dp))

            // Attendance check-in buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = L("დღევანდელი აღრიცხვა:"),
                    fontSize = 11.sp,
                    color = CosmicTextSecondary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Checkbox(
                            checked = morningChecked,
                            onCheckedChange = {
                                morningChecked = it
                                onCheckIn(morningChecked, afternoonChecked)
                            },
                            colors = CheckboxDefaults.colors(checkedColor = CosmicStardustCyan)
                        )
                        Text(L("სედერი ა"), fontSize = 11.sp, color = CosmicTextPrimary)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = afternoonChecked,
                            onCheckedChange = {
                                afternoonChecked = it
                                onCheckIn(morningChecked, afternoonChecked)
                            },
                            colors = CheckboxDefaults.colors(checkedColor = CosmicStardustCyan)
                        )
                        Text(L("სედერი ბ"), fontSize = 11.sp, color = CosmicTextPrimary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddKollelStudentDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, phone: String, masechet: String, stipend: Double, currency: String) -> Unit
) {
    var currency by remember { mutableStateOf(I18n.defaultCurrency) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var masechet by remember { mutableStateOf("שבת (შაბათი)") }
    var stipendText by remember { mutableStateOf("800") }

    AlertDialog(
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.94f),
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = L("ქოლელის აბრეხის დამატება"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CosmicTextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(L("სრული სახელი (הרב / אברך)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(L("ტელეფონის ნომერი")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                SuggestField(
                    label = L("შესასწავლი მასეხეთი / საგანი"),
                    value = masechet,
                    onValueChange = { masechet = it },
                    suggestions = Catalogs.masechtot
                )

                AmountCurrencyField(L("თვიური სტიპენდია (მილგა)"), stipendText, { stipendText = it }, currency, { currency = it })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val stipend = stipendText.toDoubleOrNull() ?: 800.0
                    if (name.isNotEmpty()) {
                        onAdd(name, phone, masechet, stipend, currency)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CosmicNebulaPurple, contentColor = CosmicOnAccent)
            ) {
                Text(L("დამატება"), fontWeight = FontWeight.Bold)
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
