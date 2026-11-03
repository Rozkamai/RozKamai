package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.components.format
import com.example.data.RozKamaiState
import com.example.model.Language
import com.example.model.PaymentMethod
import com.example.model.Screen
import com.example.model.WithdrawalStatus
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawScreen(
    onNavigate: (Screen) -> Unit
) {
    val isUrdu = RozKamaiState.currentLanguage == Language.URDU

    var selectedMethod by remember { mutableStateOf(PaymentMethod.JAZZ_CASH) }
    var amountPkrText by remember { mutableStateOf("500") }
    var accountTitle by remember { mutableStateOf("Hamza Ali") }
    var accountNumber by remember { mutableStateOf("03001234567") }
    var bankName by remember { mutableStateOf("Standard Chartered Bank") }
    var bankIban by remember { mutableStateOf("PK36SCBL0000001123456701") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val amountPkr = amountPkrText.toDoubleOrNull() ?: 0.0
    val coinsRequired = (amountPkr * 100).toLong()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = if (isUrdu) "رقم نکلوانے کی درخواست" else "Withdraw Earnings (PKR)",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = if (isUrdu) "جاز کیش، ایزی پیسہ یا پاکستانی بینک اکاؤنٹ میں فوری ادائیگی" else "Withdraw directly to JazzCash, EasyPaisa, or 1Link Pakistani Bank Account",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Balance Summary Header
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isUrdu) "دستیاب رقم" else "Available Balance",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "PKR ${RozKamaiState.wallet.availablePkr.format(2)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EmeraldLight
                ) {
                    Text(
                        text = "${RozKamaiState.availableCoins} coins",
                        color = EmeraldDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // KYC Warning if not verified
        if (RozKamaiState.currentUser?.isKycVerified != true) {
            Card(
                colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = WarningAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isUrdu) "رقم نکالنے سے پہلے CNIC تصدیق ضروری ہے۔" else "Identity verification (CNIC) is required before withdrawing.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Button(
                        onClick = { onNavigate(Screen.KYC) },
                        colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Verify", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Error & Success Messages
        if (errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text(text = errorMessage!!, color = ErrorRed, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
            }
        }

        if (successMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text(text = successMessage!!, color = SuccessGreen, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
            }
        }

        // Form Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Payment Method Selector Tabs
                Text(
                    text = if (isUrdu) "ادائیگی کا طریقہ منتخب کریں" else "Select Payment Method",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMethod.values().forEach { method ->
                        val isSelected = selectedMethod == method
                        OutlinedButton(
                            onClick = { selectedMethod = method },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) EmeraldLight else Color.Transparent
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outline
                                )
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (isUrdu) method.titleUr else method.titleEn,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) EmeraldDark else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount in PKR
                OutlinedTextField(
                    value = amountPkrText,
                    onValueChange = { amountPkrText = it },
                    label = { Text(if (isUrdu) "رقم (PKR)" else "Withdrawal Amount (PKR)") },
                    leadingIcon = { Text("Rs.", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = {
                        Text(
                            text = "Live Conversion: $coinsRequired coins • Min: PKR 500",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("withdraw_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Account Title
                OutlinedTextField(
                    value = accountTitle,
                    onValueChange = { accountTitle = it },
                    label = { Text(if (isUrdu) "اکاؤنٹ ہولڈر کا نام (Account Title)" else "Account Title (Full Name)") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Dynamic Fields based on method
                if (selectedMethod == PaymentMethod.JAZZ_CASH || selectedMethod == PaymentMethod.EASY_PAISA) {
                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it },
                        label = { Text(if (isUrdu) "${selectedMethod.titleUr} موبائل نمبر" else "${selectedMethod.titleEn} Mobile Wallet Number") },
                        leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) },
                        placeholder = { Text("03001234567") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wallet_number_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text(if (isUrdu) "بینک کا نام" else "Bank Name") },
                        leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = bankIban,
                        onValueChange = { bankIban = it },
                        label = { Text(if (isUrdu) "پاکستانی IBAN (24 حروف)" else "Pakistani IBAN (24 Characters)") },
                        leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null) },
                        placeholder = { Text("PK36SCBL0000001123456701") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        errorMessage = null
                        successMessage = null
                        if (amountPkr < 500) {
                            errorMessage = "Minimum withdrawal is PKR 500."
                            return@Button
                        }
                        if (accountTitle.trim().length < 3) {
                            errorMessage = "Please enter valid account title."
                            return@Button
                        }
                        if (RozKamaiState.availableCoins < coinsRequired) {
                            errorMessage = "Insufficient coins balance."
                            return@Button
                        }
                        showConfirmDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_withdrawal_button")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isUrdu) "درخواست جمع کریں" else "Submit Withdrawal Request",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Existing Withdrawals Status Tracker
        Text(
            text = if (isUrdu) "آپ کی ادائیگیاں" else "Withdrawal Requests",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        RozKamaiState.withdrawals.forEach { w ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PKR ${w.amountPkr.toInt()} via ${w.method.titleEn}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${w.accountTitle} (${w.accountNumberMasked}) • ${w.date}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (w.paidReference != null) {
                            Text(
                                text = "Ref: ${w.paidReference}",
                                fontSize = 11.sp,
                                color = SuccessGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (w.status) {
                            WithdrawalStatus.PAID -> SuccessGreen.copy(alpha = 0.15f)
                            WithdrawalStatus.PENDING -> WarningAmber.copy(alpha = 0.15f)
                            WithdrawalStatus.APPROVED -> EmeraldLight
                            WithdrawalStatus.REJECTED -> ErrorRed.copy(alpha = 0.15f)
                        }
                    ) {
                        Text(
                            text = if (isUrdu) w.status.labelUr else w.status.labelEn,
                            color = when (w.status) {
                                WithdrawalStatus.PAID -> SuccessGreen
                                WithdrawalStatus.PENDING -> WarningAmber
                                WithdrawalStatus.APPROVED -> EmeraldDark
                                WithdrawalStatus.REJECTED -> ErrorRed
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }

    // Confirmation Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Withdrawal", fontWeight = FontWeight.Bold) },
            text = {
                Text("Confirm withdrawal of PKR $amountPkr via ${selectedMethod.titleEn}? $coinsRequired coins will be held until approved.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        val res = RozKamaiState.submitWithdrawal(
                            selectedMethod,
                            amountPkr,
                            accountTitle,
                            accountNumber,
                            if (selectedMethod == PaymentMethod.BANK_TRANSFER) bankName else null
                        )
                        if (res.isSuccess) {
                            successMessage = "Withdrawal request submitted successfully! Funds held."
                        } else {
                            errorMessage = res.exceptionOrNull()?.message
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
