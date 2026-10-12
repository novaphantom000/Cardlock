package com.cardlock.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cardlock.app.data.model.CardItem
import com.cardlock.app.util.CardMasker
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCardScreen(
    existingCard: CardItem? = null,
    onSaveCard: (CardItem) -> Unit,
    onBackClick: () -> Unit
) {
    var title by remember { mutableStateOf(existingCard?.title ?: "") }
    var cardholderName by remember { mutableStateOf(existingCard?.cardholderName ?: "") }
    var cardNumber by remember { mutableStateOf(existingCard?.cardNumber ?: "") }
    var expiryMonth by remember { mutableStateOf(existingCard?.expiryMonth ?: "") }
    var expiryYear by remember { mutableStateOf(existingCard?.expiryYear ?: "") }
    var notes by remember { mutableStateOf(existingCard?.notes ?: "") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val detectedBrand = remember(cardNumber) {
        CardMasker.detectBrand(cardNumber)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingCard == null) "Add Payment Card" else "Edit Card") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Privacy indicator banner
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Cardlock never asks for CVV, PIN, or banking passwords. Your card data stays strictly encrypted on device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Card Nickname / Title *") },
                placeholder = { Text("e.g., Personal Checking Visa") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = cardholderName,
                onValueChange = { cardholderName = it },
                label = { Text("Cardholder Name") },
                placeholder = { Text("e.g., Jane Doe") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = cardNumber,
                onValueChange = { input ->
                    cardNumber = input.filter { it.isDigit() || it.isWhitespace() }
                },
                label = { Text("Card Number *") },
                placeholder = { Text("1234 5678 1234 5678") },
                supportingText = {
                    if (cardNumber.isNotBlank()) {
                        Text("Detected Brand: ${detectedBrand.displayName}", fontWeight = FontWeight.Medium)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = expiryMonth,
                    onValueChange = { if (it.length <= 2) expiryMonth = it.filter { c -> c.isDigit() } },
                    label = { Text("Expiry Month *") },
                    placeholder = { Text("MM") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = expiryYear,
                    onValueChange = { if (it.length <= 4) expiryYear = it.filter { c -> c.isDigit() } },
                    label = { Text("Expiry Year *") },
                    placeholder = { Text("YY or YYYY") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                placeholder = { Text("e.g., Rewards card, linked to account #123") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val cleanNum = cardNumber.filter { it.isDigit() }
                    if (title.isBlank()) {
                        errorMessage = "Please enter a title or nickname for the card."
                    } else if (cleanNum.length < 12) {
                        errorMessage = "Please enter a valid card number."
                    } else if (expiryMonth.isBlank() || expiryYear.isBlank()) {
                        errorMessage = "Please enter the card expiry month and year."
                    } else {
                        val cardToSave = CardItem(
                            id = existingCard?.id ?: UUID.randomUUID().toString(),
                            title = title.trim(),
                            cardholderName = cardholderName.trim(),
                            cardNumber = CardMasker.formatCardNumber(cleanNum),
                            expiryMonth = expiryMonth.padStart(2, '0'),
                            expiryYear = expiryYear,
                            cardBrand = detectedBrand.displayName,
                            notes = notes.trim(),
                            createdAt = existingCard?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        onSaveCard(cardToSave)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(if (existingCard == null) "Save Card to Vault" else "Update Card")
            }
        }
    }
}
