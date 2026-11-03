package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.components.RewardCelebrationDialog
import com.example.components.VideoAdPlayerDialog
import com.example.data.RozKamaiState
import com.example.model.Language
import com.example.ui.theme.*

@Composable
fun WatchEarnScreen() {
    val isUrdu = RozKamaiState.currentLanguage == Language.URDU

    var userCreditedCoins by remember { mutableLongStateOf(70L) }
    var platformCreditedCoins by remember { mutableLongStateOf(30L) }

    val isCooldown = RozKamaiState.adCooldownSecondsRemaining > 0
    val isHourlyLimitReached = RozKamaiState.adsWatchedThisHour >= RozKamaiState.maxHourlyAds
    val isDailyLimitReached = RozKamaiState.adsWatchedToday >= RozKamaiState.maxDailyAds

    val canWatch = !isCooldown && !isHourlyLimitReached && !isDailyLimitReached

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Hero Graphic & Title
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(GoldAccent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SmartDisplay,
                contentDescription = null,
                tint = GoldDark,
                modifier = Modifier.size(52.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isUrdu) "ویڈیو اشتہار دیکھیں اور کمائیں" else "Watch Video Ad & Earn",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = if (isUrdu)
                "ہر مکمل اشتہار پر 70 سکے آپ کے بٹوے میں شامل ہوں گے"
            else
                "Receive +70 Coins (PKR 0.70) instantly for every completed video",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 70/30 Split Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (isUrdu) "70/30 تقسیم کا فارمولا" else "70% User / 30% Platform Split",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isUrdu)
                        "اشتہار کا 70% منافع آپ کا ہے اور 30% پلیٹ فارم اور سرور اخراجات کے لیے رکھا جاتا ہے۔"
                    else
                        "Gross revenue per completed ad is 100 coins. You receive 70 coins directly into your wallet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldLight,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(if (isUrdu) "آپ کا حصہ" else "Your Share", fontSize = 11.sp, color = EmeraldDark)
                            Text("70%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
                            Text("+70 Coins", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = EmeraldPrimary)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(if (isUrdu) "پلیٹ فارم پول" else "Platform Share", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("30%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("30 Coins", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Ad Network Selector (Unity Ads as Primary, AppLovin MAX as Option)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isUrdu) "اشتہاری نیٹ ورک منتخب کریں" else "Select Ad Network Provider",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = RozKamaiState.selectedAdNetwork == "Unity Ads",
                        onClick = { RozKamaiState.selectedAdNetwork = "Unity Ads" },
                        label = { Text("Unity Ads (Primary)") },
                        leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldLight, selectedLabelColor = EmeraldDark),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = RozKamaiState.selectedAdNetwork == "AppLovin MAX",
                        onClick = { RozKamaiState.selectedAdNetwork = "AppLovin MAX" },
                        label = { Text("AppLovin MAX") },
                        leadingIcon = { Icon(Icons.Default.AdsClick, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldLight, selectedLabelColor = EmeraldDark),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Prominent Watch Ad Button with State Handling
        Button(
            onClick = {
                if (canWatch) {
                    RozKamaiState.isShowingVideoAd = true
                }
            },
            enabled = canWatch,
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .testTag("watch_video_ad_button")
        ) {
            Icon(
                imageVector = when {
                    isCooldown -> Icons.Default.HourglassTop
                    isHourlyLimitReached || isDailyLimitReached -> Icons.Default.Block
                    else -> Icons.Default.PlayArrow
                },
                contentDescription = null,
                tint = if (canWatch) GoldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = when {
                    isCooldown -> if (isUrdu) "انتظار فرمائیں (${RozKamaiState.adCooldownSecondsRemaining}s)" else "Wait ${RozKamaiState.adCooldownSecondsRemaining}s Cooldown"
                    isHourlyLimitReached -> if (isUrdu) "گھنٹہ وار حد مکمل (10/10)" else "Hourly Limit Reached (10/10)"
                    isDailyLimitReached -> if (isUrdu) "روزانہ حد مکمل (60/60)" else "Daily Limit Reached (60/60)"
                    else -> if (isUrdu) "اشتہار دیکھیں (+70 سکے)" else "Watch Video (+70 Coins)"
                },
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (isUrdu)
                "نوٹ: درمیان میں بند کرنے یا اسکرپٹ کے ذریعے اشتہار چھوڑنے پر کوئی انعام نہیں ملے گا۔"
            else
                "Note: Skipping or closing the ad prematurely will void all reward credits.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }

    // Video Player Dialog Simulation
    if (RozKamaiState.isShowingVideoAd) {
        VideoAdPlayerDialog(
            networkName = RozKamaiState.selectedAdNetwork,
            onCompleted = {
                RozKamaiState.isShowingVideoAd = false
                val (uCoins, pCoins) = RozKamaiState.creditCompletedAdReward(RozKamaiState.selectedAdNetwork)
                userCreditedCoins = uCoins
                platformCreditedCoins = pCoins
                RozKamaiState.isShowingCelebration = true
            },
            onSkipped = {
                RozKamaiState.isShowingVideoAd = false
            }
        )
    }

    // Verified S2S Reward Celebration
    if (RozKamaiState.isShowingCelebration) {
        RewardCelebrationDialog(
            userCoins = userCreditedCoins,
            platformCoins = platformCreditedCoins,
            onDismiss = { RozKamaiState.isShowingCelebration = false }
        )
    }
}
