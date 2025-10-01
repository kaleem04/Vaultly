package com.dapp.vaultly.data.local

// AesKeyStorage.kt
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Base64
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
val Context.userPrefrencesDatastore by preferencesDataStore("user_preferences")
object AesKeyStorage {
    private val AES_KEY = stringPreferencesKey("aes_key")


    fun readKey(context: Context): Flow<SecretKey?> {
        return context.userPrefrencesDatastore.data.map { prefs ->
            prefs[AES_KEY]?.let { encoded ->
                val bytes = Base64.getDecoder().decode(encoded)
                SecretKeySpec(bytes, "AES")
            }
        }

    }

    suspend fun saveKey(context: Context, key: SecretKey) {
        context.userPrefrencesDatastore.edit { prefs ->
            prefs[AES_KEY] = Base64.getEncoder().encodeToString(key.encoded)
        }
    }

    suspend fun clearKey(context: Context) {
        context.userPrefrencesDatastore.edit { prefs ->
            prefs.remove(AES_KEY)
        }
    }

}
