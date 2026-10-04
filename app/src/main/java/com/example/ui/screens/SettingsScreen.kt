package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppDropdown
import com.example.ui.components.CosmicGlassCard
import com.example.ui.settings.*
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onWipe: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp).testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
    ) {
        item {
            SettingsSection(L("ენა"), Icons.Default.Translate) {
                val langs = AppLanguage.values().toList()
                langs.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { lang ->
                            ChoiceTile(
                                text = lang.nativeName,
                                selected = settings.language == lang,
                                onClick = { onSettingsChange(settings.copy(language = lang)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
        item {
            SettingsSection(L("განათების რეჟიმი"), Icons.Default.Contrast) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChoiceTile(L("დღე"), settings.lighting == LightingMode.DAY, { onSettingsChange(settings.copy(lighting = LightingMode.DAY)) }, Modifier.weight(1f), Icons.Default.LightMode)
                    ChoiceTile(L("ღამე"), settings.lighting == LightingMode.NIGHT, { onSettingsChange(settings.copy(lighting = LightingMode.NIGHT)) }, Modifier.weight(1f), Icons.Default.DarkMode)
                }
            }
        }
        item {
            SettingsSection(L("ნაგულისხმევი ვალუტა"), Icons.Default.CurrencyExchange) {
                Text(
                    L("ახალი ჩანაწერები ავტომატურად ამ ვალუტით შეიქმნება. თითოეულ ფორმაში შეგიძლიათ სხვა ვალუტის არჩევა."),
                    color = CosmicTextSecondary, style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(10.dp))
                AppDropdown(
                    label = L("ვალუტა"),
                    value = Currencies.find(settings.currency),
                    options = Currencies.all,
                    optionLabel = { "${it.symbol}  ${it.code} — ${L(it.nameKa)}" },
                    onSelected = { onSettingsChange(settings.copy(currency = it.code)) }
                )
            }
        }
        item {
            SettingsSection(L("მონაცემები"), Icons.Default.Storage) {
                SettingsAction(L("სრული Excel ექსპორტი"), Icons.Default.TableChart, onExport)
                SettingsAction(L("სარეზერვო ასლის შექმნა"), Icons.Default.Backup, onBackup)
                SettingsAction(L("ასლიდან აღდგენა"), Icons.Default.Restore, onRestore)
                SettingsAction(L("ბაზის სრული გასუფთავება"), Icons.Default.DeleteForever, onWipe, danger = true)
            }
        }
        item {
            CosmicGlassCard(Modifier.fillMaxWidth()) {
                Text("Gabbai Cosmos 2.5", fontWeight = FontWeight.Bold, color = CosmicTextPrimary)
                Text(L("სინაგოგისა და ქოლელის მართვა"), color = CosmicTextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    CosmicGlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = CosmicCelestialGold, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CosmicTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
fun ChoiceTile(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val primary = MaterialTheme.colorScheme.primary
    val palette = LocalGabbaiPalette.current
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .shadow(if (selected) 10.dp else 4.dp, shape, spotColor = primary)
            .clip(shape)
            .background(
                if (selected) Brush.verticalGradient(listOf(primary.copy(alpha = 0.85f), primary))
                else Brush.verticalGradient(listOf(palette.glass, palette.surface))
            )
            .border(1.5.dp, if (selected) primary else palette.border.copy(alpha = 0.5f), shape)
            .clickable(onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = if (selected) MaterialTheme.colorScheme.onPrimary else palette.text, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text, color = if (selected) MaterialTheme.colorScheme.onPrimary else palette.text,
                fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SettingsAction(text: String, icon: ImageVector, onClick: () -> Unit, danger: Boolean = false) {
    val color = if (danger) MaterialTheme.colorScheme.error else LocalGabbaiPalette.current.text
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, color = color, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = LocalGabbaiPalette.current.mutedText)
    }
}
