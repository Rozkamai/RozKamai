package com.example.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.RozKamaiState
import com.example.model.Language
import com.example.model.Screen
import com.example.ui.theme.*
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Full-screen non-dismissible offline overlay (Section 9)
 */
@Composable
fun OfflineLockOverlay(
    isOnline: Boolean,
    onRetry: () -> Unit
) {
    AnimatedVisibility(
        visible = !isOnline,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val isUrdu = RozKamaiState.currentLanguage == Language.URDU

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable(enabled = false) {}
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier.fillMaxWidth().testTag("offline_lock_card")
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(ErrorRed.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = "No Internet Connection",
                            tint = ErrorRed,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = if (isUrdu) "انٹرنیٹ کنکشن موجود نہیں ہے" else "No Internet Connection",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isUrdu)
                            "RozKamai کو محفوظ رکھنے اور انعام کی تصدیق کے لیے فعال انٹرنیٹ کنکشن لازمی ہے۔ تمام اشتہارات اور ادائیگی روک دی گئی ہیں۔"
                        else
                            "RozKamai requires an active internet connection for fraud verification and live ledger sync. Ad watching and withdrawals are paused.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("retry_connection_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isUrdu) "دوبارہ کوشش کریں" else "Retry Connection",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Application Top Bar with Language toggle and network status indicator
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RozKamaiTopBar(
    title: String,
    onToggleLanguage: () -> Unit
) {
    val isUrdu = RozKamaiState.currentLanguage == Language.URDU

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(GoldAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "RozKamai",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isUrdu) "دیکھیں اور کمائیں (PKR)" else "Watch & Earn PKR",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        actions = {
            // Heartbeat status dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (RozKamaiState.isOnline) SuccessGreen else ErrorRed)
            )
            Spacer(modifier = Modifier.width(12.dp))

            // Language switch chip
            AssistChip(
                onClick = onToggleLanguage,
                label = {
                    Text(
                        text = if (RozKamaiState.currentLanguage == Language.ENGLISH) "اردو" else "EN",
                        fontWeight = FontWeight.Bold
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = "Switch Language",
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.testTag("language_toggle_chip")
            )
            Spacer(modifier = Modifier.width(8.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

/**
 * Bottom Navigation Bar for primary screens
 */
@Composable
fun RozKamaiBottomBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    val isUrdu = RozKamaiState.currentLanguage == Language.URDU

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentScreen == Screen.DASHBOARD,
            onClick = { onNavigate(Screen.DASHBOARD) },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
            label = { Text(if (isUrdu) "ڈیش بورڈ" else "Dashboard") },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = EmeraldPrimary)
        )
        NavigationBarItem(
            selected = currentScreen == Screen.WATCH_EARN,
            onClick = { onNavigate(Screen.WATCH_EARN) },
            icon = { Icon(Icons.Default.PlayCircle, contentDescription = "Watch & Earn") },
            label = { Text(if (isUrdu) "کمائیں" else "Watch & Earn") },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = EmeraldPrimary)
        )
        NavigationBarItem(
            selected = currentScreen == Screen.HISTORY,
            onClick = { onNavigate(Screen.HISTORY) },
            icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "History") },
            label = { Text(if (isUrdu) "تاریخچہ" else "History") },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = EmeraldPrimary)
        )
        NavigationBarItem(
            selected = currentScreen == Screen.WITHDRAW,
            onClick = { onNavigate(Screen.WITHDRAW) },
            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Withdraw") },
            label = { Text(if (isUrdu) "نکلوائیں" else "Withdraw") },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = EmeraldPrimary)
        )
        NavigationBarItem(
            selected = currentScreen == Screen.SETTINGS,
            onClick = { onNavigate(Screen.SETTINGS) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text(if (isUrdu) "ترتیبات" else "Settings") },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = EmeraldPrimary)
        )
    }
}

/**
 * Banner Ad Component (100% Platform Revenue - Section 4)
 */
