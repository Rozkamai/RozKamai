package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RozKamaiState
import com.example.model.Language
import com.example.ui.theme.*

@Composable
fun KycScreen() {
    val isUrdu = RozKamaiState.currentLanguage == Language.URDU

    var cnicInput by remember { mutableStateOf("42101-1234567-1") }
    var otpInput by remember { mutableStateOf("123456") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val isAlreadyVerified = RozKamaiState.currentUser?.isKycVerified == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(if (isAlreadyVerified) SuccessGreen.copy(alpha = 0.15f) else EmeraldLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isAlreadyVerified) Icons.Default.CheckCircle else Icons.Default.Badge,
                contentDescription = null,
                tint = if (isAlreadyVerified) SuccessGreen else EmeraldPrimary,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isUrdu) "شناختی کارڈ (CNIC) تصدیق" else "Identity Verification (KYC)",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = if (isUrdu)
                "منی لانڈرنگ کی روک تھام اور دھوکہ دہی سے تحفظ کے لیے شناختی تصدیق لازمی ہے۔"
            else
                "Enforces PECA 2016 compliance and fraud prevention. Only 1 account allowed per CNIC nationwide.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (isAlreadyVerified) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isUrdu) "آپ کا اکاؤنٹ تصدیق شدہ ہے" else "KYC Verified Successfully",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Masked CNIC: ${RozKamaiState.currentUser?.maskedCnic ?: "42101-*******-1"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Your CNIC is encrypted with AES-256-GCM. You are fully eligible for withdrawals to JazzCash, EasyPaisa, and Pakistani Bank Accounts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    if (errorMessage != null) {
                        Text(text = errorMessage!!, color = ErrorRed, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))
                    }
                    if (successMessage != null) {
                        Text(text = successMessage!!, color = SuccessGreen, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))
                    }

                    OutlinedTextField(
                        value = cnicInput,
                        onValueChange = { cnicInput = it },
                        label = { Text(if (isUrdu) "13 ہندسوں کا CNIC نمبر" else "13-Digit Pakistani CNIC") },
                        placeholder = { Text("42101-1234567-1") },
                        leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("cnic_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = { otpInput = it },
                        label = { Text(if (isUrdu) "موبائل SMS OTP کوڈ" else "Phone SMS OTP Code") },
                        placeholder = { Text("123456") },
                        leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            errorMessage = null
                            val clean = cnicInput.replace("[^0-9]".toRegex(), "")
                            if (clean.length != 13) {
                                errorMessage = "CNIC must be exactly 13 digits (e.g. 42101-1234567-1)"
                                return@Button
                            }
                            if (otpInput != "123456") {
                                errorMessage = "Invalid OTP. Use 123456 for testing."
                                return@Button
                            }

                            val ok = RozKamaiState.submitKyc(cnicInput)
                            if (ok) {
                                successMessage = "KYC verified successfully!"
                            } else {
                                errorMessage = "Failed to verify CNIC."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text(if (isUrdu) "شناختی کارڈ تصدیق کریں" else "Verify CNIC & Unlock Withdrawals", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Cryptography and Security Guarantee Card (Section 7 & 8)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Data Minimization & Encryption Guarantee",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "• We never ask for photos of your CNIC card.\n" +
                            "• Your CNIC number is stored encrypted using AES-256-GCM with hardware-isolated keys.\n" +
                            "• Duplicate check uses a cryptographic HMAC-SHA256 blind index.\n" +
                            "• Zero storage of contacts, SMS, or location data per PECA 2016 privacy principles.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
