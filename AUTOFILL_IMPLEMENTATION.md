# Vaultly Autofill Implementation Guide

## Overview
Vaultly now implements a comprehensive autofill service following Google Password Manager best practices. Users can automatically fill login credentials across apps and websites, and save new credentials when logging in.

---

## Features Implemented

### ✅ **1. Autofill Fill Requests**
- Automatically detects login fields in apps and websites
- Shows "🔐 Vaultly passwords" suggestion when login fields are detected
- Requires biometric authentication before showing credentials
- Intelligent field detection using:
  - Autofill hints (most reliable)
  - HTML attributes (for WebViews)
  - Field IDs, hints, and text content
  - Input types (password fields)

### ✅ **2. Autofill Save Requests**
- Automatically prompts to save new credentials when users log into apps
- Extracts username/email and password from login forms
- Saves credentials to user's encrypted vault
- Syncs saved credentials to autofill cache automatically
- Shows toast notification: "Password saved to Vaultly"

### ✅ **3. Smart Domain Matching**
- Matches credentials to apps using intelligent heuristics
- Handles common app name variations (e.g., "Facebook" matches "fb", "meta")
- Built-in mappings for popular apps:
  - Google (gmail, youtube)
  - Facebook (fb, meta)
  - Twitter (x)
  - Instagram (ig)
  - Microsoft (outlook, office)
  - Netflix, Amazon, Spotify, WhatsApp, LinkedIn
- Falls back to showing all credentials if no exact match found

### ✅ **4. Auto-Sync on Credential Changes**
- Automatically syncs autofill cache when:
  - User adds/updates a credential in the vault
  - User deletes a credential
  - User saves a credential via autofill
- No manual sync needed - always up to date

### ✅ **5. Biometric Security**
- Requires fingerprint/face unlock before showing credentials
- Shows modern bottom sheet UI with:
  - User avatars (first letter of username)
  - Masked passwords (••••••••)
  - Website/app name display
  - Cancel button
- Works even when Vaultly app is closed

### ✅ **6. Enhanced UI**
- Beautiful credential selection dialog
- Shows website name alongside username
- Material Design 3 theming
- Smooth animations and transitions
- Handle bar for easy dismissal

---

## Files Modified

### 1. **VaultlyAutofillService.kt**
**Location:** `app/src/main/java/com/dapp/vaultly/autofill/VaultlyAutofillService.kt`

**Changes:**
- Implemented `onSaveRequest()` to handle saving new credentials
- Added `SaveInfo` to fill responses to trigger save prompts
- Improved `parseViewStructure()` with HTML attribute detection
- Added helper method `findValueForAutofillId()` to extract field values
- Added `extractDomainFromPackage()` for friendly app name mapping
- Updated presentation text to "🔐 Vaultly passwords"

**Key Methods:**
```kotlin
override fun onFillRequest(...)  // Handles autofill requests
override fun onSaveRequest(...)  // Handles saving new credentials
private fun parseViewStructure(...)  // Detects login fields
private fun findValueForAutofillId(...)  // Extracts field values
private fun extractDomainFromPackage(...)  // Maps package to friendly name
```

### 2. **VaultlyAutofillRepository.kt**
**Location:** `app/src/main/java/com/dapp/vaultly/data/repository/VaultlyAutofillRepository.kt`

**Changes:**
- Improved `getMatchingCredentials()` with smart filtering
- Added `matchesDomain()` method for intelligent app/website matching
- Handles common app name variations and aliases
- Returns all credentials if no match (user can choose)

**Key Methods:**
```kotlin
suspend fun getMatchingCredentials(packageName: String): List<AutofillCredential>
private fun matchesDomain(website: String, packageName: String): Boolean
```

### 3. **DashboardViewmodel.kt**
**Location:** `app/src/main/java/com/dapp/vaultly/ui/viewmodels/DashboardViewmodel.kt`

**Changes:**
- Added automatic autofill sync in `addOrUpdateCredential()`
- Added automatic autofill sync in `deleteCredential()`
- Ensures autofill cache stays in sync with vault changes

**Modified Methods:**
```kotlin
fun addOrUpdateCredential(userId: String, credential: Credential) {
    // ... saves credential ...
    vaultlyAutofillRepository.syncCredentials(userId)  // Auto-sync
}

fun deleteCredential(userId: String, website: String) {
    // ... deletes credential ...
    vaultlyAutofillRepository.syncCredentials(userId)  // Auto-sync
}
```

### 4. **AuthenticateBeforeAutofillActivity.kt**
**Location:** `app/src/main/java/com/dapp/vaultly/autofill/ui/AuthenticateBeforeAutofillActivity.kt`

**Changes:**
- Improved `CredentialModalItem` composable to show website name
- Added padding and better typography
- Shows "username • website" format

### 5. **autofill_service_config.xml**
**Location:** `app/src/main/res/xml/autofill_service_config.xml`

**Changes:**
- Updated `settingsActivity` to point to `MainActivity`
- Allows users to access app settings from autofill service settings

---

## How It Works

### **Autofill Flow (Fill Request)**

1. **User taps on login field in an app**
2. **Android detects autofillable fields** and calls `VaultlyAutofillService.onFillRequest()`
3. **Service parses view structure** to identify username/email and password fields
4. **Service shows autofill suggestion**: "🔐 Vaultly passwords"
5. **User taps the suggestion**
6. **AuthenticateBeforeAutofillActivity launches**:
   - Loads matching credentials from encrypted cache
   - Shows biometric prompt
7. **After authentication succeeds**:
   - Shows bottom sheet with matching credentials
   - User selects a credential
8. **Fields are automatically filled** with selected credentials

### **Save Flow (Save Request)**

