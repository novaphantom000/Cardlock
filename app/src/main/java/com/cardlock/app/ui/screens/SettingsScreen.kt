package com.cardlock.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cardlock.app.util.AutoLockManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isBiometricsAvailable: Boolean,
    isBiometricsEnabled: Boolean,
    currentAutoLockTimeoutMs: Long,
    onToggleBiometrics: (Boolean) -> Unit,
    onUpdateAutoLockTimeout: (Long) -> Unit,
    onNavigateToBackup: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Security") },
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
            // Biometrics section
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = "Biometric Unlock", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (isBiometricsAvailable) "Unlock vault using fingerprint or face" else "Biometrics not available on device",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isBiometricsEnabled,
                        onCheckedChange = { onToggleBiometrics(it) },
                        enabled = isBiometricsAvailable
                    )
                }
            }

            // Auto-Lock Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.LockClock, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = "Inactivity Auto-Lock", style = MaterialTheme.typography.titleMedium)
                    }

                    val options = listOf(
                        "30 Seconds" to AutoLockManager.TIMEOUT_30_SEC,
                        "1 Minute" to AutoLockManager.TIMEOUT_1_MIN,
                        "5 Minutes" to AutoLockManager.TIMEOUT_5_MIN
                    )

                    options.forEach { (label, timeoutMs) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentAutoLockTimeoutMs == timeoutMs,
                                onClick = { onUpdateAutoLockTimeout(timeoutMs) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Backup & Restore option
            Card(
                onClick = onNavigateToBackup,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Backup, contentDescription = null)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = "Backup & Restore", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Export or import encrypted vault backups",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Honest Security Policy & Threat Model Explanation
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Security Model & Limitations",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "• Fully Offline: Cardlock never requests internet access or communicates with external servers.\n" +
                                "• Zero CVV/PINs: Cardlock explicitly forbids storing CVV, PINs, or banking passwords.\n" +
                                "• AES-256-GCM + PBKDF2: Key derived using 120,000 PBKDF2-HMAC-SHA256 iterations.\n" +
                                "• Device Security: Protection relies on local Android OS integrity. On rooted devices or under active malware, memory compromise is possible.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
