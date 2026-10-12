package com.cardlock.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardlock.app.data.model.CardItem
import com.cardlock.app.ui.components.CardTile
import com.cardlock.app.util.CardMasker
import com.cardlock.app.util.ClipboardHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    card: CardItem,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var isNumberRevealed by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(card.title) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEditClick) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Card")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Card", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Card visual header
            CardTile(
                card = card,
                onClick = { isNumberRevealed = !isNumberRevealed }
            )

            // Reveal/Hide Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isNumberRevealed) "Card Details Unmasked" else "Card Number Masked",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Button(
                    onClick = { isNumberRevealed = !isNumberRevealed },
                    colors = if (isNumberRevealed) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary) else ButtonDefaults.buttonColors()
                ) {
                    Icon(
                        imageVector = if (isNumberRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isNumberRevealed) "Mask Details" else "Reveal Details")
                }
            }

            Divider()

            // Card details list
            CardDetailRow(
                label = "Cardholder Name",
                value = card.cardholderName.ifBlank { "N/A" },
                onCopy = {
                    ClipboardHelper.copyToClipboard(context, "Cardholder Name", card.cardholderName)
                }
            )

            CardDetailRow(
                label = "Card Number",
                value = if (isNumberRevealed) CardMasker.formatCardNumber(card.cardNumber) else CardMasker.getMaskedCardNumber(card.cardNumber),
                isMonospace = true,
                onCopy = {
                    ClipboardHelper.copyToClipboard(context, "Card Number", card.cardNumber.filter { it.isDigit() })
                }
            )

            CardDetailRow(
                label = "Expiration Date",
                value = "${card.expiryMonth}/${card.expiryYear}",
                onCopy = {
                    ClipboardHelper.copyToClipboard(context, "Expiration Date", "${card.expiryMonth}/${card.expiryYear}")
                }
            )

            if (card.notes.isNotBlank()) {
                CardDetailRow(
                    label = "Notes",
                    value = card.notes,
                    onCopy = {
                        ClipboardHelper.copyToClipboard(context, "Notes", card.notes)
                    }
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Payment Card?") },
            text = { Text("Are you sure you want to delete '${card.title}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteClick()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CardDetailRow(
    label: String,
    value: String,
    isMonospace: Boolean = false,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
                    letterSpacing = if (isMonospace) 1.5.sp else 0.sp
                ),
                fontWeight = FontWeight.Medium
            )
        }
        IconButton(onClick = onCopy) {
            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy $label")
        }
    }
}
