package com.example.ui.screens

import com.example.ui.settings.L
import com.example.ui.settings.Lf

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.security.CosmicVault
import com.example.ui.components.CosmicGlassCard
import com.example.ui.components.CosmicMetricCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.GabbaiViewModel

@Composable
fun SecurityScreen(
    viewModel: GabbaiViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
    ) {
        // 1. Cryptographic Status Summary
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CosmicMetricCard(
                    title = L("დაშიფვრის სტანდარტი"),
                    value = "AES-256",
                    subtitle = "GCM 128-bit Auth Tag",
                    icon = Icons.Default.Shield,
                    accentColor = CosmicStardustCyan,
                    modifier = Modifier.weight(1f)
                )
                CosmicMetricCard(
                    title = L("სალაროს ჰეშ-ჯაჭვი"),
                    value = if (state.integrityVerified) L("დაცულია ✓") else L("დარღვევა!"),
                    subtitle = "SHA-256 Immutability",
                    icon = Icons.Default.VerifiedUser,
                    accentColor = if (state.integrityVerified) CosmicEmeraldSuccess else CosmicAlertRose,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 2. Gabbai Master PIN Vault
        item {
            CosmicGlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (state.isVaultUnlocked) CosmicEmeraldSuccess else CosmicCelestialGold
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    if (state.isVaultUnlocked) CosmicEmeraldSuccess.copy(alpha = 0.2f)
                                    else CosmicCelestialGold.copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (state.isVaultUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (state.isVaultUnlocked) CosmicEmeraldSuccess else CosmicCelestialGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = L("გაბაის სარქველი (כספת הגבאי)"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = CosmicTextPrimary
                            )
                            Text(
                                text = if (state.isVaultUnlocked) L("სარქველი ღიაა • წვდომა სრულია") else L("სარქველი ჩაკეტილია • საჭიროა PIN"),
                                fontSize = 11.sp,
                                color = CosmicTextSecondary
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.toggleVaultLock() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isVaultUnlocked) CosmicAlertRose.copy(alpha = 0.25f) else CosmicEmeraldSuccess.copy(alpha = 0.25f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (state.isVaultUnlocked) L("დაბლოკვა") else L("განბლოკვა"),
                            fontSize = 11.sp,
                            color = if (state.isVaultUnlocked) CosmicAlertRose else CosmicEmeraldSuccess,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (!state.isVaultUnlocked) {
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            pinInput = it
                            pinError = false
                            if (it.length == 4) {
                                if (!viewModel.checkPinAndUnlock(it)) {
                                    pinError = true
                                }
                            }
                        },
                        label = { Text(L("შეიყვანეთ გაბაის 4-ნიშნა PIN (ნაგულისხმევი: 7700)")) },
                        isError = pinError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError) {
                        Text(
                            text = L("არასწორი PIN კოდი!"),
                            color = CosmicAlertRose,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // 3. Cryptographic Audit Inspector
        item {
            CosmicGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = L("კრიპტოგრაფიული აუდიტის რეესტრი (SHA-256 Audit Trail)"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = CosmicTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = L("თითოეული ტრანზაქცია გენერირებს შეუცვლელ კრიპტოგრაფიულ ჰეშს (Blockchain Block Chain). ნებისმიერი არაავტორიზებული ჩარევა მყისიერად გამოავლენს მთლიანობის დარღვევას."),
                    fontSize = 12.sp,
                    color = CosmicTextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                state.transactions.take(3).forEach { tx ->
                    Surface(
                        color = CosmicDeepSpace,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CosmicBorderGlow),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    modifier = Modifier.weight(1f),
                                    text = tx.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CosmicCelestialGold
                                )
                                Text(
                                    text = "${com.example.ui.settings.money(tx.amount, tx.currency)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CosmicStardustCyan
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "BLOCK HASH: ${tx.hash}",
                                fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                                fontFamily = FontFamily.Monospace,
                                color = CosmicStardustCyan.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "PREV HASH: ${tx.prevHash}",
                                fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                                fontFamily = FontFamily.Monospace,
                                color = CosmicTextMuted
                            )
                        }
                    }
                }
            }
        }

        // 4. Zero-Knowledge Local Architecture
        item {
            CosmicGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = CosmicCelestialGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = L("ლოკალური დაშიფრული სარქველი (Offline-First)"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CosmicTextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = L("სინაგოგის ფინანსური მონაცემები, შემოწირულობები და ცედაკის ანონიმური მიმღებები ინახება ექსკლუზიურად მოწყობილობის დაცულ მეხსიერებაში SQLite Room ძრავის და AES-256 GCM კრიპტოგრაფიული მოდულის დაცვის ქვეშ."),
                    fontSize = 12.sp,
                    color = CosmicTextSecondary
                )
            }
        }
    }
}