@Composable
fun BannerAdView(
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        RozKamaiState.recordBannerImpression()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = GoldDark,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "AD",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                Column {
                    Text(
                        text = "Daraz Online Shopping Pakistan",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "Mega Discounts across Pakistan",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Button(
                onClick = {},
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Install", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Full-screen Rewarded Video Ad Simulator Dialog (Section 3)
 */
@Composable
fun VideoAdPlayerDialog(
    networkName: String,
    onCompleted: () -> Unit,
    onSkipped: () -> Unit
) {
    var secondsRemaining by remember { mutableIntStateOf(15) }
    var showSkipWarning by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining--
        }
        onCompleted()
    }

    Dialog(
        onDismissRequest = { showSkipWarning = true },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Simulated Video Visual Canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF0D47A1),
                                Color(0xFF00251A),
                                Color(0xFF1B5E20)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleFilled,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Easypaisa Digital Payments",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Send Money across Pakistan Instantly with Zero Fees",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Top Header: Network Badge & Countdown Timer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = networkName,
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$secondsRemaining",
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { showSkipWarning = true },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Skip Ad",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Bottom Progress Bar
            LinearProgressIndicator(
                progress = { (15 - secondsRemaining) / 15f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .align(Alignment.BottomCenter),
                color = GoldAccent,
                trackColor = Color.White.copy(alpha = 0.3f)
            )

            // Skip Warning Dialog
            if (showSkipWarning) {
                AlertDialog(
                    onDismissRequest = { showSkipWarning = false },
                    title = { Text("Skip Ad?", fontWeight = FontWeight.Bold) },
                    text = {
                        Text("If you close this ad early, 0 coins will be credited. Watch the remaining $secondsRemaining seconds to receive your 70 PKR coins reward.")
                    },
                    confirmButton = {
                        TextButton(onClick = { showSkipWarning = false }) {
                            Text("Resume Ad", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showSkipWarning = false
                            onSkipped()
                        }) {
                            Text("Skip (No Reward)", color = ErrorRed)
                        }
                    }
                )
            }
        }
    }
}

/**
 * Celebration Dialog on Verified S2S Reward Credit
 */
@Composable
fun RewardCelebrationDialog(
    userCoins: Long,
    platformCoins: Long,
    onDismiss: () -> Unit
) {
    val isUrdu = RozKamaiState.currentLanguage == Language.URDU

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(GoldAccent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = null,
                    tint = GoldDark,
                    modifier = Modifier.size(36.dp)
                )
            }
        },
        title = {
            Text(
                text = if (isUrdu) "+$userCoins سکے شامل کردیے گئے!" else "+$userCoins Coins Credited!",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isUrdu)
                        "اشتہار کامیابی کے ساتھ مکمل ہوا۔ آمدنی کی 70/30 تقسیم:"
                    else
                        "Ad completed and verified via S2S callback. 70/30 Revenue Split Breakdown:",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                if (isUrdu) "آپ کا حصہ (70%):" else "Your Share (70%):",
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldPrimary
                            )
                            Text("+$userCoins coins (PKR ${(userCoins / 100.0).format(2)})", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                        }
                        Divider(modifier = Modifier.padding(vertical = 6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                if (isUrdu) "پلیٹ فارم شیئر (30%):" else "Platform Share (30%):",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                            Text("$platformCoins coins", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isUrdu) "شاندار!" else "Claim Rewards")
            }
        }
    )
}

/**
 * Interstitial Ad Simulator (Frequency Capped - Section 4)
 */
@Composable
fun InterstitialAdDialog(
    onDismiss: () -> Unit
) {
    var timer by remember { mutableIntStateOf(5) }

    LaunchedEffect(Unit) {
        while (timer > 0) {
            delay(1000L)
            timer--
        }
    }

    Dialog(
        onDismissRequest = { if (timer == 0) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = timer == 0, dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = GoldDark,
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(
                            text = "INTERSTITIAL AD (100% Platform Revenue)",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(64.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Foodpanda Pakistan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Up to 50% Off on Your Favorite Restaurants in Lahore, Karachi & Islamabad",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Order Food Online")
                    }
                }

                // Close Button with countdown
                IconButton(
                    onClick = { if (timer == 0) onDismiss() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    if (timer > 0) {
                        Text(
                            text = "$timer",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Icon(Icons.Default.Close, contentDescription = "Close Ad")
                    }
                }
            }
        }
    }
}

fun Double.format(digits: Int) = String.format(Locale.US, "%.${digits}f", this)
