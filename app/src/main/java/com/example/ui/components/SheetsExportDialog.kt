package com.example.ui.components

import com.example.ui.settings.L
import com.example.ui.settings.Lf

import androidx.compose.ui.text.style.TextOverflow
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.theme.*
import com.example.ui.viewmodel.GabbaiViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetsExportDialog(
    viewModel: GabbaiViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    var selectedCategoryIndex by remember { mutableStateOf(0) }
    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    val categories = listOf(
        L("ჯამაათის წევრები (מתפללים)"),
        L("თორის ალიები & ნედერები"),
        L("ფინანსური სალარო"),
        L("გაჭირვებულთა ბაზა (ნეზაკაკიმ)"),
        L("ქოლელის სტიპენდიები")
    )

    // Generate CSV data according to selected category
    val (headers, rows, csvContent) = remember(selectedCategoryIndex, state) {
        when (selectedCategoryIndex) {
            0 -> {
                val h = listOf("ID", L("სრული სახელი"), "שם לתורה", L("ხარისხი"), L("სტატუსი"), L("სია (Compliance)"), L("შავი სიის მიზეზი"), L("სკამი"), L("ტელეფონი"), L("მისამართი"), L("საწევრო"), L("ვალი"), L("ბოლო გადახდა"), L("ბოლო თარიღი"), L("იარცეიტი"))
                val r = state.members.map { m ->
                    val lastDate = if (m.lastPaymentDateMillis > 0) dateFormatter.format(Date(m.lastPaymentDateMillis)) else "-"
                    listOf(
                        m.id.toString(),
                        m.fullName,
                        m.hebrewName,
                        m.tribalStatus,
                        m.status.titleKa,
                        m.complianceCategory.titleKa,
                        m.blackListReason.ifEmpty { "-" },
                        m.seatNumber.ifEmpty { "-" },
                        m.phone.ifEmpty { "-" },
                        m.address.ifEmpty { "-" },
                        m.annualFeeAmount.toInt().toString(),
                        m.outstandingDebt.toInt().toString(),
                        m.lastPaymentAmount.toInt().toString(),
                        lastDate,
                        m.yahrzeitDate.ifEmpty { "-" }
                    )
                }
                Triple(h, r, generateCsvString(h, r))
            }
            1 -> {
                val h = listOf("ID", L("ფარაშა"), L("ალიის ტიპი"), L("გამარჯვებული"), L("თანხა"), L("სტატუსი"), L("თარიღი"))
                val r = state.aliyot.map { a ->
                    listOf(
                        a.id.toString(),
                        a.parashaName,
                        a.aliyahType.titleKa,
                        a.winnerName,
                        a.amount.toInt().toString(),
                        if (a.status.name == "PAID") L("გადახდილია") else L("ნედერი (ვალი)"),
                        dateFormatter.format(Date(a.dateMillis))
                    )
                }
                Triple(h, r, generateCsvString(h, r))
            }
            2 -> {
                val h = listOf("ID", L("დასახელება"), L("ტიპი"), L("კატეგორია"), L("თანხა"), L("პირი"), L("გადახდის მეთოდი"), L("თარიღი"), L("სტატუსი"))
                val r = state.transactions.map { t ->
                    listOf(
                        t.id.toString(),
                        t.title,
                        if (t.type.name == "INCOME") L("შემოსავალი") else L("გასავალი"),
                        t.category.titleKa,
                        t.amount.toInt().toString(),
                        t.memberName.ifEmpty { "-" },
                        t.paymentMethod,
                        dateFormatter.format(Date(t.dateMillis)),
                        t.status.name
                    )
                }
                Triple(h, r, generateCsvString(h, r))
            }
            3 -> {
                val h = listOf(L("კოდი"), L("სახელი/აღწერა"), L("კატეგორია"), L("სულადობა"), L("ტელეფონი"), L("მისამართი"), L("საჭიროება"), L("დამტკიცებული"), L("სტატუსი"))
                val r = state.needyBeneficiaries.map { b ->
                    listOf(
                        b.identifierCode,
                        b.fullNameOrPseudonym,
                        b.category.titleKa,
                        b.familyMembersCount.toString(),
                        b.phone.ifEmpty { "-" },
                        b.address.ifEmpty { "-" },
                        b.monthlyEstimatedNeed.toInt().toString(),
                        b.monthlyApprovedAid.toInt().toString(),
                        b.status.titleKa
                    )
                }
                Triple(h, r, generateCsvString(h, r))
            }
            else -> {
                val h = listOf("ID", L("აბრეხი (სტუდენტი)"), L("ტელეფონი"), L("მასეხთა"), L("სტიპენდია"), L("სტატუსი"), L("დასწრება"))
                val r = state.kollelStudents.map { k ->
                    listOf(
                        k.id.toString(),
                        k.fullName,
                        k.phone.ifEmpty { "-" },
                        k.currentMasechet,
                        k.monthlyStipend.toInt().toString(),
                        if (k.stipendPaidThisMonth) L("გაცემულია") else L("გასაცემია"),
                        "${k.totalSessionsPresent}/${k.totalSessionsExpected}"
                    )
                }
                Triple(h, r, generateCsvString(h, r))
            }
        }
    }

    AlertDialog(
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.94f),
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TableChart, contentDescription = null, tint = CosmicEmeraldSuccess)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = L("Sheets / ცხრილების ექსპორტი"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CosmicTextPrimary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = L("დახურვა"), tint = CosmicTextMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Category Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedCategoryIndex,
                    containerColor = CosmicDarkSurface,
                    contentColor = CosmicEmeraldSuccess,
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.forEachIndexed { index, catName ->
                        Tab(
                            selected = selectedCategoryIndex == index,
                            onClick = { selectedCategoryIndex = index },
                            text = {
                                Text(
                                    text = catName,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedCategoryIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedCategoryIndex == index) CosmicEmeraldSuccess else CosmicTextMuted
                                )
                            }
                        )
                    }
                }

                // Table Summary info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = Lf("სულ ჩანაწერი: {0}", rows.size),
                        fontSize = 11.sp,
                        color = CosmicTextSecondary
                    )
                    Text(
                        text = L("UTF-8 BOM • Excel & Google Sheets მზადყოფნა"),
                        fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                        color = CosmicTextMuted
                    )
                }

                // Horizontal scrollable table preview
                val horizontalScroll = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CosmicSurfaceGlass)
                        .border(1.dp, CosmicBorderGlow, RoundedCornerShape(8.dp))
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(horizontalScroll)
                    ) {
                        // Header Row
                        item {
                            Row(
                                modifier = Modifier
                                    .background(CosmicDarkSurface)
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                headers.forEach { h ->
                                    Text(
                                        text = h,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = CosmicEmeraldSuccess,
                                        modifier = Modifier.widthIn(min = 90.dp)
                                    )
                                }
                            }
                            Divider(color = CosmicBorderGlow)
                        }

                        // Data Rows
                        items(rows) { row ->
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 6.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                row.forEach { cell ->
                                    Text(
                                        text = cell,
                                        fontSize = 11.sp,
                                        color = CosmicTextSecondary,
                                        modifier = Modifier.widthIn(min = 90.dp)
                                    )
                                }
                            }
                            Divider(color = CosmicTextPrimary.copy(alpha = 0.04f))
                        }
                    }
                }

                // Action Buttons: Share CSV, Copy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            shareCsvFile(context, categories[selectedCategoryIndex], csvContent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicEmeraldSuccess),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = CosmicDeepSpace)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(L("Sheets / Excel გაზიარება"), color = CosmicOnAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Sheets Export", csvContent)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, L("CSV დაკოპირებულია ბუფერში!"), Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmicTextPrimary),
                        modifier = Modifier.weight(0.7f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(L("კოპირება"), fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {},
        containerColor = CosmicDarkSurface
    )
}

