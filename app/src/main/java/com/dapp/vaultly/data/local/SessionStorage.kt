package com.dapp.vaultly.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore("user_preferences")

object SessionStorage {

    private val IS_CONNECTED = booleanPreferencesKey("is_connected")
    suspend fun saveSession(context: Context, connected: Boolean) {
        context.dataStore.edit { pref ->
            pref[IS_CONNECTED] = connected

        }
    }

    fun readSession(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { pref ->
            pref[IS_CONNECTED] ?: false

        }
    }
}