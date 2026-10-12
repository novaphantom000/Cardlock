# Cardlock

Cardlock is a privacy-first, fully offline Android payment-card vault built with Kotlin, Jetpack Compose, and Material 3 Expressive.

## Features

- **Card Vault Management**: Add, edit, delete, search, and organize payment cards (Visa, Mastercard, Amex, Discover, etc.).
- **Zero Highly-Sensitive Data Policy**: Cardlock **never** requests or stores CVV/CVC, PINs, banking passwords, or OTPs.
- **Privacy Masking**: Card numbers are masked by default (`•••• •••• •••• 1234`) and require explicit user action to reveal.
- **Safe Clipboard**: Copy details with warnings and automatic best-effort 30-second clipboard clearing.
- **Locking & Security**: Master-password unlock, optional biometric unlock (Android Keystore wrapped key), inactivity auto-lock, and manual locking.
- **Encrypted Local Backup & Restore**: Export and import password-derived AES-256-GCM encrypted JSON backups.
- **Fully Offline & Privacy Guarded**: No internet permission in `AndroidManifest.xml`, no analytics, cloud sync, ads, or third-party tracking.
- **Screenshot & Preview Protection**: `FLAG_SECURE` window setting prevents app switcher leaks and screenshot captures.

---

## Security Architecture & Key Derivation

1. **Master Password Verification & Key Derivation**:
   - Uses **PBKDF2-HMAC-SHA256** with **120,000 iterations** and a cryptographically secure 16-byte random salt to derive a 256-bit AES master key.
   - Never stores the master password in plain text or preferences. Verification uses an encrypted verification magic token (`CARDLOCK_VERIFY_V1`).

2. **Authenticated Data Encryption**:
   - Vault payloads (stored in local SQLite via Room) are encrypted with **AES-256-GCM** (Galois/Counter Mode).
   - Provides both confidentiality and authenticity/integrity checks.

3. **Biometric Integration**:
   - Biometric unlock uses the **Android Keystore** system to generate an AES-256 key requiring biometric authentication (`AUTH_BIOMETRIC_STRONG` / `AUTH_BIOMETRIC_WEAK`).
   - The master key is wrapped and decrypted in hardware-backed storage upon successful biometric authentication.

---

## Honest Security & Threat Model Limitations

Cardlock adheres to transparent security practices and does **NOT** claim absolute security:

- **Offline Operating Assumption**: Cardlock assumes the physical device is under the control of the user.
- **Rooted Devices & Malware**: If the device is rooted or compromised by low-level malware/keyloggers, memory or screen contents can potentially be inspected when unlocked.
- **Backup Password Dependence**: Encrypted backups are as strong as the password chosen during export. Weak passwords can be vulnerable to offline brute-force attacks.
- **No Cloud Recovery**: Because Cardlock is 100% offline and stores no credentials on external servers, if you lose your master password and have no backup, vault recovery is mathematically impossible.

---

## Building and Testing

### Build Requirements
- JDK 17 or JDK 21
- Android SDK 34 / 35

### Running Unit Tests
```bash
./gradlew test
```

### Building Debug APK
```bash
./gradlew assembleDebug
```
The APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.
