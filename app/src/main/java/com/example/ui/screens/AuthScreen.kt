package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RozKamaiState
import com.example.model.Language
import com.example.model.User
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val isUrdu = RozKamaiState.currentLanguage == Language.URDU
    var isRegisterMode by remember { mutableStateOf(false) }

    var phone by remember { mutableStateOf("03001234567") }
    var password by remember { mutableStateOf("UserSecret123!") }
    var fullName by remember { mutableStateOf("Hamza Ali") }
    var is18Plus by remember { mutableStateOf(true) }
    var isTermsAccepted by remember { mutableStateOf(true) }
    var passwordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showOtpDialog by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("123456") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Branding
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(GoldAccent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "RozKamai",
                tint = EmeraldPrimary,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isRegisterMode)
                (if (isUrdu) "نیا اکاؤنٹ بنائیں" else "Create RozKamai Account")
            else
                (if (isUrdu) "اپنے اکاؤنٹ میں لاگ ان کریں" else "Sign In to RozKamai"),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = if (isUrdu) "اشتہارات دیکھیں اور روزانہ PKR کمائیں" else "Watch ads daily and withdraw via JazzCash / EasyPaisa",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // Error Banner
        if (errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = errorMessage!!, color = ErrorRed, fontSize = 13.sp)
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Full Name (Only Register)
                if (isRegisterMode) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text(if (isUrdu) "مکمل نام" else "Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Phone Number
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(if (isUrdu) "پاکستانی موبائل نمبر (03XXXXXXXXX)" else "Pakistani Mobile Number") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    placeholder = { Text("03001234567") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("phone_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (isUrdu) "پاس ورڈ" else "Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // 18+ and Terms Declarations (Only Register)
                if (isRegisterMode) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { is18Plus = !is18Plus }
                    ) {
                        Checkbox(
                            checked = is18Plus,
                            onCheckedChange = { is18Plus = it },
                            colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                        )
                        Text(
                            text = if (isUrdu) "میں تصدیق کرتا ہوں کہ میری عمر 18 سال یا اس سے زیادہ ہے۔" else "I declare that I am 18 years of age or older.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isTermsAccepted = !isTermsAccepted }
                    ) {
                        Checkbox(
                            checked = isTermsAccepted,
                            onCheckedChange = { isTermsAccepted = it },
                            colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                        )
                        Text(
                            text = if (isUrdu) "میں RozKamai کی شرائط و ضوابط اور پرائیویسی پالیسی سے متفق ہوں۔" else "I agree to Terms of Service & Privacy Policy.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        errorMessage = null
                        val cleanPhone = phone.trim().replace("-", "")
                        if (!cleanPhone.startsWith("03") || cleanPhone.length != 11) {
                            errorMessage = if (isUrdu) "درست 11 ہندسوں کا موبائل نمبر درج کریں (03XXXXXXXXX)" else "Enter a valid 11-digit Pakistani phone number (03XXXXXXXXX)"
                            return@Button
                        }
                        if (password.length < 6) {
                            errorMessage = if (isUrdu) "پاس ورڈ کم از کم 6 حروف پر مشتمل ہونا چاہیے" else "Password must be at least 6 characters"
                            return@Button
                        }

                        if (isRegisterMode) {
                            if (!is18Plus) {
                                errorMessage = if (isUrdu) "اکاؤنٹ بنانے کے لیے 18+ تصدیق لازمی ہے" else "18+ declaration is mandatory for registration"
                                return@Button
                            }
                            if (!isTermsAccepted) {
                                errorMessage = if (isUrdu) "شرائط قبول کرنا ضروری ہے" else "You must accept Terms of Service"
                                return@Button
                            }
                            showOtpDialog = true
                        } else {
                            // Login directly
                            RozKamaiState.currentUser = User(
                                id = "usr_pk_001",
                                phone = cleanPhone,
                                fullName = fullName.ifEmpty { "Hamza Ali" },
                                isAgeVerified = true,
                                isKycVerified = true,
                                maskedCnic = "42101-*******-1"
                            )
                            onAuthSuccess()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("auth_submit_button")
                ) {
                    Text(
                        text = if (isRegisterMode)
                            (if (isUrdu) "مفت اکاؤنٹ بنائیں" else "Create Free Account")
                        else
                            (if (isUrdu) "لاگ ان کریں" else "Sign In"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = {
                        errorMessage = null
                        isRegisterMode = !isRegisterMode
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isRegisterMode)
                            (if (isUrdu) "پہلے سے اکاؤنٹ موجود ہے؟ لاگ ان کریں" else "Already have an account? Sign In")
                        else
                            (if (isUrdu) "نیا اکاؤنٹ بنانا چاہتے ہیں؟ رجسٹریشن کریں" else "Don't have an account? Register"),
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // SMS OTP Verification Dialog
    if (showOtpDialog) {
        AlertDialog(
            onDismissRequest = { showOtpDialog = false },
            title = { Text(if (isUrdu) "SMS OTP کی تصدیق" else "Verify SMS OTP", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = if (isUrdu)
                            "ہم نے آپ کے نمبر $phone پر ایک تصدیقی کوڈ بھیجا ہے۔ ٹیسٹنگ کے لیے 123456 درج کریں۔"
                        else
                            "We sent a 6-digit verification code to $phone. For dev/testing, code is 123456.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { otpCode = it },
                        label = { Text("6-Digit OTP") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (otpCode == "123456") {
                            showOtpDialog = false
                            RozKamaiState.currentUser = User(
                                id = "usr_pk_${System.currentTimeMillis()}",
                                phone = phone,
                                fullName = fullName,
                                isAgeVerified = true,
                                isKycVerified = false,
                                maskedCnic = null
                            )
                            onAuthSuccess()
                        } else {
                            errorMessage = "Invalid OTP code. Enter 123456 in dev mode."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(if (isUrdu) "تصدیق کریں" else "Verify & Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOtpDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
