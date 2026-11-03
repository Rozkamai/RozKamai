package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.components.BannerAdView
import com.example.components.format
import com.example.data.RozKamaiState
import com.example.model.Language
import com.example.model.LedgerEntry
import com.example.model.LedgerType
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    val isUrdu = RozKamaiState.currentLanguage == Language.URDU

    var selectedFilter by remember { mutableStateOf("ALL") }
    var selectedEntry by remember { mutableStateOf<LedgerEntry?>(null) }

    val filteredEntries = when (selectedFilter) {
        "REWARDS" -> RozKamaiState.ledgerEntries.filter { it.type == LedgerType.REWARD_USER_70 }
        "WITHDRAWALS" -> RozKamaiState.ledgerEntries.filter { it.type == LedgerType.WITHDRAWAL_HOLD || it.type == LedgerType.WITHDRAWAL_PAID || it.type == LedgerType.WITHDRAWAL_REFUND }
        else -> RozKamaiState.ledgerEntries
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = if (isUrdu) "آمدنی اور ٹرانزیکشن ہسٹری" else "Transaction & Ledger Audit",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = if (isUrdu) "ہر سکے کی نقل و حرکت کا مکمل محفوظ ریکارڈ" else "Tamper-evident append-only ledger entries",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text(if (isUrdu) "تمام" else "All") }
                )
                FilterChip(
                    selected = selectedFilter == "REWARDS",
                    onClick = { selectedFilter = "REWARDS" },
                    label = { Text(if (isUrdu) "اشتہارات (70%)" else "Ad Rewards (70%)") }
                )
                FilterChip(
                    selected = selectedFilter == "WITHDRAWALS",
                    onClick = { selectedFilter = "WITHDRAWALS" },
                    label = { Text(if (isUrdu) "ادائیگیاں" else "Withdrawals") }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isUrdu) "کوئی ٹرانزیکشن موجود نہیں ہے" else "No transactions found",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize().testTag("ledger_list")
                ) {
                    items(filteredEntries) { entry ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedEntry = entry }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (entry.type.isCredit) SuccessGreen.copy(alpha = 0.12f)
                                                else ErrorRed.copy(alpha = 0.12f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (entry.type.isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = if (entry.type.isCredit) SuccessGreen else ErrorRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = if (isUrdu) entry.type.labelUr else entry.type.labelEn,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = entry.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = entry.timestamp,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${if (entry.type.isCredit) "+" else ""}${entry.amountCoins} coins",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (entry.type.isCredit) SuccessGreen else ErrorRed
                                    )
                                    Text(
                                        text = "PKR ${entry.amountPkr.format(2)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Bottom Sheet
    selectedEntry?.let { entry ->
        ModalBottomSheet(onDismissRequest = { selectedEntry = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Ledger Audit Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                DetailRow("Transaction ID", entry.id)
                DetailRow("Entry Type", entry.type.name)
                DetailRow("Amount Coins", "${entry.amountCoins} coins")
                DetailRow("PKR Equivalent", "PKR ${entry.amountPkr.format(2)}")
                DetailRow("Balance After", "${entry.balanceAfterCoins} coins")
                DetailRow("Reference Session", entry.referenceId)
                DetailRow("Audit Description", entry.description)
                DetailRow("Timestamp", entry.timestamp)

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { selectedEntry = null },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}
