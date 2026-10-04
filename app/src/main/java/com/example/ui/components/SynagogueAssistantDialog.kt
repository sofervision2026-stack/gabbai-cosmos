package com.example.ui.components

import com.example.ui.settings.L
import com.example.ui.settings.Lf

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.service.AssistantResult
import com.example.ui.theme.*
import com.example.ui.viewmodel.GabbaiViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SynagogueAssistantDialog(
    viewModel: GabbaiViewModel,
    onDismiss: () -> Unit
) {
    val assistantResult by viewModel.assistantResult.collectAsState()
    val isLoading by viewModel.isAssistantLoading.collectAsState()

    var queryText by remember { mutableStateOf("") }

    val presetQueries = listOf(
        L("ვის აქვს გადაუხდელი საწევრო ან ვალი?"),
        L("რა არის ამ თვის შემოსავალი და გასავალი?"),
        L("რა თანხა გაიცა ცედაკიდან და რა მიმართულებით?"),
        "Кто из прихожан должен больше всего?",
        "Сделай краткую сводку по финансам и цдаке"
    )

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
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CosmicNebulaPurple.copy(alpha = 0.25f))
                            .border(1.dp, CosmicStardustCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CosmicStardustCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = L("Gabbai AI ასისტენტი"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CosmicTextPrimary
                        )
                        Text(
                            text = L("Gemini 3.5 Flash • სინაგოგის ინტელექტი"),
                            fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                            color = CosmicCelestialGold
                        )
                    }
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
                    .heightIn(min = 280.dp, max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Preset query suggestions
                Text(
                    text = L("სწრაფი შეკითხვები:"),
                    fontSize = 11.sp,
                    color = CosmicTextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presetQueries) { preset ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CosmicSurfaceCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CosmicBorderGlow),
                            modifier = Modifier.clickable {
                                queryText = preset
                                viewModel.askAssistant(preset)
                            }
                        ) {
                            Text(
                                text = preset,
                                fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                                color = CosmicTextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Query Input Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = queryText,
                        onValueChange = { queryText = it },
                        placeholder = { Text(L("იკითხეთ სინაგოგის ჩანაწერებზე ან დოკუმენტაციაზე...")) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (queryText.isNotBlank()) {
                                viewModel.askAssistant(queryText)
                            }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CosmicStardustCyan,
                            unfocusedBorderColor = CosmicBorderGlow
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_assistant_input")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (queryText.isNotBlank()) {
                                viewModel.askAssistant(queryText)
                            }
                        },
                        enabled = queryText.isNotBlank() && !isLoading,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (queryText.isNotBlank()) CosmicStardustCyan else CosmicSurfaceCard)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = L("გაგზავნა"),
                            tint = if (queryText.isNotBlank()) CosmicDeepSpace else CosmicTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Content View (Answer & Matches)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CosmicSurfaceGlass)
                        .border(1.dp, CosmicBorderGlow, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    if (isLoading) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = CosmicStardustCyan,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = L("Gemini ამუშავებს სინაგოგის რეესტრს & დოკუმენტებს..."),
                                fontSize = 11.sp,
                                color = CosmicTextSecondary
                            )
                        }
                    } else if (assistantResult != null) {
                        val res = assistantResult!!
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Provider badge
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(4.dp),
                                        color = CosmicStardustCyan.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "✨ ${res.provider}",
                                            fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                                            fontWeight = FontWeight.Bold,
                                            color = CosmicStardustCyan,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // AI Answer Text
                            item {
                                Text(
                                    text = res.answer,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    color = CosmicTextPrimary
                                )
                            }

                            // Matched Members section if any
                            if (res.matchedMembers.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = L("დაკავშირებული წევრები:"),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CosmicCelestialGold
                                    )
                                }
                                items(res.matchedMembers) { m ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = CosmicDarkSurface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CosmicBorderGlow),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(m.fullName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CosmicTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text("${m.tribalStatus} • ${m.status.titleKa}", fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, color = CosmicTextSecondary)
                                            }
                                            if (m.outstandingDebt > 0) {
                                                Text(Lf("ვალი: {0}", com.example.ui.settings.money(m.outstandingDebt)), fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, color = CosmicAlertRose, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            // Matched Needy Beneficiaries section if any
                            if (res.matchedNeedy.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = L("გაჭირვებულთა ბაზის ჩანაწერები:"),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CosmicAlertRose
                                    )
                                }
                                items(res.matchedNeedy) { n ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = CosmicDarkSurface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CosmicAlertRose.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("${n.identifierCode} - ${n.fullNameOrPseudonym}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CosmicTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(Lf("{0} • {1} სული", n.category.titleKa, n.familyMembersCount), fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, color = CosmicTextSecondary)
                                            }
                                            Text(Lf("დამტკიცებული: {0}", com.example.ui.settings.money(n.monthlyApprovedAid)), fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, color = CosmicEmeraldSuccess, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = CosmicTextMuted,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = L("შეიყვანეთ შეკითხვა ან აირჩიეთ ზედა სწრაფი ბარათებიდან."),
                                fontSize = 11.sp,
                                color = CosmicTextMuted
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        containerColor = CosmicDarkSurface
    )
}
