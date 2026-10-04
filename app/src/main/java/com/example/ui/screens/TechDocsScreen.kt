package com.example.ui.screens

import com.example.ui.settings.L
import com.example.ui.settings.Lf

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CosmicGlassCard
import com.example.ui.theme.*

@Composable
fun TechDocsScreen(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
    ) {
        // 1. Technical Whitepaper Header
        item {
            CosmicGlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = CosmicStardustCyan.copy(alpha = 0.4f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = CosmicStardustCyan,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = L("ტექნიკური დოკუმენტაცია & სისტემური არქიტექტურა"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CosmicTextPrimary
                        )
                        Text(
                            text = "GABBAI COSMOS v2.5 • סקירה טכנולוגית והשוואת תוכנות גבאים",
                            fontSize = 11.sp,
                            color = CosmicCelestialGold
                        )
                    }
                }
            }
        }

        // 2. Existing Market Solutions Analysis (השוואת תוכנות גבאי קיימות)
        item {
            CosmicGlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = CosmicCelestialGold.copy(alpha = 0.35f)
            ) {
                Text(
                    text = L("1. ბაზრის ანალიზი და არსებული ანალოგების მიმოხილვა"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = CosmicCelestialGold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = L("სინაგოგებისა და გაბაების ბაზარზე არსებული წამყვანი სისტემების დეტალური შესწავლა:"),
                    fontSize = 12.sp,
                    color = CosmicTextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                DocMarketItem(
                    title = "תוכנת הגבאי (The Gabbai Software)",
                    description = L("ერთ-ერთი ყველაზე ძველი და გავრცელებული პროგრამა ისრაელში. უპირატესობა: עליות და იარცეიტების აღრიცხვა. ნაკლოვანებები: მოძველებული Windows დესკტოპ ინტერფეისი, მობილური აპლიკაციის არარსებობა, რთული მონაცემთა ბაზა, ქოლელის მოდულის სიმწირე."),
                    color = CosmicAuroraBlue
                )

                DocMarketItem(
                    title = "הגבאי הממוחשב (Computerized Gabbai)",
                    description = L("ფართო ფუნქციონალი: ადგილების გრაფიკული განაწილება (מיפוי מקומות), ორმხრივი ბუღალტერია. ნაკლოვანებები: რთული და გადატვირთული მენიუ, არ გააჩნია თანამედროვე კრიპტოგრაფიული დაცვა (AES-256), მოითხოვს ხანგრძლივ ტრენინგს."),
                    color = CosmicCelestialGold
                )

                DocMarketItem(
                    title = "גבאי נט (Gabbai Net)",
                    description = L("თანამედროვე Cloud გადაწყვეტა, ციფრული ეკრანების მხარდაჭერა. ნაკლოვანებები: ინტერნეტზე მკაცრი დამოკიდებულება (შაბათზე/მოწყობილობის გათიშვისას უმოქმედოა), პერსონალური მონაცემების გარე ღრუბელში შენახვა, რაც ზრდის კონფიდენციალობის რისკს."),
                    color = CosmicNebulaPurple
                )

                DocMarketItem(
                    title = "צדקה בקליק & בית תפילה (Tzedakah Click / Beit Tefila)",
                    description = L("ფოკუსირებულია მხოლოდ საკრედიტო შემოწირულობებზე. ნაკლოვანებები: არ გააჩნია სინაგოგის ყოველდღიური ხარჯების (კომუნალური, რემონტი, ხელფასები) და ქოლელის სრულფასოვანი მართვა."),
                    color = CosmicAlertRose
                )
            }
        }

        // 3. Gabbai Cosmos Innovations Matrix
        item {
            CosmicGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = L("2. რატომ GABBAI COSMOS? უპირატესობათა მატრიცა"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = CosmicStardustCyan
                )
                Spacer(modifier = Modifier.height(10.dp))

                ComparisonRow(L("დიზაინი & UX"), L("მოძველებული Windows ცხრილები"), L("კოსმოსური ანიმაციური Jetpack Compose M3"))
                ComparisonRow(L("უსაფრთხოება"), L("ღია ტექსტი ან მარტივი პაროლი"), L("AES-256-GCM + SHA-256 ჰეშ-ჯაჭვი"))
                ComparisonRow(L("ავტონომიურობა"), L("Cloud დამოკიდებულება"), L("100% Offline-First დაშიფრული Room DB"))
                ComparisonRow(L("ქოლელის ინტეგრაცია"), L("არ არსებობს / ცალკე Excel"), L("სედერი ა/ბ დასწრება + მილგების აღრიცხვა"))
                ComparisonRow(L("ცედაკის კონფიდენცია"), L("სახელობითი სია (უხერხული)"), "מתן בסתר - ანონიმური კოდები (TZ-XXX)")
                ComparisonRow(L("სეფერ თორის აუქციონი"), L("ქაღალდის ჩანაწერები"), L("ერთი შეხებით ფასუკების/ალიების გატარება"))
            }
        }

        // 4. Cryptographic Vault Specification
        item {
            CosmicGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = L("3. კრიპტოგრაფიული ალგორითმები და მთლიანობის დაცვა"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = CosmicTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = L("• AES-256-GCM (Galois/Counter Mode): გამოიყენება ყველა სენსიტიური შენიშვნის, წევრის დეტალებისა და ცედაკის მონაცემების დასაშიფრად. 128-ბიტიანი ავთენტიფიკაციის თეგი უზრუნველყოფს მონაცემთა შეუცვლელობას.\n\n• SHA-256 Ledger Hash Chain: ყოველი ფინანსური ტრანზაქცია უკავშირდება წინა ტრანზაქციის ჰეშს ფორმულით:\n  H = SHA256(ID + PrevHash + Timestamp + Amount + Type + Title)\nნებისმიერი არაავტორიზებული ჩარევა ბაზაში არღვევს ჰეშ-ჯაჭვს და სისტემა მომენტალურად აფიქსირებს განგაშს."),
                    fontSize = 12.sp,
                    color = CosmicTextSecondary,
                    lineHeight = 18.sp
                )
            }
        }

        // 5. Database Schema Architecture
        item {
            CosmicGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = L("4. მონაცემთა ბაზის რელაციური სტრუქტურა (Room DB)"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = CosmicCelestialGold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = L("სისტემა მოიცავს 7 დამოუკიდებელ მაგრამ კოორდინირებულ ცხრილს:\n1. transactions: სალაროს შემოსავლები/გასავლები ჰეშებით\n2. members: რაბინატი, გაბაები, მრევლი, საწევრო გადასახადები\n3. aliyot: სეფერ თორის ფასუკები, ალიები, ნედერები\n4. kollel_students: ქოლელის აბრეხები, შესასწავლი მასეხეთები\n5. kollel_attendance: დღიური სესიების დასწრების ჟურნალი\n6. tzedakah_funds: ანონიმური დახმარების ფონდი (מתן בסתר)\n7. synagogue_events: ქიდუშები, ბარ-მიცვები, ბიუჯეტირება"),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CosmicTextSecondary,
                    lineHeight = 17.sp
                )
            }
        }

        // 6. Halachic & Future Roadmap
        item {
            CosmicGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = L("5. ჰალახური პრინციპები და განვითარების გეგმა"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = CosmicEmeraldSuccess
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = L("• შაბათის რეჟიმი: აპლიკაცია ითვალისწინებს შაბათის შემდგომ დაუყოვნებლივ სინქრონიზაციას (ნედერების ფიქსაცია შაბათის გასვლისთანავე).\n• NFC ჭკვიანი ბარათები გაბაისთვის: წევრების მომენტალური იდენტიფიცირება.\n• ციფრული ეკრანის ინტერფეისი (לוח דיגיטלי): ლოცვების დროების (זמני היום) და იარცეიტების ავტომატური ჩვენება კედლის მონიტორზე."),
                    fontSize = 12.sp,
                    color = CosmicTextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun DocMarketItem(
    title: String,
    description: String,
    color: Color
) {
    Surface(
        color = CosmicDeepSpace,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = CosmicTextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun ComparisonRow(
    feature: String,
    marketAvg: String,
    gabbaiCosmos: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = feature,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = CosmicTextPrimary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = Lf("ძველი: {0}", marketAvg),
                fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                color = CosmicAlertRose.copy(alpha = 0.8f),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Gabbai: $gabbaiCosmos",
                fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                color = CosmicEmeraldSuccess,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
        }
        Divider(color = CosmicTextPrimary.copy(alpha = 0.05f), modifier = Modifier.padding(top = 4.dp))
    }
}
