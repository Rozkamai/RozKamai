package com.example.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.model.Language
import com.example.model.LedgerEntry
import com.example.model.LedgerType
import com.example.model.PaymentMethod
import com.example.model.Screen
import com.example.model.User
import com.example.model.Wallet
import com.example.model.Withdrawal
import com.example.model.WithdrawalStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object RozKamaiState {
    var currentScreen by mutableStateOf(Screen.SPLASH)
    var currentLanguage by mutableStateOf(Language.ENGLISH)
    var isOnline by mutableStateOf(true)

    // User Session
    var currentUser by mutableStateOf<User?>(
        User(
            id = "usr_pk_001",
            phone = "03001234567",
            fullName = "Hamza Ali",
            isAgeVerified = true,
            isKycVerified = true,
            maskedCnic = "42101-*******-1"
        )
    )

    // Wallet (in integer coins, 100 coins = 1 PKR)
    var availableCoins by mutableLongStateOf(24500L) // PKR 245.00
    var pendingCoins by mutableLongStateOf(50000L)   // PKR 500.00
    var totalEarnedCoins by mutableLongStateOf(74500L) // PKR 745.00

    val wallet: Wallet
        get() = Wallet(
            availableCoins = availableCoins,
            pendingCoins = pendingCoins,
            totalEarnedCoins = totalEarnedCoins
        )

    // Ad Limits and Cooldowns
    val maxHourlyAds = 10
    var adsWatchedThisHour by mutableIntStateOf(3)
    val maxDailyAds = 60
    var adsWatchedToday by mutableIntStateOf(14)
    var adCooldownSecondsRemaining by mutableIntStateOf(0)
    var lastAdCompletedTimestamp by mutableLongStateOf(0L)

    // Interstitial Frequency Capping (max 1 per 3 minutes = 180,000 ms)
    var lastInterstitialTimestamp by mutableLongStateOf(0L)
    const val INTERSTITIAL_COOLDOWN_MS = 180000L

    // In-memory Append-Only Ledger
    val ledgerEntries = mutableStateListOf<LedgerEntry>(
        LedgerEntry(
            id = "led_001",
            type = LedgerType.REWARD_USER_70,
            amountCoins = 70L,
            balanceAfterCoins = 24500L,
            description = "Rewarded Ad completion (70% user share)",
            referenceId = "ses_unity_9918",
            timestamp = "Just now"
        ),
        LedgerEntry(
            id = "led_002",
            type = LedgerType.REWARD_USER_70,
            amountCoins = 70L,
            balanceAfterCoins = 24430L,
            description = "Rewarded Ad completion (70% user share)",
            referenceId = "ses_unity_9917",
            timestamp = "15 mins ago"
        ),
        LedgerEntry(
            id = "led_003",
            type = LedgerType.WITHDRAWAL_HOLD,
            amountCoins = -50000L,
            balanceAfterCoins = 24360L,
            description = "Withdrawal request PKR 500 via JazzCash",
            referenceId = "wdr_001",
            timestamp = "2 hours ago"
        ),
        LedgerEntry(
            id = "led_004",
            type = LedgerType.REWARD_USER_70,
            amountCoins = 70L,
            balanceAfterCoins = 74360L,
            description = "Rewarded Ad completion (70% user share)",
            referenceId = "ses_max_1102",
            timestamp = "Yesterday"
        )
    )

    // Withdrawals
    val withdrawals = mutableStateListOf<Withdrawal>(
        Withdrawal(
            id = "wdr_001",
            amountPkr = 500.0,
            amountCoins = 50000L,
            method = PaymentMethod.JAZZ_CASH,
            accountTitle = "Hamza Ali",
            accountNumberMasked = "0300-*******",
            bankName = null,
            status = WithdrawalStatus.PENDING,
            date = "Today, 02:40 PM"
        ),
        Withdrawal(
            id = "wdr_002",
            amountPkr = 1000.0,
            amountCoins = 100000L,
            method = PaymentMethod.EASY_PAISA,
            accountTitle = "Hamza Ali",
            accountNumberMasked = "0300-*******",
            bankName = null,
            status = WithdrawalStatus.PAID,
            date = "Oct 28, 2026",
            paidReference = "EP_TRX_9928172"
        )
    )

    // Ad Impressions Counter
    var bannerImpressionsCount by mutableIntStateOf(42)
    var interstitialImpressionsCount by mutableIntStateOf(5)

    // Active Ad Playing Dialog State
    var isShowingVideoAd by mutableStateOf(false)
    var isShowingCelebration by mutableStateOf(false)
    var isShowingInterstitial by mutableStateOf(false)
    var selectedAdNetwork by mutableStateOf("Unity Ads")

    private fun getCurrentFormattedTime(): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(Date())
    }

    /**
     * Executes 70% User / 30% Platform Split on Ad Completion
     */
    fun creditCompletedAdReward(networkName: String): Pair<Long, Long> {
        val grossCoins = 100L
        val userShare = 70L      // floor(100 * 0.70)
        val platformShare = 30L  // 100 - 70

        val newBalance = availableCoins + userShare
        availableCoins = newBalance
        totalEarnedCoins += userShare
        adsWatchedThisHour += 1
        adsWatchedToday += 1
        adCooldownSecondsRemaining = 30
        lastAdCompletedTimestamp = System.currentTimeMillis()

        // Append User 70% entry
        val sessionId = "ses_${UUID.randomUUID().toString().substring(0, 8)}"
        ledgerEntries.add(
            0,
            LedgerEntry(
                id = "led_${UUID.randomUUID().toString().substring(0, 8)}",
                type = LedgerType.REWARD_USER_70,
                amountCoins = userShare,
                balanceAfterCoins = newBalance,
                description = "Rewarded Ad completed via $networkName (+70 coins)",
                referenceId = sessionId,
                timestamp = getCurrentFormattedTime()
            )
        )

        return Pair(userShare, platformShare)
    }

    /**
     * Submits a withdrawal request (locks balance from available to pending)
     */
    fun submitWithdrawal(
        method: PaymentMethod,
        amountPkr: Double,
        accountTitle: String,
        accountNumber: String,
        bankName: String?
    ): Result<Withdrawal> {
        val requiredCoins = (amountPkr * 100).toLong()

        if (availableCoins < requiredCoins) {
            return Result.failure(Exception("Insufficient available balance."))
        }

        if (amountPkr < 500.0) {
            return Result.failure(Exception("Minimum withdrawal limit is PKR 500."))
        }

        val hasPending = withdrawals.any { it.status == WithdrawalStatus.PENDING || it.status == WithdrawalStatus.APPROVED }
        if (hasPending) {
            return Result.failure(Exception("You already have an active pending withdrawal request."))
        }

        if (currentUser?.isKycVerified != true) {
            return Result.failure(Exception("KYC identity verification with CNIC is required before withdrawing."))
        }

        // Lock funds atomically
        availableCoins -= requiredCoins
        pendingCoins += requiredCoins

        val withdrawalId = "wdr_${UUID.randomUUID().toString().substring(0, 8)}"
        val maskedNum = if (accountNumber.length > 4) {
            accountNumber.substring(0, 4) + "-*******"
        } else {
            "****"
        }

        val withdrawal = Withdrawal(
            id = withdrawalId,
            amountPkr = amountPkr,
            amountCoins = requiredCoins,
            method = method,
            accountTitle = accountTitle,
            accountNumberMasked = maskedNum,
            bankName = bankName,
            status = WithdrawalStatus.PENDING,
            date = "Just now"
        )

        withdrawals.add(0, withdrawal)

        // Ledger Entry for Hold
        ledgerEntries.add(
            0,
            LedgerEntry(
                id = "led_${UUID.randomUUID().toString().substring(0, 8)}",
                type = LedgerType.WITHDRAWAL_HOLD,
                amountCoins = -requiredCoins,
                balanceAfterCoins = availableCoins,
                description = "Withdrawal request PKR ${amountPkr.toInt()} via ${method.name}",
                referenceId = withdrawalId,
                timestamp = getCurrentFormattedTime()
            )
        )

        return Result.success(withdrawal)
    }

    fun submitKyc(cnic: String): Boolean {
        val clean = cnic.replace("[^0-9]".toRegex(), "")
        if (clean.length != 13) return false

        val masked = "${clean.substring(0, 5)}-*******-${clean.substring(12, 13)}"
        currentUser = currentUser?.copy(
            isKycVerified = true,
            maskedCnic = masked
        )
        return true
    }

    fun canShowInterstitial(): Boolean {
        // Frequency cap: max 1 per 3 minutes (180,000 ms), never on withdraw or auth screens
        if (currentScreen == Screen.WITHDRAW || currentScreen == Screen.AUTH || currentScreen == Screen.SPLASH) {
            return false
        }
        val now = System.currentTimeMillis()
        return now - lastInterstitialTimestamp >= INTERSTITIAL_COOLDOWN_MS
    }

    fun recordInterstitialShown() {
        lastInterstitialTimestamp = System.currentTimeMillis()
        interstitialImpressionsCount += 1
    }

    fun recordBannerImpression() {
        bannerImpressionsCount += 1
    }
}