1. **User logs into an app** (types username and password)
2. **User submits the login form**
3. **Android detects new login** and calls `VaultlyAutofillService.onSaveRequest()`
4. **Service extracts credentials**:
   - Parses view structure to find filled values
   - Extracts username/email and password
5. **Service saves to vault**:
   - Creates new `Credential` object
   - Calls `vaultRepository.addOrUpdateCredential()`
   - Syncs to autofill cache
6. **Shows toast**: "Password saved to Vaultly"
7. **Credential is now available** for future autofill

### **Domain Matching Logic**

When an app requests autofill, the service:
1. Gets all cached credentials
2. Filters by matching domain/package name:
   - Direct substring match
   - Split package and website into parts
   - Check if any significant parts match
   - Check against known app name aliases
3. Returns matching credentials (or all if no match)

---

## User Guide

### **Enable Autofill**

1. Open Android Settings
2. Navigate to **Passwords & accounts**
3. Tap **Autofill service**
4. Select **Vaultly**
5. Grant permission

**Or from the app:**
1. Open Vaultly
2. Go to **Profile** tab
3. Find **Autofill Service** section
4. Tap to enable

### **Using Autofill**

1. **Open any app or website with login**
2. **Tap on username/email field**
3. **See "🔐 Vaultly passwords" suggestion** above keyboard
4. **Tap the suggestion**
5. **Authenticate with fingerprint/face**
6. **Select credential** from the list
7. **Fields are auto-filled** - tap login!

### **Saving New Passwords**

1. **Log into an app** (type your credentials)
2. **Tap the login button**
3. **Android shows save prompt**
4. **Tap "Save"**
5. **Vaultly saves the credential**
6. **Next time you can autofill!**

---

## Security

### **Encryption**
- Credentials are encrypted using **AES-256-GCM**
- Encryption key stored in **Android Keystore**
- Key never leaves secure hardware

### **Authentication**
- **Biometric prompt required** before showing credentials
- Supports: Fingerprint, Face unlock, PIN/Pattern
- Works even when app is closed

### **Storage**
- Encrypted credentials cached in **DataStore**
- User ID cached for offline autofill
- Cache cleared on logout

### **Privacy**
- Vaultly never sends credentials to servers
- Autofill works completely offline
- Only you can decrypt your passwords

---

## Troubleshooting

### **Autofill not showing**
- Check if Vaultly is enabled in Android Settings > Autofill service
- Ensure you have saved credentials in your vault
- Try manually syncing: Dashboard > "Sync to Blockchain"

### **Biometric prompt not appearing**
- Ensure biometric is set up on device
- Go to Settings > Security > Biometric
- Add fingerprint or face unlock

### **Credentials not saving**
- Ensure you're logged into Vaultly
- Check app permissions
- Verify autofill service is enabled

### **Wrong credentials shown**
- Domain matching may be too broad
- Manually select correct credential from list
- Edit credential website name in vault for better matching

---

## Known Limitations

1. **WebView detection**: Some apps use custom WebViews that don't properly expose autofill hints
2. **Two-step logins**: Apps with separate username and password screens may only save on second screen
3. **OAuth/SSO**: Apps using OAuth (Google Sign-In, etc.) won't trigger autofill
4. **Custom keyboards**: Some third-party keyboards may block autofill suggestions

---

## Future Enhancements

### **Potential Improvements**
1. **Inline Presentations** (Android 11+): Show credentials directly in keyboard
2. **Password Generator**: Suggest strong passwords during account creation
3. **Update Detection**: Detect password changes and prompt to update
4. **Multi-account Support**: Handle apps with multiple saved accounts
5. **Web Domain Association**: Digital Asset Links for seamless web/app autofill
6. **Import/Export**: Import from Chrome, Firefox, etc.
7. **Password Health**: Check for weak/reused passwords
8. **Autofill for Cards**: Support credit card autofill

---

## Developer Notes

### **Testing Autofill**

1. **Test in Chrome**:
   - Open `https://example.com/login`
   - Tap username field
   - Should see Vaultly suggestion

2. **Test in Apps**:
   - Install any app with login (Twitter, Facebook, etc.)
   - Open login screen
   - Tap username field
   - Should see Vaultly suggestion

3. **Test Save Request**:
   - Log into a new app
   - Should see Android's "Save password?" prompt
   - Tap Save
   - Check if credential appears in Vaultly vault

### **Debugging**

Enable logging with Timber:
```kotlin
Timber.d("VaultlyAutofill", "Your debug message")
```

View logs:
```bash
adb logcat | grep VaultlyAutofill
```

### **Common Issues**

**Issue**: `onSaveRequest` not called
- **Fix**: Ensure `SaveInfo` is added to `FillResponse` in `onFillRequest`

**Issue**: Fields not detected
- **Fix**: Improve `parseViewStructure()` heuristics
- Check if app uses proper autofill hints

**Issue**: App crashes when closed
- **Fix**: Repository uses `AppKit.getAccount()` which may be null
- Falls back to cached user ID from DataStore

---

## API Requirements

- **Minimum SDK**: 27 (Android 8.1)
- **Target SDK**: 34 (Android 14)
- **Permissions Required**:
  - `android.permission.BIND_AUTOFILL_SERVICE`

---

## Summary

Vaultly now provides a complete autofill experience:
- ✅ Fill credentials in any app or website
- ✅ Save new credentials automatically
- ✅ Smart domain matching
- ✅ Biometric security
- ✅ Beautiful UI
- ✅ Auto-sync on changes
- ✅ Works offline

Users can now enjoy Google Password Manager-level convenience with Vaultly's decentralized security!

---

**Last Updated**: January 23, 2026
**Version**: 1.0.0
**Status**: ✅ Production Ready
