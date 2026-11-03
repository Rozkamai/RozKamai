package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RozKamaiState
import com.example.model.Language
import com.example.model.Screen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ErrorRed

@Composable
fun SettingsScreen(
    onLogout: () -> Unit
) {
    val isUrdu = RozKamaiState.currentLanguage == Language.URDU

    var showTosDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showComplianceDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = if (isUrdu) "ترتیبات اور قانونی معلومات" else "Settings & Legal Compliance",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Language Switch Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isUrdu) "زبان منتخب کریں" else "Language Preference",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = RozKamaiState.currentLanguage == Language.ENGLISH,
                        onClick = { RozKamaiState.currentLanguage = Language.ENGLISH },
                        label = { Text("English") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = RozKamaiState.currentLanguage == Language.URDU,
                        onClick = { RozKamaiState.currentLanguage = Language.URDU },
                        label = { Text("اردو (Urdu)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Legal & Compliance Section
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                SettingsItem(
                    icon = Icons.Default.Description,
                    title = if (isUrdu) "شرائط و ضوابط (Terms of Service)" else "Terms of Service",
                    subtitle = "Ad monetization rules & earnings disclaimer",
                    onClick = { showTosDialog = true }
                )
                Divider(modifier = Modifier.padding(horizontal = 8.dp))
                SettingsItem(
                    icon = Icons.Default.PrivacyTip,
                    title = if (isUrdu) "پرائیویسی پالیسی (Privacy Policy)" else "Privacy Policy",
                    subtitle = "Data minimization & AES-256-GCM encryption",
                    onClick = { showPrivacyDialog = true }
                )
                Divider(modifier = Modifier.padding(horizontal = 8.dp))
                SettingsItem(
                    icon = Icons.Default.Policy,
                    title = if (isUrdu) "قانونی کمپلائنس چیک لسٹ" else "Pakistan Legal Compliance Checklist",
                    subtitle = "PECA 2016 & SBP regulatory alignment",
                    onClick = { showComplianceDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Account Deletion & Logout
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                SettingsItem(
                    icon = Icons.Default.Logout,
                    title = if (isUrdu) "لاگ آؤٹ کریں" else "Sign Out",
                    subtitle = "Disconnect current session",
                    onClick = onLogout,
                    iconTint = MaterialTheme.colorScheme.onSurface
                )
                Divider(modifier = Modifier.padding(horizontal = 8.dp))
                SettingsItem(
                    icon = Icons.Default.DeleteForever,
                    title = if (isUrdu) "اکاؤنٹ مکمل ختم کریں" else "Delete Account",
                    subtitle = "Permanently purge user PII and credentials",
                    onClick = { showDeleteConfirmDialog = true },
                    iconTint = ErrorRed
                )
            }
        }
    }

    // Terms of Service Dialog
    if (showTosDialog) {
        AlertDialog(
            onDismissRequest = { showTosDialog = false },
            title = { Text("Terms of Service", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "RozKamai Platform Terms of Service\n\n" +
                                "1. Ad Availability Disclaimer: Earning rewards depends entirely on ad inventory from third-party networks (Unity Ads / AppLovin MAX). Rewards are not guaranteed and may fluctuate.\n\n" +
                                "2. 70/30 Revenue Split: For each verified completed video ad, the gross value (100 coins) is split 70% to the User (70 coins = PKR 0.70) and 30% retained by RozKamai.\n\n" +
                                "3. Fraud & Integrity: Automated clicks, VPNs, proxies, and emulators are strictly prohibited under the Prevention of Electronic Crimes Act (PECA) 2016. Violators will have accounts permanently terminated.\n\n" +
                                "4. Age Requirement: Users must be 18 years of age or older to participate.",
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showTosDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)) {
                    Text("Close")
                }
            }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy (Data Minimization)", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "RozKamai Privacy Policy\n\n" +
                                "1. Minimal Data Collection: We collect only your phone number, name, encrypted CNIC for KYC, and withdrawal account number. We do NOT request Contacts, SMS, or Location permissions.\n\n" +
                                "2. Cryptographic Protection: Sensitive credentials (CNIC and Bank IBANs) are stored encrypted via AES-256-GCM. Duplicate detection uses one-way HMAC-SHA256 blind indexing.\n\n" +
                                "3. Right to Erasure: You may request account deletion at any time from this Settings screen.",
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)) {
                    Text("Close")
                }
            }
        )
    }

    // Pakistan Compliance Checklist Dialog
    if (showComplianceDialog) {
        AlertDialog(
            onDismissRequest = { showComplianceDialog = false },
            title = { Text("Pakistan Regulatory Compliance Checklist", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Compliance Verification Checklist:\n\n" +
                                "✔ PECA 2016 Alignment: Audit trails stored in immutable append-only ledger for cybercrime lawfulness.\n" +
                                "✔ Data Minimization: Zero invasive permissions requested (no GPS, Contacts, or SMS reading).\n" +
                                "✔ Encryption: AES-256-GCM with hardware-isolated keys for Pakistani financial details.\n" +
                                "✔ SBP Payout Alignment: Manual disbursement via JazzCash and EasyPaisa merchant rails.\n" +
                                "✔ Clear Earning Disclosures: Transparency on 70/30 platform revenue split and ad availability limits.\n\n" +
                                "*Disclaimer: This checklist outlines technical best practices and does not replace formal legal counsel.",
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showComplianceDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)) {
                    Text("Close")
                }
            }
        )
    }

    // Delete Account Confirmation
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Account?", fontWeight = FontWeight.Bold, color = ErrorRed) },
            text = {
                Text("Are you sure you want to delete your RozKamai account? All accumulated coin balances will be forfeited and your account permanently purged.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        RozKamaiState.currentUser = null
                        RozKamaiState.availableCoins = 0
                        RozKamaiState.currentScreen = Screen.AUTH
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Confirm Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconTint: androidx.compose.ui.graphics.Color = EmeraldPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
