package com.cardlock.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cardlock.app.data.model.CardItem
import com.cardlock.app.data.repository.VaultRepository
import com.cardlock.app.ui.screens.*
import com.cardlock.app.ui.theme.CardlockTheme
import com.cardlock.app.util.AutoLockManager
import kotlinx.coroutines.launch
import javax.crypto.Cipher

class MainActivity : FragmentActivity() {

    private lateinit var repository: VaultRepository
    private lateinit var autoLockManager: AutoLockManager

    private var currentAutoLockTimeoutMs by mutableLongStateOf(AutoLockManager.DEFAULT_TIMEOUT_MS)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Protect sensitive app switcher previews and screenshots
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        repository = VaultRepository(applicationContext)

        autoLockManager = AutoLockManager(timeoutMs = currentAutoLockTimeoutMs) {
            repository.lockVault()
        }

        setContent {
            CardlockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val isUnlocked by repository.isUnlocked.collectAsState()

                    var cards by remember { mutableStateOf<List<CardItem>>(emptyList()) }
                    var selectedCard by remember { mutableStateOf<CardItem?>(null) }
                    var isInitialized by remember { mutableStateOf(repository.isVaultInitialized()) }

                    val isBiometricsAvailable = remember {
                        val biometricManager = BiometricManager.from(applicationContext)
                        biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
                    }
                    var isBiometricsEnabled by remember { mutableStateOf(repository.isBiometricsEnabled()) }

                    LaunchedEffect(isUnlocked) {
                        if (isUnlocked) {
                            cards = repository.getAllCards()
                            autoLockManager.userInteraction()
                        } else {
                            navController.navigate("unlock") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }

                    NavHost(navController = navController, startDestination = if (isUnlocked) "vault" else "unlock") {
                        composable("unlock") {
                            UnlockScreen(
                                isInitialized = isInitialized,
                                isBiometricsAvailable = isBiometricsAvailable && isBiometricsEnabled,
                                onSetupVault = { password ->
                                    if (repository.setupVault(password)) {
                                        isInitialized = true
                                        navController.navigate("vault") {
                                            popUpTo("unlock") { inclusive = true }
                                        }
                                    }
                                },
                                onUnlockWithPassword = { password ->
                                    val success = repository.unlockWithPassword(password)
                                    if (success) {
                                        navController.navigate("vault") {
                                            popUpTo("unlock") { inclusive = true }
                                        }
                                    }
                                    success
                                },
                                onUnlockWithBiometrics = {
                                    val cipher = repository.getBiometricCipherForUnlock()
                                    if (cipher != null) {
                                        showBiometricPrompt(cipher) { authenticatedCipher ->
                                            if (repository.unlockWithAuthenticatedBiometricCipher(authenticatedCipher)) {
                                                navController.navigate("vault") {
                                                    popUpTo("unlock") { inclusive = true }
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        composable("vault") {
                            VaultScreen(
                                cards = cards,
                                onCardClick = { card ->
                                    selectedCard = card
                                    navController.navigate("card_detail")
                                },
                                onAddCardClick = {
                                    selectedCard = null
                                    navController.navigate("add_edit_card")
                                },
                                onLockClick = {
                                    repository.lockVault()
                                },
                                onSettingsClick = {
                                    navController.navigate("settings")
                                }
                            )
                        }

                        composable("card_detail") {
                            selectedCard?.let { card ->
                                CardDetailScreen(
                                    card = card,
                                    onEditClick = {
                                        navController.navigate("add_edit_card")
                                    },
                                    onDeleteClick = {
                                        lifecycleScope.launch {
                                            repository.deleteCard(card.id)
                                            cards = repository.getAllCards()
                                            navController.popBackStack()
                                        }
                                    },
                                    onBackClick = { navController.popBackStack() }
                                )
                            }
                        }

                        composable("add_edit_card") {
                            AddEditCardScreen(
                                existingCard = selectedCard,
                                onSaveCard = { cardToSave ->
                                    lifecycleScope.launch {
                                        repository.saveCard(cardToSave)
                                        cards = repository.getAllCards()
                                        navController.popBackStack()
                                    }
                                },
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                isBiometricsAvailable = isBiometricsAvailable,
                                isBiometricsEnabled = isBiometricsEnabled,
                                currentAutoLockTimeoutMs = currentAutoLockTimeoutMs,
                                onToggleBiometrics = { enabled ->
                                    if (enabled) {
                                        val cipher = repository.getBiometricCipherForEnable()
                                        if (cipher != null) {
                                            showBiometricPrompt(cipher) { authenticatedCipher ->
                                                if (repository.enableBiometricsWithAuthenticatedCipher(authenticatedCipher)) {
                                                    isBiometricsEnabled = true
                                                }
                                            }
                                        }
                                    } else {
                                        repository.disableBiometrics()
                                        isBiometricsEnabled = false
                                    }
                                },
                                onUpdateAutoLockTimeout = { newTimeout ->
                                    currentAutoLockTimeoutMs = newTimeout
                                    autoLockManager.updateTimeout(newTimeout)
                                },
                                onNavigateToBackup = {
                                    navController.navigate("backup_restore")
                                },
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        composable("backup_restore") {
                            BackupRestoreScreen(
                                currentCards = cards,
                                onRestoreCards = { restoredCards ->
                                    lifecycleScope.launch {
                                        restoredCards.forEach { repository.saveCard(it) }
                                        cards = repository.getAllCards()
                                    }
                                },
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (::autoLockManager.isInitialized) {
            autoLockManager.userInteraction()
        }
    }

    override fun onStop() {
        super.onStop()
        if (::autoLockManager.isInitialized) {
            autoLockManager.stop()
        }
    }

    private fun showBiometricPrompt(cipher: Cipher, onSuccess: (Cipher) -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val authenticatedCipher = result.cryptoObject?.cipher ?: cipher
                    onSuccess(authenticatedCipher)
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Cardlock Vault")
            .setSubtitle("Authenticate using your biometric credential")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }
}
