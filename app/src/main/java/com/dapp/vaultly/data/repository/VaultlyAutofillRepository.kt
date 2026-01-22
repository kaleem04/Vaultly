package com.dapp.vaultly.data.repository

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dapp.vaultly.data.model.AutofillCredential
import com.dapp.vaultly.data.model.Credential
import com.reown.appkit.client.AppKit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

private val Context.autofillDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "autofill_secure_datastore"
)

@Singleton
class VaultlyAutofillRepository @Inject constructor(
    private val vaultRepository: UserVaultRepository,
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.autofillDataStore
    private val keyAlias = "vaultly_autofill_key"

    private companion object {
        val KEY_LAST_USER_ID = stringPreferencesKey("last_user_id")
        val KEY_ENCRYPTED_CREDENTIALS = stringPreferencesKey("encrypted_credentials")
    }

    init {
        initializeKeystore()
    }

    /**
     * Initialize a key that does NOT require authentication
     * Security is provided by biometric prompt in the UI, not the key itself
     */
    private fun initializeKeystore() {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)

            // Delete old key if it exists with wrong config
            if (keyStore.containsAlias(keyAlias)) {
                keyStore.deleteEntry(keyAlias)
                Log.d("VaultlyAutofill", "Deleted old keystore entry")
            }

            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )

            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
            Log.d("VaultlyAutofill", "Keystore initialized successfully (no auth required)")
        } catch (e: Exception) {
            Log.e("VaultlyAutofill", "Error initializing keystore", e)
        }
    }

    /**
     * Sync credentials - encrypt and store them
     * Call this after login or when credentials change
     */
    suspend fun syncCredentials(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("VaultlyAutofill", "Syncing credentials for user: $userId")

                // Get credentials from vault
                val credentials = vaultRepository.getCredentials(userId).firstOrNull() ?: emptyList()

                if (credentials.isEmpty()) {
                    Log.d("VaultlyAutofill", "No credentials to sync")
                    return@withContext
                }

                // Convert credentials to JSON
                val jsonArray = JSONArray()
                credentials.forEach { credential ->
                    val jsonObject = JSONObject().apply {
                        put("website", credential.website)
                        put("username", credential.username)
                        put("password", credential.password)
                        put("note", credential.note)
                    }
                    jsonArray.put(jsonObject)
                }

                // Encrypt the JSON string
                val encryptedData = encryptCredentials(jsonArray.toString())

                if (encryptedData != null) {
                    // Store in DataStore
                    dataStore.edit { preferences ->
                        preferences[KEY_LAST_USER_ID] = userId
                        preferences[KEY_ENCRYPTED_CREDENTIALS] = encryptedData
                    }
                    Log.d("VaultlyAutofill", "Successfully synced ${credentials.size} encrypted credentials")
                } else {
                    Log.e("VaultlyAutofill", "Failed to encrypt credentials")
                }
            } catch (e: Exception) {
                Log.e("VaultlyAutofill", "Error syncing credentials", e)
            }
        }
    }

    /**
     * Get credentials for autofill
     * Note: Biometric security is enforced in AuthenticateBeforeAutofillActivity,
     * not at the key level. This allows the key to work when app is closed.
     */
    suspend fun getMatchingCredentials(packageName: String): List<AutofillCredential> {
        return withContext(Dispatchers.IO) {
            try {
                // Try to get current user ID
                var userId = AppKit.getAccount()?.address

                // If app is closed, get last logged in user from DataStore
                if (userId == null) {
                    userId = dataStore.data.map { preferences ->
                        preferences[KEY_LAST_USER_ID]
                    }.first()
                    Log.d("VaultlyAutofill", "App closed, using cached userId: $userId")
                }

                if (userId == null) {
                    Log.d("VaultlyAutofill", "No user ID found")
                    return@withContext emptyList()
                }

                // Get encrypted credentials from DataStore
                val encryptedData = dataStore.data.map { preferences ->
                    preferences[KEY_ENCRYPTED_CREDENTIALS]
                }.first()

                if (encryptedData == null) {
                    Log.d("VaultlyAutofill", "No cached credentials found")
                    return@withContext emptyList()
                }

                // Decrypt credentials
                val decryptedJson = decryptCredentials(encryptedData)
                if (decryptedJson == null) {
                    Log.e("VaultlyAutofill", "Failed to decrypt credentials")
                    return@withContext emptyList()
                }

                // Parse JSON
                val jsonArray = JSONArray(decryptedJson)
                val credentials = mutableListOf<AutofillCredential>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    credentials.add(
                        AutofillCredential(
                            id = "$userId-$i",
                            website = obj.getString("website"),
                            username = obj.getString("username"),
                            password = obj.getString("password"),
                            note = obj.getString("note")
                        )
                    )
                }

                Log.d("VaultlyAutofill", "Successfully loaded ${credentials.size} credentials for package: $packageName")
                credentials
            } catch (e: Exception) {
                Log.e("VaultlyAutofill", "Error getting credentials", e)
                emptyList()
            }
        }
    }

    private fun encryptCredentials(data: String): String? {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            val secretKey = keyStore.getKey(keyAlias, null) as SecretKey

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

            // Combine IV and encrypted data
            val combined = iv + encryptedBytes
            android.util.Base64.encodeToString(combined, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("VaultlyAutofill", "Encryption failed", e)
            null
        }
    }

    private fun decryptCredentials(encryptedData: String): String? {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            val secretKey = keyStore.getKey(keyAlias, null) as SecretKey

            val combined = android.util.Base64.decode(encryptedData, android.util.Base64.DEFAULT)

            // Extract IV (first 12 bytes for GCM)
            val iv = combined.copyOfRange(0, 12)
            val encryptedBytes = combined.copyOfRange(12, combined.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e("VaultlyAutofill", "Decryption failed", e)
            null
        }
    }

    /**
     * Clear all credentials when user logs out
     */
    suspend fun clearCredentials() {
        withContext(Dispatchers.IO) {
            try {
                dataStore.edit { preferences ->
                    preferences.remove(KEY_LAST_USER_ID)
                    preferences.remove(KEY_ENCRYPTED_CREDENTIALS)
                }
                Log.d("VaultlyAutofill", "Cleared all autofill credentials")
            } catch (e: Exception) {
                Log.e("VaultlyAutofill", "Error clearing credentials", e)
            }
        }
    }

    /**
     * Check if credentials are cached
     */
    suspend fun hasCredentialsCached(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                dataStore.data.map { preferences ->
                    preferences[KEY_ENCRYPTED_CREDENTIALS] != null
                }.first()
            } catch (e: Exception) {
                false
            }
        }
    }

    fun getAppLabel(packageName: String): String {
        return try {
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}