/**
 * Builds standard CSV string formatted with UTF-8 BOM so Excel opens Georgian/Hebrew text properly.
 */
private fun generateCsvString(headers: List<String>, rows: List<List<String>>): String {
    val sb = StringBuilder()
    // UTF-8 BOM
    sb.append('\uFEFF')
    sb.append(headers.joinToString(",") { escapeCsvCell(it) }).append("\n")
    rows.forEach { row ->
        sb.append(row.joinToString(",") { escapeCsvCell(it) }).append("\n")
    }
    return sb.toString()
}

private fun escapeCsvCell(cell: String): String {
    return if (cell.contains(",") || cell.contains("\"") || cell.contains("\n")) {
        "\"" + cell.replace("\"", "\"\"") + "\""
    } else {
        cell
    }
}

private fun shareCsvFile(context: Context, categoryName: String, csvContent: String) {
    try {
        val fileName = "${categoryName.replace("[^a-zA-Z0-9_\\-]".toRegex(), "_")}_${System.currentTimeMillis()}.csv"
        val cachePath = File(context.cacheDir, "exports")
        cachePath.mkdirs()
        val file = File(cachePath, fileName)
        FileOutputStream(file).use { out ->
            out.write(csvContent.toByteArray(Charsets.UTF_8))
        }

        val fileUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, "GabbaiCosmos - $categoryName")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, L("გახსნა Google Sheets / Excel-ში")))
    } catch (e: Exception) {
        // Fallback to text share if FileProvider fails
        val textIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, csvContent)
            putExtra(Intent.EXTRA_SUBJECT, "GabbaiCosmos - $categoryName")
        }
        context.startActivity(Intent.createChooser(textIntent, L("მონაცემების გაზიარება")))
    }
}
