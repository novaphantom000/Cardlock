package com.cardlock.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cardlock.app.data.model.CardItem
import com.cardlock.app.ui.components.CardTile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    cards: List<CardItem>,
    onCardClick: (CardItem) -> Unit,
    onAddCardClick: () -> Unit,
    onLockClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredCards = remember(cards, searchQuery) {
        if (searchQuery.isBlank()) {
            cards
        } else {
            cards.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.cardholderName.contains(searchQuery, ignoreCase = true) ||
                        it.cardBrand.contains(searchQuery, ignoreCase = true) ||
                        it.cardNumber.takeLast(4).contains(searchQuery)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cardlock Vault") },
                actions = {
                    IconButton(onClick = onLockClick) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock Vault")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCardClick) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Card")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search cards...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )

            if (filteredCards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "No cards in vault yet.\nTap + to add your first payment card." else "No matching cards found.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredCards, key = { it.id }) { card ->
                        CardTile(
                            card = card,
                            onClick = { onCardClick(card) }
                        )
                    }
                }
            }
        }
    }
}
