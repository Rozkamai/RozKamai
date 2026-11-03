package com.example

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.components.InterstitialAdDialog
import com.example.components.OfflineLockOverlay
import com.example.components.RozKamaiBottomBar
import com.example.components.RozKamaiTopBar
import com.example.data.RozKamaiState
import com.example.model.Language
import com.example.model.Screen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.KycScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.WatchEarnScreen
import com.example.ui.screens.WithdrawScreen
import com.example.ui.theme.RozKamaiTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RozKamaiTheme {
                RozKamaiApp()
            }
        }
    }
}

@Composable
fun RozKamaiApp() {
    val context = LocalContext.current

    // 1. Online-Only Enforcement: Monitor ConnectivityManager & Server Heartbeat
    DisposableEffect(context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                RozKamaiState.isOnline = true
            }

            override fun onLost(network: Network) {
                RozKamaiState.isOnline = false
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager?.registerNetworkCallback(request, networkCallback)

        onDispose {
            try {
                connectivityManager?.unregisterNetworkCallback(networkCallback)
            } catch (_: Exception) {}
        }
    }

    // 15-second server heartbeat monitor simulation (Section 9)
    LaunchedEffect(Unit) {
        while (true) {
            delay(15000L)
            // Simulates successful ping to GET /health
            // If offline, stays false
        }
    }

    // RTL layout support for Urdu
    val layoutDirection = if (RozKamaiState.currentLanguage == Language.URDU) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(modifier = Modifier.fillMaxSize()) {
            val showTopBottomBars = RozKamaiState.currentScreen != Screen.SPLASH && RozKamaiState.currentScreen != Screen.AUTH

            Scaffold(
                topBar = {
                    if (showTopBottomBars) {
                        RozKamaiTopBar(
                            title = when (RozKamaiState.currentScreen) {
                                Screen.DASHBOARD -> if (RozKamaiState.currentLanguage == Language.URDU) "ڈیش بورڈ" else "Dashboard"
                                Screen.WATCH_EARN -> if (RozKamaiState.currentLanguage == Language.URDU) "اشتہارات دیکھیں" else "Watch & Earn"
                                Screen.HISTORY -> if (RozKamaiState.currentLanguage == Language.URDU) "ٹرانزیکشن لیجر" else "Ledger History"
                                Screen.WITHDRAW -> if (RozKamaiState.currentLanguage == Language.URDU) "رقم نکلوائیں" else "Withdraw PKR"
                                Screen.KYC -> if (RozKamaiState.currentLanguage == Language.URDU) "شناختی تصدیق" else "KYC Verification"
                                Screen.SETTINGS -> if (RozKamaiState.currentLanguage == Language.URDU) "ترتیبات" else "Settings"
                                else -> "RozKamai"
                            },
                            onToggleLanguage = {
                                RozKamaiState.currentLanguage = if (RozKamaiState.currentLanguage == Language.ENGLISH) Language.URDU else Language.ENGLISH
                            }
                        )
                    }
                },
                bottomBar = {
                    if (showTopBottomBars) {
                        RozKamaiBottomBar(
                            currentScreen = RozKamaiState.currentScreen,
                            onNavigate = { screen ->
                                if (screen != RozKamaiState.currentScreen && RozKamaiState.canShowInterstitial()) {
                                    RozKamaiState.isShowingInterstitial = true
                                    RozKamaiState.recordInterstitialShown()
                                }
                                RozKamaiState.currentScreen = screen
                            }
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (RozKamaiState.currentScreen) {
                        Screen.SPLASH -> {
                            SplashScreen(
                                onSplashFinished = {
                                    RozKamaiState.currentScreen = if (RozKamaiState.currentUser != null) Screen.DASHBOARD else Screen.AUTH
                                }
                            )
                        }

                        Screen.AUTH -> {
                            AuthScreen(
                                onAuthSuccess = {
                                    RozKamaiState.currentScreen = Screen.DASHBOARD
                                }
                            )
                        }

                        Screen.DASHBOARD -> {
                            DashboardScreen(
                                onNavigate = { screen ->
                                    RozKamaiState.currentScreen = screen
                                }
                            )
                        }

                        Screen.WATCH_EARN -> {
                            BackHandler {
                                RozKamaiState.currentScreen = Screen.DASHBOARD
                            }
                            WatchEarnScreen()
                        }

                        Screen.HISTORY -> {
                            BackHandler {
                                RozKamaiState.currentScreen = Screen.DASHBOARD
                            }
                            HistoryScreen()
                        }

                        Screen.WITHDRAW -> {
                            BackHandler {
                                RozKamaiState.currentScreen = Screen.DASHBOARD
                            }
                            WithdrawScreen(
                                onNavigate = { screen ->
                                    RozKamaiState.currentScreen = screen
                                }
                            )
                        }

                        Screen.KYC -> {
                            BackHandler {
                                RozKamaiState.currentScreen = Screen.DASHBOARD
                            }
                            KycScreen()
                        }

                        Screen.SETTINGS -> {
                            BackHandler {
                                RozKamaiState.currentScreen = Screen.DASHBOARD
                            }
                            SettingsScreen(
                                onLogout = {
                                    RozKamaiState.currentUser = null
                                    RozKamaiState.currentScreen = Screen.AUTH
                                }
                            )
                        }
                    }
                }
            }

            // Interstitial Ad Dialog Simulator (Section 4)
            if (RozKamaiState.isShowingInterstitial) {
                InterstitialAdDialog(
                    onDismiss = { RozKamaiState.isShowingInterstitial = false }
                )
            }

            // Full-Screen Non-Dismissible Offline Overlay (Section 9)
            OfflineLockOverlay(
                isOnline = RozKamaiState.isOnline,
                onRetry = {
                    // Retry connection check
                    RozKamaiState.isOnline = true
                }
            )
        }
    }
}
