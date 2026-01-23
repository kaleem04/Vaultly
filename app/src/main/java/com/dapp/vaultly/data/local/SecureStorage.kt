package com.dapp.vaultly.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureStorage @Inject constructor(
    private val context: Context
) {

    private val prefsName = "vaultly_secure_prefs"
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val prefs by lazy {
        EncryptedSharedPreferences.create(
            prefsName,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private object Keys {
        const val LOCK_ENABLED = "lock_enabled"
        const val PIN_HASH = "pin_hash"
        const val PIN_SALT = "pin_salt"
        const val LAST_UNLOCK_TS = "last_unlock_ts"
        const val FAILED_ATTEMPTS = "failed_attempts"
    }

    fun isLockEnabled(): Boolean {
        return prefs.getBoolean(Keys.LOCK_ENABLED, false)
    }

    fun setLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(Keys.LOCK_ENABLED, enabled).apply()
    }

    fun savePinHash(hashBase64: String, saltBase64: String) {
        prefs.edit().putString(Keys.PIN_HASH, hashBase64).putString(Keys.PIN_SALT, saltBase64).apply()
    }

    fun getPinHash(): Pair<String, String>? {
        val hash = prefs.getString(Keys.PIN_HASH, null)
        val salt = prefs.getString(Keys.PIN_SALT, null)
        return if (hash != null && salt != null) Pair(hash, salt) else null
    }

    fun setLastUnlockTs(ts: Long) {
        prefs.edit().putLong(Keys.LAST_UNLOCK_TS, ts).apply()
    }

    fun getLastUnlockTs(): Long {
        return prefs.getLong(Keys.LAST_UNLOCK_TS, 0L)
    }

    fun resetFailedAttempts() {
        prefs.edit().putInt(Keys.FAILED_ATTEMPTS, 0).apply()
    }

    fun incrementFailedAttempts(): Int {
        val cur = prefs.getInt(Keys.FAILED_ATTEMPTS, 0)
        val next = cur + 1
        prefs.edit().putInt(Keys.FAILED_ATTEMPTS, next).apply()
        return next
    }

    fun clearPin() {
        prefs.edit().remove(Keys.PIN_HASH).remove(Keys.PIN_SALT).apply()
    }

}
