package com.example.ui.components

import com.example.ui.settings.L
import com.example.ui.settings.Lf

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.random.Random

/**
 * Cosmic Animated Starfield & Nebula Canvas background.
 * Draws subtle twinkling stars and celestial glow.
 */
@Composable
fun CosmicAnimatedBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val palette = LocalGabbaiPalette.current
    val infiniteTransition = rememberInfiniteTransition(label = "cosmic_nebula")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Pre-calculate fixed star positions
    val stars = remember {
        List(60) {
            Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat() * 2f + 1f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .drawBehind {
                val topGlow = if (palette.isNight) NightPurple else DayGold
                val bottomGlow = if (palette.isNight) NightCyan else DayForest
                // Reflected ambient light, adapted to each lighting mode
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(topGlow.copy(alpha = pulse * 0.22f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.15f),
                        radius = size.width * 0.8f
                    )
                )

                // Stardust cyan nebula at bottom left
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(bottomGlow.copy(alpha = pulse * 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.85f),
                        radius = size.width * 0.7f
                    )
                )

                // Golden Torah glow in center
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf((if (palette.isNight) NightGold else DayGold).copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.4f),
                        radius = size.width * 0.6f
                    )
                )

                // Draw starry particles
                stars.forEach { (nx, ny, r) ->
                    val x = nx * size.width
                    val y = ny * size.height
                    drawCircle(
                        color = palette.text.copy(alpha = 0.22f + (r / 3f) * 0.3f),
                        radius = r,
                        center = Offset(x, y)
                    )
                }
            }
    ) {
        content()
    }
}

/**
 * Ultra-Modern Cosmic Glass Card with subtle stardust border.
 */
@Composable
fun CosmicGlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = LocalGabbaiPalette.current.border,
    cornerRadius: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .shadow(14.dp, RoundedCornerShape(cornerRadius), ambientColor = borderColor.copy(alpha = 0.24f), spotColor = borderColor.copy(alpha = 0.34f))
            .clip(RoundedCornerShape(cornerRadius))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        LocalGabbaiPalette.current.text.copy(alpha = 0.72f),
                        borderColor.copy(alpha = 0.12f),
                        borderColor.copy(alpha = 0.58f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            ),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = LocalGabbaiPalette.current.glass
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

/**
 * Cosmic Metric Card for Dashboards and Financial analytics.
 */
@Composable
fun CosmicMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    testTag: String = "metric_card"
) {
    CosmicGlassCard(
        modifier = modifier.testTag(testTag),
        borderColor = accentColor.copy(alpha = 0.4f)
    ) {
        // Icon + title on one line, the amount gets the full card width so it never gets cut.
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = com.example.ui.settings.nb(title),
                style = MaterialTheme.typography.labelMedium,
                color = LocalGabbaiPalette.current.secondaryText,
                maxLines = 2,
                lineHeight = 16.sp,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontSize = if (value.length > 11) 17.sp else 20.sp,
            color = LocalGabbaiPalette.current.text,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = accentColor,
            fontSize = 11.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Cryptographic verification badge showing AES-256 & SHA-256 integrity.
 */
@Composable
fun CosmicCryptoBadge(
    hash: String = "SHA-256 Verified",
    isVerified: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isVerified) CosmicStardustCyan.copy(alpha = 0.12f) else CosmicAlertRose.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isVerified) CosmicStardustCyan.copy(alpha = 0.5f) else CosmicAlertRose.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isVerified) Icons.Default.Lock else Icons.Default.Warning,
                contentDescription = "Crypto badge",
                tint = if (isVerified) CosmicStardustCyan else CosmicAlertRose,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = hash,
                color = if (isVerified) CosmicStardustCyan else CosmicAlertRose,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Top App Bar with cosmic styling, date, and security lock indicator.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CosmicTopBar(
    currentTitle: String,
    subtitle: String,
    isVaultUnlocked: Boolean,
    onToggleVault: () -> Unit,
    onOpenDocs: () -> Unit,
    onOpenAssistant: () -> Unit = {},
    onOpenVoice: () -> Unit = {},
    onToggleTheme: () -> Unit = {},
    onToggleLanguage: () -> Unit = {},
    isNight: Boolean = true,
    languageCode: String = "KA",
    onBackup: () -> Unit = {},
    onRestore: () -> Unit = {},
    onWipe: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = currentTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LocalGabbaiPalette.current.text,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalGabbaiPalette.current.secondaryText,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        actions = {
            IconButton(onClick = onToggleLanguage) {
                Text(languageCode, style = MaterialTheme.typography.labelLarge, color = CosmicCelestialGold)
            }
            IconButton(onClick = onToggleTheme) {
                Icon(if (isNight) Icons.Default.LightMode else Icons.Default.DarkMode, if (isNight) L("დღის რეჟიმი") else L("ღამის რეჟიმი"), tint = CosmicCelestialGold)
            }
            var menuExpanded by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, L("მეტი მოქმედება"), tint = LocalGabbaiPalette.current.text)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text(L("პარამეტრები"), maxLines = 1) }, leadingIcon = { Icon(Icons.Default.Settings, null) }, onClick = { menuExpanded = false; onOpenSettings() }, modifier = Modifier.testTag("btn_settings"))
                    DropdownMenuItem(text = { Text(L("ხმოვანი მართვა"), maxLines = 1) }, leadingIcon = { Icon(Icons.Default.Mic, null) }, onClick = { menuExpanded = false; onOpenVoice() })
                    DropdownMenuItem(text = { Text("Gabbai AI", maxLines = 1) }, leadingIcon = { Icon(Icons.Default.AutoAwesome, null) }, onClick = { menuExpanded = false; onOpenAssistant() }, modifier = Modifier.testTag("btn_ai_assistant"))
                    DropdownMenuItem(text = { Text(L("დოკუმენტაცია"), maxLines = 1) }, leadingIcon = { Icon(Icons.Default.Description, null) }, onClick = { menuExpanded = false; onOpenDocs() }, modifier = Modifier.testTag("btn_docs_top"))
                    DropdownMenuItem(text = { Text(L("უსაფრთხოება"), maxLines = 1) }, leadingIcon = { Icon(if (isVaultUnlocked) Icons.Default.LockOpen else Icons.Default.Lock, null) }, onClick = { menuExpanded = false; onToggleVault() }, modifier = Modifier.testTag("btn_vault_toggle"))
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text(com.example.ui.settings.tr(L("სარეზერვო ასლის შექმნა"), "Создать резервную копию"), maxLines = 1) }, leadingIcon = { Icon(Icons.Default.Backup, null) }, onClick = { menuExpanded = false; onBackup() })
                    DropdownMenuItem(text = { Text(com.example.ui.settings.tr(L("ასლიდან აღდგენა"), "Восстановить из копии"), maxLines = 1) }, leadingIcon = { Icon(Icons.Default.Restore, null) }, onClick = { menuExpanded = false; onRestore() })
                    DropdownMenuItem(text = { Text(com.example.ui.settings.tr(L("ბაზის სრული გასუფთავება"), "Полная очистка базы"), maxLines = 1, color = MaterialTheme.colorScheme.error) }, leadingIcon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) }, onClick = { menuExpanded = false; onWipe() })
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = LocalGabbaiPalette.current.glass
        ),
        modifier = modifier
    )
}
