package com.cardlock.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cardlock.app.data.backup.BackupManager
import com.cardlock.app.data.model.CardItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    currentCards: List<CardItem>,
    onRestoreCards: (List<CardItem>) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    var exportPassword by remember { mutableStateOf("") }
    var importPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var pendingImportContent by remember { mutableStateOf<String?>(null) }

    // System File Saver for Backup Export
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                if (exportPassword.length < 6) {
                    statusMessage = "Backup password must be at least 6 characters."
                    return@rememberLauncherForActivityResult
                }
                val backupJson = BackupManager.exportBackup(currentCards, exportPassword.toCharArray())
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(backupJson.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Encrypted backup exported successfully!", Toast.LENGTH_LONG).show()
                statusMessage = "Backup exported successfully."
            } catch (e: Exception) {
                statusMessage = "Export failed: ${e.localizedMessage}"
            }
        }
    }

    // System File Picker for Backup Restore
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.use { it.readText() }
                if (content != null) {
                    pendingImportContent = content
                    statusMessage = "File selected. Enter backup password below to decrypt."
                }
            } catch (e: Exception) {
                statusMessage = "Failed to read backup file: ${e.localizedMessage}"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Encrypted Backup & Restore") },
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Export Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Export Encrypted Backup",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Generates an AES-256-GCM encrypted JSON file containing your vault cards. Protect it with a strong password.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = exportPassword,
                        onValueChange = { exportPassword = it; statusMessage = null },
                        label = { Text("Backup Password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (exportPassword.length < 6) {
                                statusMessage = "Backup password must be at least 6 characters."
                            } else {
                                exportFileLauncher.launch("cardlock_backup_${System.currentTimeMillis()}.json")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Backup File")
                    }
                }
            }

            // Restore Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Restore Vault from Backup",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Import cards from an existing encrypted Cardlock backup file.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = { importFileLauncher.launch(arrayOf("application/json", "*/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Upload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (pendingImportContent == null) "Select Backup File" else "File Selected (Tap to Change)")
                    }

                    if (pendingImportContent != null) {
                        OutlinedTextField(
                            value = importPassword,
                            onValueChange = { importPassword = it; statusMessage = null },
                            label = { Text("Enter Backup Password to Decrypt") },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                try {
                                    val restoredCards = BackupManager.importBackup(
                                        pendingImportContent!!,
                                        importPassword.toCharArray()
                                    )
                                    onRestoreCards(restoredCards)
                                    Toast.makeText(context, "Successfully restored ${restoredCards.size} cards!", Toast.LENGTH_LONG).show()
                                    pendingImportContent = null
                                    importPassword = ""
                                    statusMessage = "Vault restored successfully."
                                } catch (e: Exception) {
                                    statusMessage = "Failed to decrypt backup. Incorrect password or corrupt file."
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Decrypt & Restore Cards")
                        }
                    }
                }
            }

            if (statusMessage != null) {
                Text(
                    text = statusMessage!!,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
