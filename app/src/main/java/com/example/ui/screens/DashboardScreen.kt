package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.TransactionType
import com.example.data.security.CosmicVault
import com.example.ui.components.CosmicCryptoBadge
import com.example.ui.components.CosmicGlassCard
import com.example.ui.components.CosmicMetricCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.GabbaiScreen
import com.example.ui.viewmodel.GabbaiUiState
import com.example.ui.settings.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    state: GabbaiUiState,
    onNavigate: (GabbaiScreen) -> Unit,
    onOpenVoice: () -> Unit = {},
    onExport: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
    ) {
        // 1. Hero Cosmic Sanctuary Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, CosmicBorderGlow, RoundedCornerShape(22.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.cosmic_synagogue_hero_1790845710719),
                    contentDescription = "Cosmic Synagogue Sanctuary",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Dark gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    CosmicDeepSpace.copy(alpha = 0.85f),
                                    CosmicDeepSpace
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = CosmicStardustCyan.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CosmicStardustCyan)
                        ) {
                            Text(
                                text = "GABBAI COSMOS v2.5",
                                color = CosmicStardustCyan,
                                fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        CosmicCryptoBadge(
                            hash = "AES-256-GCM Vault",
                            isVerified = state.integrityVerified
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = tr("სინაგოგის მართვის ერთიანი სისტემა", "Единая система управления синагогой"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CosmicTextPrimary
                    )
                    Text(
                        text = tr("ფინანსები • წევრები • ალიები • ცედაკა", "Финансы • прихожане • алиёт • цдака"),
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmicCelestialGold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // 2. Metrics Row 1: Balance & Outstanding Pledges
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CosmicMetricCard(
                    title = L("წმინდა ბალანსი (יתרה)"),
                    value = money(state.netBalance),
                    subtitle = L("აქტიური სალარო"),
                    icon = Icons.Default.AccountBalance,
                    accentColor = if (state.netBalance >= 0) CosmicStardustCyan else CosmicAlertRose,
                    modifier = Modifier.weight(1f),
                    testTag = "card_balance"
                )

                CosmicMetricCard(
                    title = L("დავალიანება (חובות)"),
                    value = money(state.totalOutstandingPledges),
                    subtitle = L("ნედერები & საწევრო"),
                    icon = Icons.Default.PendingActions,
                    accentColor = CosmicCelestialGold,
                    modifier = Modifier.weight(1f),
                    testTag = "card_pledges"
                )
            }
        }

        // 3. Metrics Row 2: Incomes & Expenses
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CosmicMetricCard(
                    title = L("სულ შემოსავალი (הכנסות)"),
                    value = signedMoney(state.totalIncome, true),
                    subtitle = L("ალიები, შემოწირულობები"),
                    icon = Icons.Default.TrendingUp,
                    accentColor = CosmicEmeraldSuccess,
                    modifier = Modifier.weight(1f),
                    testTag = "card_income"
                )

                CosmicMetricCard(
                    title = L("სულ გასავალი (הוצאות)"),
                    value = signedMoney(state.totalExpense, false),
                    subtitle = L("ხელფასები, ქოლელი, დენი"),
                    icon = Icons.Default.TrendingDown,
                    accentColor = CosmicAlertRose,
                    modifier = Modifier.weight(1f),
                    testTag = "card_expense"
                )
            }
        }

        item {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onOpenVoice, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Mic, null); Spacer(Modifier.width(8.dp)); Text(tr("ხმოვანი მართვა", "Голосовое управление"), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.TableChart, null); Spacer(Modifier.width(8.dp)); Text(tr("სრული Excel", "Полный Excel"), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        // 4. Quick Action Cosmic Panel
        item {
            CosmicGlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = L("სწრაფი წვდომის მოდულები"),
                    style = MaterialTheme.typography.titleSmall,
                    color = CosmicTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))

                // All six modules on ONE line, each gets an equal share of the width.
                Row(modifier = Modifier.fillMaxWidth().testTag("quick_actions_row"), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    QuickActionIcon(Icons.Default.MenuBook, L("ალიები"), CosmicCelestialGold, { onNavigate(GabbaiScreen.ALIYOT) }, Modifier.weight(1f))
                    QuickActionIcon(Icons.Default.Payments, L("ფინანსები"), CosmicStardustCyan, { onNavigate(GabbaiScreen.FINANCES) }, Modifier.weight(1f))
                    QuickActionIcon(Icons.Default.School, L("ქოლელი"), CosmicNebulaPurple, { onNavigate(GabbaiScreen.KOLLEL) }, Modifier.weight(1f))
                    QuickActionIcon(Icons.Default.VolunteerActivism, L("ცედაკა"), CosmicAlertRose, { onNavigate(GabbaiScreen.TZEDAKAH) }, Modifier.weight(1f))
                    QuickActionIcon(Icons.Default.People, L("წევრები"), CosmicAuroraBlue, { onNavigate(GabbaiScreen.MEMBERS) }, Modifier.weight(1f))
                    QuickActionIcon(Icons.Default.Celebration, L("ღონისძიებები"), CosmicEmeraldSuccess, { onNavigate(GabbaiScreen.EVENTS) }, Modifier.weight(1f))
                }
            }
        }

        // 5. Active Torah Aliyot Bids Summary
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = L("ბოლო ალიები და ფასუკები"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LocalGabbaiPalette.current.text,
                    lineHeight = 25.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = Lf("ყველა ({0})", state.aliyot.size),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.clickable { onNavigate(GabbaiScreen.ALIYOT) }
                )
            }
        }

        items(state.aliyot.take(3)) { aliyah ->
            CosmicGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp,
                borderColor = CosmicCelestialGold.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = aliyah.aliyahType.titleKa, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = CosmicCelestialGold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(text = "(${aliyah.aliyahType.titleHe})", fontSize = 11.sp, color = CosmicTextSecondary, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = aliyah.winnerName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CosmicTextPrimary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = nb(aliyah.parashaName),
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmicTextMuted,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = money(aliyah.amount, aliyah.currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CosmicStardustCyan
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (aliyah.status == com.example.data.model.TransactionStatus.PAID)
                                CosmicEmeraldSuccess.copy(alpha = 0.2f)
                            else
                                CosmicAlertRose.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (aliyah.status == com.example.data.model.TransactionStatus.PAID) L("გადახდილია") else L("ნედერი (ვალი)"),
                                fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                                color = if (aliyah.status == com.example.data.model.TransactionStatus.PAID) CosmicEmeraldSuccess else CosmicAlertRose,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // 6. Recent Ledger Transactions with SHA-256 Hash
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = L("სალაროს ბოლო ტრანზაქციები"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LocalGabbaiPalette.current.text,
                    lineHeight = 25.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = Lf("სრულად ({0})", state.transactions.size),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.clickable { onNavigate(GabbaiScreen.FINANCES) }
                )
            }
        }

        items(state.transactions.take(4)) { tx ->
            CosmicGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp,
                borderColor = if (tx.type == TransactionType.INCOME) CosmicEmeraldSuccess.copy(alpha = 0.2f) else CosmicAlertRose.copy(alpha = 0.2f)
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (tx.type == TransactionType.INCOME) CosmicEmeraldSuccess.copy(alpha = 0.15f)
                                    else CosmicAlertRose.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (tx.type == TransactionType.INCOME) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = if (tx.type == TransactionType.INCOME) CosmicEmeraldSuccess else CosmicAlertRose,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = nb(tx.title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = CosmicTextPrimary,
                                maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = dateFormatter.format(Date(tx.dateMillis)),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = CosmicTextMuted
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = CosmicVault.formatShortHash(tx.hash),
                                    fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                                    color = CosmicStardustCyan.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    Text(
                        text = signedMoney(tx.amount, tx.type == TransactionType.INCOME, tx.currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (tx.type == TransactionType.INCOME) CosmicEmeraldSuccess else CosmicAlertRose
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionIcon(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f))
                .border(1.dp, color.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                 modifier = Modifier.size(21.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = LocalGabbaiPalette.current.secondaryText,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
