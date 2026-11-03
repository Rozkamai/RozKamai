package com.example.model

enum class Screen {
    SPLASH,
    AUTH,
    DASHBOARD,
    WATCH_EARN,
    HISTORY,
    WITHDRAW,
    KYC,
    SETTINGS
}

enum class Language(val code: String, val label: String) {
    ENGLISH("en", "English"),
    URDU("ur", "اردو")
}

enum class PaymentMethod(val id: String, val titleEn: String, val titleUr: String) {
    JAZZ_CASH("JAZZ_CASH", "JazzCash", "جاز کیش"),
    EASY_PAISA("EASY_PAISA", "EasyPaisa", "ایزی پیسہ"),
    BANK_TRANSFER("BANK_TRANSFER", "Bank Transfer", "بینک ٹرانسفر")
}

enum class WithdrawalStatus(val labelEn: String, val labelUr: String) {
    PENDING("Pending", "زیر التواء"),
    APPROVED("Approved", "منظور شدہ"),
    PAID("Paid", "ادا شدہ"),
    REJECTED("Rejected", "مسترد")
}

enum class LedgerType(val labelEn: String, val labelUr: String, val isCredit: Boolean) {
    REWARD_USER_70("Rewarded Ad (70%)", "اشتہار انعام (70%)", true),
    REWARD_PLATFORM_30("Platform Pool (30%)", "پلیٹ فارم شیئر (30%)", false),
    WITHDRAWAL_HOLD("Withdrawal Hold", "رقم نکالنے پر روک", false),
    WITHDRAWAL_PAID("Withdrawal Paid", "رقم ادا کردی گئی", false),
    WITHDRAWAL_REFUND("Withdrawal Refund", "رقم واپس جمع", true),
    MANUAL_ADJUSTMENT("System Adjustment", "سسٹم تبدیلی", true)
}

data class User(
    val id: String,
    val phone: String,
    val fullName: String,
    val isAgeVerified: Boolean,
    val isKycVerified: Boolean,
    val maskedCnic: String?
)

data class Wallet(
    val availableCoins: Long,
    val pendingCoins: Long,
    val totalEarnedCoins: Long
) {
    val availablePkr: Double get() = availableCoins / 100.0
    val pendingPkr: Double get() = pendingCoins / 100.0
    val totalEarnedPkr: Double get() = totalEarnedCoins / 100.0
}

data class LedgerEntry(
    val id: String,
    val type: LedgerType,
    val amountCoins: Long,
    val balanceAfterCoins: Long,
    val description: String,
    val referenceId: String,
    val timestamp: String
) {
    val amountPkr: Double get() = kotlin.math.abs(amountCoins) / 100.0
}

data class Withdrawal(
    val id: String,
    val amountPkr: Double,
    val amountCoins: Long,
    val method: PaymentMethod,
    val accountTitle: String,
    val accountNumberMasked: String,
    val bankName: String?,
    val status: WithdrawalStatus,
    val date: String,
    val paidReference: String? = null
)
