# Vaultly

> **Decentralized Password Manager for Android** — your credentials, secured by blockchain and encrypted on IPFS.

Vaultly is an Android application that lets users store passwords and secure notes in an AES-GCM encrypted vault that lives on IPFS (via Pinata) and is anchored to the Polygon Amoy blockchain through a smart contract. Access is gated by wallet authentication (WalletConnect / Reown AppKit) and an optional biometric lock screen.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Tech Stack & Prerequisites](#2-tech-stack--prerequisites)
3. [Repository Map](#3-repository-map)
4. [Local Development Quickstart](#4-local-development-quickstart)
5. [Configuration](#5-configuration)
6. [Common Workflows](#6-common-workflows)
7. [Deployment Playbook](#7-deployment-playbook)
8. [Operations Runbook](#8-operations-runbook)
9. [Troubleshooting](#9-troubleshooting)
10. [Security](#10-security)
11. [Contributing](#11-contributing)
12. [License & Acknowledgements](#12-license--acknowledgements)

---

## 1. Project Overview

### What Is Vaultly?

Vaultly is a **non-custodial, decentralized credential manager** targeting Android users who want full ownership of their secrets without relying on a centralized server. Credentials never leave the device unencrypted — the vault blob is encrypted with AES-GCM using a key derived from the user's wallet signature via HKDF, then uploaded to IPFS through Pinata, and the resulting Content Identifier (CID) is stored on-chain.

### Who Is It For?

- Web3 / crypto-native users who already hold a self-custodial wallet.
- Privacy-focused individuals who distrust centralized password-manager vendors.
- Developers learning decentralized storage patterns on Android.

### Key Features

| Feature | Description |
|---|---|
| **Blockchain-anchored vault** | CID stored on Polygon Amoy via a Solidity smart contract |
| **IPFS storage** | Encrypted vault blob pinned with Pinata |
| **AES-GCM encryption** | 256-bit key derived from wallet ECDSA signature using HKDF |
| **WalletConnect / Reown AppKit** | Wallet connection, signature request, one-click login |
| **Biometric lock screen** | Fingerprint / face unlock to re-enter the app after backgrounding |
| **Autofill service** | System-level autofill integration with biometric pre-authentication |
| **Password & Notes** | Two credential types — structured passwords and freeform secure notes |
| **Search & filter** | Real-time debounced search across website, username, and note fields |
| **Dark / Light / Dynamic theme** | Material You with a custom gold+dark-brown brand palette |
| **Screenshot protection** | `FLAG_SECURE` prevents screenshots and recent-app previews |
| **Secure clipboard** | Android 13+ sensitive flag on copied passwords |

---

## 2. Tech Stack & Prerequisites

### Technology Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Kotlin | 2.2.20 |
| UI Framework | Jetpack Compose + Material 3 | BOM 2025.09.00 |
| Architecture | MVVM + Clean Architecture | — |
| Dependency Injection | Dagger Hilt | 2.57.1 |
| Local Database | Room (SQLite) | 2.8.0 |
| HTTP Client | Retrofit + OkHttp | 2.9.0 |
| JSON | Gson | 3.0.0 |
| Blockchain | Web3j | 4.9.4 |
| Wallet Integration | Reown AppKit (WalletConnect) | 1.4.11 |
| Decentralized Storage | Pinata (IPFS) | REST API |
| Encryption | Android Keystore + AES-GCM / HKDF | — |
| Biometric | AndroidX Biometric | 1.1.0 |
| Navigation | Compose Navigation | 2.9.6 |
| Build System | Gradle KTS | AGP 8.11.2 |
| Testing | JUnit 4 + Espresso | 4.13.2 / 3.7.0 |

### Prerequisites

| Tool | Required Version | Install |
|---|---|---|
| **Android Studio** | Ladybug (2024.2.x) or newer | [developer.android.com/studio](https://developer.android.com/studio) |
| **JDK** | 11 (bundled with Android Studio) | — |
| **Android SDK** | API 27–36 | SDK Manager in Android Studio |
| **Kotlin** | 2.2.20 (managed by Gradle) | Bundled |
| **Git** | 2.x | [git-scm.com](https://git-scm.com) |
| **A WalletConnect-compatible wallet** | MetaMask, Rainbow, etc. | Device / Emulator |

> **Network access:** The app calls Pinata (IPFS), PolygonScan, and a custom IPFS gateway. Ensure these are reachable from your build/test environment.

---

## 3. Repository Map

```
Vaultly/
├── app/
│   ├── assets/
│   │   └── MyContractABI.json          # Solidity ABI for the on-chain vault contract
│   ├── build.gradle.kts                # App-level Gradle config (deps, SDK versions, signing)
│   ├── proguard-rules.pro              # ProGuard / R8 obfuscation rules
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml     # Permissions, activities, autofill service declaration
│       │   └── java/com/dapp/vaultly/
│       │       ├── MainActivity.kt         # Compose host, AppKit init, FLAG_SECURE
│       │       ├── VaultlyApp.kt           # Nav graph, bottom nav, search bar composable
│       │       ├── VaultlyApplication.kt   # Hilt Application class
│       │       ├── autofill/
│       │       │   ├── VaultlyAutofillService.kt              # Android AutofillService impl
│       │       │   └── ui/
│       │       │       └── AuthenticateBeforeAutofillActivity.kt  # Biometric gate for autofill
│       │       ├── data/
│       │       │   ├── local/
│       │       │   │   ├── VaultlyDatabase.kt      # Room DB with two tables
│       │       │   │   ├── CredentialsDao.kt        # CRUD DAO for credentials
│       │       │   │   ├── UserVaultDao.kt          # DAO for vault metadata
│       │       │   │   ├── CredentialEntity.kt      # Stored: website, CID, encryptedBlob
│       │       │   │   ├── UserVaultEntity.kt       # Vault metadata entity
│       │       │   │   ├── SecureStorage.kt         # Android Keystore wrapper (EncryptedSharedPrefs)
│       │       │   │   ├── AesKeyStorage.kt         # Stores derived AES key securely
│       │       │   │   └── ThemePreferences.kt      # DataStore for theme preference
│       │       │   ├── remote/
│       │       │   │   ├── PinataApiService.kt      # Retrofit interface – Pinata IPFS
│       │       │   │   ├── PolygonApiService.kt     # Retrofit interface – PolygonScan RPC
│       │       │   │   └── IpfsGatewayService.kt   # Retrofit interface – IPFS gateway GET
│       │       │   ├── repository/
│       │       │   │   ├── CredentialRepository.kt       # Local CRUD + sync
│       │       │   │   ├── UserVaultRepository.kt        # Vault encrypt/decrypt, Pinata, IPFS
│       │       │   │   ├── PolygonRepository.kt          # getCID from smart contract
│       │       │   │   └── VaultlyAutofillRepository.kt  # Autofill credential cache
│       │       │   └── model/
│       │       │       ├── Credential.kt            # Core domain model + CredentialType enum
│       │       │       ├── AddPasswordUiState.kt    # Form state for add/edit screen
│       │       │       ├── DashboardUiState.kt      # Dashboard list + search state
│       │       │       ├── WalletUiState.kt         # Wallet connection states (sealed class)
│       │       │       ├── VaultlyRoutes.kt         # Navigation route enum
│       │       │       ├── PinataModels.kt          # Request/response models for Pinata
│       │       │       ├── PolygonResponse.kt       # RPC response wrapper
│       │       │       └── VaultlyTheme.kt          # Theme enum
│       │       ├── di/
│       │       │   └── VaultlyModule.kt             # Hilt @Module — DB, Retrofit, SecureStorage
│       │       ├── ui/
│       │       │   ├── screens/
│       │       │   │   ├── DashboardScreen.kt       # Credential list, search, filter
│       │       │   │   ├── AddPasswordScreen.kt     # Add / edit credential bottom sheet
│       │       │   │   ├── ProfileScreen.kt         # Settings: theme, autofill, logout
│       │       │   │   ├── LockScreen.kt            # Biometric lock / PIN fallback
│       │       │   │   ├── WelcomeScreen.kt         # Wallet connection entry point
│       │       │   │   ├── SplashScreen.kt          # Animated app intro
│       │       │   │   └── CustomComponents.kt      # VaultCard, buttons, shared UI
│       │       │   ├── viewmodels/
│       │       │   │   ├── AuthViewmodel.kt         # Wallet state, signature, key derivation
│       │       │   │   ├── DashboardViewmodel.kt    # Credential CRUD, search, autofill sync
│       │       │   │   ├── AddPasswordViewmodel.kt  # Form state, validation, save
│       │       │   │   ├── LockViewModel.kt         # Lock/unlock lifecycle
│       │       │   │   ├── VaultlyThemeViewmodel.kt # Theme switching
│       │       │   │   └── AutofillSettingsViewmodel.kt  # Autofill enable/disable
│       │       │   └── theme/
│       │       │       ├── Color.kt                 # Brand palette (Gold #FFD700, Dark Brown #1E1B16)
│       │       │       ├── Theme.kt                 # Material 3 theme + dynamic color
│       │       │       └── Type.kt                  # Typography
│       │       └── util/
│       │           ├── Constants.kt                 # API endpoints, contract address, JWT, API key
│       │           ├── CryptoUtil.kt                # AES-GCM encrypt/decrypt, HKDF key derivation
│       │           ├── BiometricAuth.kt             # BiometricPrompt wrapper
│       │           ├── ClipboardUtil.kt             # Secure clipboard (sensitive flag)
│       │           └── PinHashUtil.kt               # PIN hashing utility
│       ├── test/                                    # Unit tests (JVM)
│       └── androidTest/                             # Instrumented tests (device/emulator)
├── gradle/
│   ├── libs.versions.toml                           # Gradle version catalog
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── build.gradle.kts                                 # Root Gradle config (plugin declarations)
├── settings.gradle.kts                              # Module inclusion, repo URLs
├── gradle.properties                                # JVM args, AndroidX flags
├── gradlew / gradlew.bat                            # Gradle wrapper scripts
└── *.md                                             # Developer documentation (autofill fixes, impl notes)
```

---

## 4. Local Development Quickstart

### Step 1 — Clone

```bash
git clone https://github.com/kaleem04/Vaultly.git
cd Vaultly
```

### Step 2 — Open in Android Studio

1. Launch Android Studio.
2. **File → Open** → select the `Vaultly` directory.
3. Wait for Gradle sync to complete. Android Studio will download all dependencies automatically.

> Gradle dependencies are resolved from `google()`, `mavenCentral()`, and `https://jitpack.io`. Ensure outbound internet access is available on your machine.

### Step 3 — Configure API Keys and Secrets

> ⚠️ **Do not commit real secrets to source control.** See [Section 5 — Configuration](#5-configuration) for the recommended approach.

Before building, open `app/src/main/java/com/dapp/vaultly/util/Constants.kt` and replace the placeholder values with your own credentials:

| Constant | Purpose | Where to Get It |
|---|---|---|
| `JWT_TOKEN` | Pinata JWT for IPFS pinning | [app.pinata.cloud](https://app.pinata.cloud) → API Keys |
| `API_KEY` | PolygonScan API key | [polygonscan.com](https://polygonscan.com) → My API Keys |
| `CONTRACT_ADDRESS` | Deployed vault smart contract | Your deployment (see §7) |
| `IPFS_URL` | Your Pinata dedicated gateway | Pinata → Gateways |

### Step 4 — Connect a Device or Start an Emulator

- **Physical device (recommended):** Enable USB debugging on an Android 8.1+ (API 27+) device and connect via USB.
- **Emulator:** Create an AVD in the AVD Manager (Pixel 6, API 34+ recommended) and start it.

### Step 5 — Build and Run

Click **Run ▶** in Android Studio, or use the Gradle wrapper from the terminal:

```bash
# Debug build and install on connected device/emulator
./gradlew installDebug
```

### Step 6 — First Launch

1. The app opens to the **Splash → Welcome** screen.
2. Tap **Connect Wallet** — the Reown AppKit modal appears.
3. Use a WalletConnect-compatible wallet (MetaMask, Rainbow, etc.) to scan the QR code.
4. Approve the **Sign Message** request — this signature is used to derive your AES encryption key.
5. The **Dashboard** loads with your (empty) vault.

### Step 7 — Run Tests

```bash
# Unit tests (JVM)
./gradlew test

# Instrumented tests (requires connected device or emulator)
./gradlew connectedAndroidTest
```

### Step 8 — Lint

```bash
./gradlew lint
```

Lint reports are generated at `app/build/reports/lint-results-debug.html`.

---

## 5. Configuration

### Environment Variables / Secrets

Vaultly currently stores its configuration in `Constants.kt` at compile time. The recommended migration path is to use Gradle `buildConfigField` entries sourced from local properties so secrets are never committed.

#### Recommended `local.properties` Setup

Add the following to your `local.properties` file (this file is already git-ignored by Android's default `.gitignore`):

```properties
# local.properties — NEVER commit this file
pinata.jwt=eyJhbGci...your_jwt_here
polygonscan.api_key=YOUR_POLYGONSCAN_API_KEY
pinata.gateway_url=https://your-gateway.mypinata.cloud/
contract.address=0xYourContractAddress
```

Then in `app/build.gradle.kts`, read these at build time:

```kotlin
import java.util.Properties

val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

android {
    defaultConfig {
        buildConfigField("String", "PINATA_JWT", "\"${localProps["pinata.jwt"] ?: ""}\"")
        buildConfigField("String", "POLYGON_API_KEY", "\"${localProps["polygonscan.api_key"] ?: ""}\"")
        buildConfigField("String", "PINATA_GATEWAY_URL", "\"${localProps["pinata.gateway_url"] ?: ""}\"")
        buildConfigField("String", "CONTRACT_ADDRESS", "\"${localProps["contract.address"] ?: ""}\"")
    }
}
```

Then replace hardcoded values in `Constants.kt`:

```kotlin
const val JWT_TOKEN = BuildConfig.PINATA_JWT
const val API_KEY = BuildConfig.POLYGON_API_KEY
```

#### `.env.example`

Create a `.env.example` file at the repository root to document required keys for new contributors:

```
# .env.example — copy to local.properties and fill in your values

# Pinata IPFS — https://app.pinata.cloud → API Keys
pinata.jwt=<YOUR_PINATA_JWT_TOKEN>

# Pinata dedicated gateway (or public: https://gateway.pinata.cloud/)
pinata.gateway_url=https://<YOUR_SUBDOMAIN>.mypinata.cloud/

# PolygonScan API key — https://polygonscan.com → My API Keys
polygonscan.api_key=<YOUR_POLYGONSCAN_API_KEY>

# Deployed smart contract address on Polygon Amoy testnet
contract.address=0x<YOUR_CONTRACT_ADDRESS>
```

### Smart Contract

The `CONTRACT_ADDRESS` in `Constants.kt` points to a Solidity contract on **Polygon Amoy testnet** that implements (at minimum):

```solidity
function setCID(string calldata cid) external;
function getCID(address user) external view returns (string memory);
```

The ABI is stored in `app/assets/MyContractABI.json`. To re-deploy on a new network, update both the ABI file and `CONTRACT_ADDRESS`.

### WalletConnect Project ID

The Reown AppKit requires a WalletConnect **Project ID** registered at [cloud.reown.com](https://cloud.reown.com). Locate the initialization call in `MainActivity.kt` and supply the Project ID via `BuildConfig` (same pattern as above).

---

## 6. Common Workflows

### 6.1 Adding a New Feature

```bash
# 1. Create a feature branch from main
git checkout -b feature/your-feature-name

# 2. Develop, then build
./gradlew assembleDebug

# 3. Run tests
./gradlew test

# 4. Lint
./gradlew lint

# 5. Open a PR (see §11 — Contributing)
```

### 6.2 Adding a New Screen

1. Create a new composable file in `ui/screens/`.
2. Add a route entry to the `VaultlyRoutes` enum in `data/model/VaultlyRoutes.kt`.
3. Add the `composable(VaultlyRoutes.YOUR_SCREEN.route) { ... }` block to the `NavHost` in `VaultlyApp.kt`.
4. Add bottom-nav or top-nav entry if applicable.

### 6.3 Adding a New Credential Field

1. Update `Credential.kt` and `AddPasswordUiState.kt` with the new field.
2. Update the Room entity (`CredentialEntity.kt`) and increment the DB version in `VaultlyDatabase.kt` (add a `Migration`).
3. Update the DAO (`CredentialsDao.kt`) queries as needed.
4. Update `AddPasswordScreen.kt` and `AddPasswordViewmodel.kt` to expose the field in the UI.
5. Update `UserVaultRepository.kt` (serialization/deserialization of the vault JSON).

### 6.4 Database Migrations

Room migrations are defined in `VaultlyDatabase.kt`. Every schema change **must** be accompanied by a migration:

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE credentials ADD COLUMN newField TEXT NOT NULL DEFAULT ''")
    }
}

Room.databaseBuilder(context, VaultlyDatabase::class.java, "vaultly_db")
    .addMigrations(MIGRATION_1_2)
    .build()
```

### 6.5 Updating Dependencies

1. Edit version numbers in `gradle/libs.versions.toml` or directly in `app/build.gradle.kts`.
2. Run `./gradlew dependencies` to check the resolved dependency tree.
3. Run `./gradlew test` and `./gradlew connectedAndroidTest` to verify nothing broke.

### 6.6 Deploying the Smart Contract

> Prerequisites: Node.js, Hardhat or Foundry, a funded Polygon Amoy wallet.

```bash
# Example with Hardhat (adapt to your toolchain)
cd contracts/          # your smart contract project
npx hardhat compile
npx hardhat run scripts/deploy.js --network amoy
```

After deployment:
- Copy the deployed address into `Constants.kt` (`CONTRACT_ADDRESS`).
- Copy the new ABI JSON into `app/assets/MyContractABI.json`.

---

## 7. Deployment Playbook

### 7.1 Build Variants

| Variant | Command | Purpose |
|---|---|---|
| Debug | `./gradlew assembleDebug` | Development, internal testing |
| Release | `./gradlew assembleRelease` | Production / Play Store |

### 7.2 Signing the Release APK

1. Generate a keystore (one-time):

```bash
keytool -genkeypair -v \
  -keystore vaultly-release.jks \
  -alias vaultly \
  -keyalg RSA -keysize 2048 \
  -validity 10000
```

2. Add signing config to `app/build.gradle.kts`:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file(localProps["keystore.path"] as String)
        storePassword = localProps["keystore.password"] as String
        keyAlias = localProps["keystore.alias"] as String
        keyPassword = localProps["key.password"] as String
    }
}
buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        isMinifyEnabled = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
}
```

3. Add keystore properties to `local.properties` (never commit).

4. Build the signed release APK:

```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

### 7.3 CI/CD

> There is currently **no CI/CD pipeline** configured in this repository. The steps below are a recommended starting point using GitHub Actions.

Create `.github/workflows/android-ci.yml`:

```yaml
name: Android CI

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '11'
          distribution: 'temurin'
      - name: Write secrets to local.properties
        run: |
          echo "pinata.jwt=${{ secrets.PINATA_JWT }}" >> local.properties
          echo "polygonscan.api_key=${{ secrets.POLYGONSCAN_API_KEY }}" >> local.properties
          echo "pinata.gateway_url=${{ secrets.PINATA_GATEWAY_URL }}" >> local.properties
          echo "contract.address=${{ secrets.CONTRACT_ADDRESS }}" >> local.properties
      - name: Build debug APK
        run: ./gradlew assembleDebug
      - name: Run unit tests
        run: ./gradlew test
      - name: Upload APK artifact
        uses: actions/upload-artifact@v4
        with:
          name: debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk
```

### 7.4 Publishing to Google Play

1. Build a signed release AAB: `./gradlew bundleRelease`
2. Output: `app/build/outputs/bundle/release/app-release.aab`
3. Upload to the [Google Play Console](https://play.google.com/console).
4. Fill out store listing, screenshots, and content rating.
5. Submit for review.

### 7.5 Rollback Strategy

- **Android app:** Google Play supports rolling back to a previous release via the Play Console → Release → rollout controls.
- **Smart contract:** Solidity contracts are immutable once deployed. Deploy a new version and update `CONTRACT_ADDRESS` in a new app release.
- **IPFS data:** IPFS is content-addressed and immutable. Vaultly stores the CID on-chain, so rolling back means re-pinning an older CID and updating the on-chain record via `setCID`.

---

## 8. Operations Runbook

### 8.1 Logging

The app uses Android's standard `Log` class (`android.util.Log`). Key log tags:

| Tag | Component |
|---|---|
| `Vaultly` / `VaultlyApp` | General app lifecycle |
| `AuthViewModel` | Wallet connection, key derivation |
| `DashboardViewModel` | Credential loading/saving |
| `VaultlyAutofillService` | Autofill request/response |
| `UserVaultRepository` | IPFS upload/download, encryption |
| `PolygonRepository` | On-chain CID read |

View logs in real time with ADB:

```bash
adb logcat -s "Vaultly" "AuthViewModel" "DashboardViewModel" "VaultlyAutofillService"
```

> In production builds with ProGuard enabled, enable `keepclassmembers` rules in `proguard-rules.pro` to retain relevant class names for debugging.

### 8.2 Monitoring

There is no server-side component to monitor — the app is entirely client-side. Key external services to watch:

| Service | Status Page |
|---|---|
| Pinata (IPFS pinning) | [status.pinata.cloud](https://status.pinata.cloud) |
| Polygon Amoy testnet | [status.polygon.technology](https://status.polygon.technology) |
| Reown / WalletConnect | [status.walletconnect.com](https://status.walletconnect.com) |

### 8.3 Backups

User data is stored in two places:

| Location | Contents | User-controlled? |
|---|---|---|
| Room DB (on-device) | Encrypted credential blobs + CIDs | Via Android backup or manual export |
| IPFS via Pinata | Encrypted vault JSON blob | Yes — as long as Pinata account is active |
| Polygon Amoy chain | Mapping of wallet address → CID | Immutable on-chain, no backup needed |

**Important:** The AES decryption key is derived deterministically from the wallet's ECDSA signature. As long as the user retains their wallet seed phrase, they can always re-derive the decryption key and recover their vault from IPFS.

### 8.4 Incident Response Checklist

**User cannot log in / vault won't load:**
- [ ] Verify internet connectivity.
- [ ] Check Pinata status page — is the pinning service degraded?
- [ ] Check Polygon Amoy RPC — is the `getCID` call timing out?
- [ ] Confirm the wallet address matches the contract record (use PolygonScan explorer).
- [ ] Confirm the Pinata JWT has not expired or been revoked.
- [ ] Check `adb logcat` for stack traces from `AuthViewModel` or `PolygonRepository`.

**Autofill not working:**
- [ ] Confirm the app is set as the device Autofill Service (Settings → Passwords → Autofill service → Vaultly).
- [ ] Confirm biometric authentication is enrolled on the device.
- [ ] Look for errors in `VaultlyAutofillService` logcat tag.
- [ ] See also: `AUTOFILL_DEBUG_INSTRUCTIONS.md` in the repo root.

**Credential not saving to IPFS:**
- [ ] Confirm `JWT_TOKEN` is valid and not expired — test with a direct Pinata API call.
- [ ] Check for `4xx` / `5xx` responses in OkHttp logs.
- [ ] Confirm `IPFS_URL` gateway is reachable from the device.

---

## 9. Troubleshooting

### Gradle Sync Fails

| Symptom | Fix |
|---|---|
| `Could not resolve com.reown:appkit` | Add `maven { url = uri("https://jitpack.io") }` to `settings.gradle.kts` repositories |
| `Unsupported class file major version` | Ensure JDK 11 is selected in Android Studio → Project Structure → SDK Location |
| `Duplicate class kotlin.collections...` | Check for conflicting Kotlin stdlib versions in the dependency tree (`./gradlew dependencies`) |

### Build Errors

| Symptom | Fix |
|---|---|
| `error: unresolved reference: BuildConfig` | Enable `buildFeatures { buildConfig = true }` in `app/build.gradle.kts` |
| Hilt `@HiltViewModel` not found at compile time | Ensure `ksp("com.google.dagger:hilt-android-compiler:...")` is in the dependencies |
| Room `Cannot find implementation` | Check that `ksp("androidx.room:room-compiler:...")` is declared |

### Runtime Errors

| Symptom | Fix |
|---|---|
| App crashes on launch with `NullPointerException` in AppKit | Verify that the WalletConnect Project ID is initialized before AppKit is accessed |
| Vault data not loading after wallet connect | Check `PolygonRepository` logs — the smart contract may return an empty CID for a new wallet |
| Biometric prompt not appearing | Ensure at least one biometric method is enrolled in device Settings |
| Autofill suggestions not showing | Set Vaultly as the active Autofill Service in Android Settings |
| `javax.net.ssl.SSLHandshakeException` | The IPFS gateway URL or Pinata URL may be misconfigured; verify `IPFS_URL` includes `https://` |

### Common ADB Commands

```bash
# View Vaultly-specific logs
adb logcat -s "Vaultly"

# Clear app data (resets vault, forces re-login)
adb shell pm clear com.dapp.vaultly

# Install latest debug APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Force-stop the app
adb shell am force-stop com.dapp.vaultly
```

---

## 10. Security

### Threat Model Highlights

| Threat | Mitigation |
|---|---|
| Stolen device | Biometric lock screen; `FLAG_SECURE` prevents screen capture |
| Credential theft at rest | AES-256-GCM encryption; key stored in Android Keystore |
| Key recovery without wallet | HKDF key derivation requires the wallet's private key — not recoverable without seed |
| MITM on API traffic | HTTPS enforced for all Retrofit clients; OkHttp TLS by default |
| Clipboard snooping | Android 13+ `EXTRA_IS_SENSITIVE` clipboard flag; clipboard auto-cleared |
| Screenshots / recent apps | `FLAG_SECURE` on `MainActivity` |
| Unauthorized autofill | Biometric authentication gate (`AuthenticateBeforeAutofillActivity`) |

### Encryption Details

- **Algorithm:** AES-256-GCM (authenticated encryption).
- **Key derivation:** HKDF-SHA256 from the wallet's ECDSA signature over a domain-specific message.
- **Key storage:** The derived AES key is stored using `AesKeyStorage`, which wraps Android Keystore–backed `EncryptedSharedPreferences` (`androidx.security:security-crypto`).
- **IV:** A fresh random IV is generated for every encryption operation and prepended to the ciphertext.
- **Vault format:** The credential list is JSON-serialized, encrypted as a single blob, and uploaded to IPFS.

### ⚠️ Hardcoded Secrets — Action Required

> **The current codebase contains live API credentials in `Constants.kt` (Pinata JWT and PolygonScan API key). These must be rotated and moved to a secrets-management solution before this repository is made public or used in production.**

Steps to remediate:
1. Revoke the existing Pinata JWT at [app.pinata.cloud](https://app.pinata.cloud) → API Keys.
2. Revoke the existing PolygonScan API key at [polygonscan.com](https://polygonscan.com).
3. Generate new keys and follow the `local.properties` / `BuildConfig` approach described in [Section 5](#5-configuration).
4. Add `local.properties` to `.gitignore` (already done by default Android projects — verify).

### Dependency Scanning

Run a dependency vulnerability check before each release:

```bash
./gradlew dependencyCheckAnalyze
```

> This requires the [OWASP Dependency-Check Gradle plugin](https://github.com/jeremylong/DependencyCheck). Add it to `build.gradle.kts` if not already present.

### Reporting Vulnerabilities

Please **do not** open a public GitHub issue for security vulnerabilities. Instead, email the maintainer directly (see the GitHub profile for contact details) or use GitHub's private [Security Advisory](https://github.com/kaleem04/Vaultly/security/advisories/new) feature.

---

## 11. Contributing

### Branching Strategy

```
main                  ← stable, production-ready
└── feature/<name>    ← new features
└── fix/<name>        ← bug fixes
└── chore/<name>      ← tooling, deps, documentation
└── hotfix/<name>     ← urgent production fixes
```

All changes go through a Pull Request targeting `main`.

### Pull Request Checklist

Before opening a PR, confirm:

- [ ] Code compiles without errors (`./gradlew assembleDebug`)
- [ ] All existing unit tests pass (`./gradlew test`)
- [ ] Lint passes with no new warnings (`./gradlew lint`)
- [ ] No secrets or credentials are committed
- [ ] Room migrations are included if the schema changed
- [ ] New screens/features are covered by at least a basic unit test
- [ ] PR description explains *what* changed and *why*

### Code Style

- Follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Use `kotlin.code.style=official` (already set in `gradle.properties`).
- Format code with Android Studio's built-in formatter (`Ctrl+Alt+L` / `Cmd+Alt+L`).
- Prefer coroutines and `StateFlow` / `SharedFlow` over `LiveData`.
- Use Hilt for all dependency injection — avoid manual `object` singletons.

### Commit Conventions

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <short description>

feat(autofill): add biometric gate before showing suggestions
fix(crypto): handle empty signature gracefully
chore(deps): bump Hilt to 2.57.1
docs(readme): add troubleshooting section
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`.

---

## 12. License & Acknowledgements

### License

> <!-- TODO: Add a LICENSE file to the repository root. Common choices: MIT, Apache 2.0. -->
>
> The license for this project has not yet been specified. Until a `LICENSE` file is added, all rights are reserved by the author.

### Acknowledgements

- [Jetpack Compose](https://developer.android.com/jetpack/compose) — modern Android UI toolkit.
- [Reown AppKit](https://reown.com) (formerly WalletConnect) — wallet connection protocol.
- [Web3j](https://github.com/hyperledger/web3j) — Java/Kotlin Ethereum library.
- [Pinata](https://pinata.cloud) — IPFS pinning service.
- [Polygon](https://polygon.technology) — EVM-compatible L2 network.
- [Dagger Hilt](https://dagger.dev/hilt/) — dependency injection for Android.
- [Room](https://developer.android.com/training/data-storage/room) — SQLite ORM for Android.
- [OWASP](https://owasp.org) — security best practices and dependency scanning tooling.
