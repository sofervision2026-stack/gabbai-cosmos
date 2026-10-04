package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.default.AutoStories
import androidx.compose.material.icons.default.ContentCopy
import androidx.compose.material.icons.default.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MemberEntity
import com.example.ui.theme.*
import com.example.ui.settings.L
import com.example.ui.settings.Lf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiSheberachDialog(
    members: List<MemberEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf("HEALTH") } // HEALTH or DONATION

    // ფილტრაცია: ვინც საჭიროებს მოხსენიებას
    val targetMembers = remember(members, selectedType) {
        if (selectedType == "HEALTH") {
            members.filter { it.yahrzeitDate.isNotEmpty() || it.outstandingDebt > 0 }
        } else {
            members.filter { it.lastPaymentAmount > 0 || it.duesPaid }
        }
    }

    // გენერირებული ტექსტის აწყობა
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
