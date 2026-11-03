package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.components.BannerAdView
import com.example.components.format
import com.example.data.RozKamaiState
import com.example.model.Language
import com.example.model.Screen
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    onNavigate: (Screen) -> Unit
) {
    val isUrdu = RozKamaiState.currentLanguage == Language.URDU
    val wallet = RozKamaiState.wallet

    // Real-time 30s auto-refresh trigger & cooldown decrementer
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            if (RozKamaiState.adCooldownSecondsRemaining > 0) {
                RozKamaiState.adCooldownSecondsRemaining--
            }
        }
    }

    Scaffold(
        bottomBar = {
            BannerAdView()
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Welcome Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isUrdu) "خوش آمدید، ${RozKamaiState.currentUser?.fullName ?: "صارف"}" else "Assalam-o-Alaikum, ${RozKamaiState.currentUser?.fullName ?: "User"}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (isUrdu) "100 سکے = PKR 1.00 • 70/30 تقسیم" else "100 Coins = PKR 1.00 • 70/30 Revenue Split",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // KYC Status Badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (RozKamaiState.currentUser?.isKycVerified == true) SuccessGreen.copy(alpha = 0.15f) else WarningAmber.copy(alpha = 0.15f),
                    modifier = Modifier.testTag("kyc_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (RozKamaiState.currentUser?.isKycVerified == true) Icons.Default.VerifiedUser else Icons.Default.GppMaybe,
                            contentDescription = null,
                            tint = if (RozKamaiState.currentUser?.isKycVerified == true) SuccessGreen else WarningAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (RozKamaiState.currentUser?.isKycVerified == true) (if (isUrdu) "KYC تصدیق شدہ" else "KYC Verified") else (if (isUrdu) "KYC باقی ہے" else "KYC Needed"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (RozKamaiState.currentUser?.isKycVerified == true) SuccessGreen else WarningAmber
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Real-Time Primary Balance Card (Section 6 & 11)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("balance_card")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(EmeraldDark, EmeraldPrimary, Color(0xFF004D40))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isUrdu) "دستیاب رقم (Available Balance)" else "Available Balance",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = GoldAccent
                            ) {
                                Text(
                                    text = "70% User Share",
                                    color = Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Large PKR balance display
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "PKR ",
                                color = GoldAccent,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = wallet.availablePkr.format(2),
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Text(
                            text = "${wallet.availableCoins} coins",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))
                        Divider(color = Color.White.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Mini metrics: Total Earned and Pending Withdrawals
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (isUrdu) "کل کمائی (Total Earned)" else "Total Earned",
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "PKR ${wallet.totalEarnedPkr.format(2)}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isUrdu) "زیر التواء رقم (Pending)" else "Pending Withdrawals",
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "PKR ${wallet.pendingPkr.format(2)}",
                                    color = GoldAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Hourly Ad-Limit Progress Card & Cooldown (Section 5 & 11)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isUrdu) "گھنٹہ وار اشتہارات کی حد" else "Hourly Ad Limit Progress",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${RozKamaiState.adsWatchedThisHour} / ${RozKamaiState.maxHourlyAds} ads completed this hour",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { RozKamaiState.adsWatchedThisHour / RozKamaiState.maxHourlyAds.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = EmeraldPrimary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        if (RozKamaiState.adCooldownSecondsRemaining > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Next ad in: ${RozKamaiState.adCooldownSecondsRemaining}s cooldown",
                                color = WarningAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Progress Ring Display
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(EmeraldLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${RozKamaiState.maxHourlyAds - RozKamaiState.adsWatchedThisHour}",
                            color = EmeraldDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Primary Call to Action Button
            Button(
                onClick = {
                    if (RozKamaiState.canShowInterstitial()) {
                        RozKamaiState.isShowingInterstitial = true
                        RozKamaiState.recordInterstitialShown()
                    }
                    onNavigate(Screen.WATCH_EARN)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("watch_earn_cta_button")
            ) {
                Icon(Icons.Default.PlayCircleFilled, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isUrdu) "اشتہار دیکھیں اور 70 سکے کمائیں" else "Watch Ad & Earn 70 Coins",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Secondary Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onNavigate(Screen.WITHDRAW) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isUrdu) "رقم نکلوائیں" else "Withdraw")
                }

                OutlinedButton(
                    onClick = { onNavigate(Screen.HISTORY) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isUrdu) "لیجر ہسٹری" else "Ledger")
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Recent Activity Title
            Text(
                text = if (isUrdu) "حالیہ ٹرانزیکشنز" else "Recent Transactions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Last 2 Ledger Entries
            RozKamaiState.ledgerEntries.take(2).forEach { entry ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (entry.type.isCredit) SuccessGreen.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (entry.type.isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (entry.type.isCredit) SuccessGreen else ErrorRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isUrdu) entry.type.labelUr else entry.type.labelEn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = entry.timestamp,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "${if (entry.type.isCredit) "+" else ""}${entry.amountCoins} coins",
                            fontWeight = FontWeight.Bold,
                            color = if (entry.type.isCredit) SuccessGreen else ErrorRed
                        )
                    }
                }
            }
        }
    }
}
