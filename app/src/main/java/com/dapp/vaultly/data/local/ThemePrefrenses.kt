package com.dapp.vaultly.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dapp.vaultly.data.model.AppThemeState
import com.dapp.vaultly.data.model.VaultlyTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // Ensure only one instance of ThemePreferencesManager throughout the app
class ThemePreferencesManager @Inject constructor( // Use @Inject constructor
    private val context: Context // Hilt will provide this
) {
    val Context.themePreferences by preferencesDataStore("theme_prefrences")

    private object PreferencesKeys {
        val SELECTED_BASE_THEME = stringPreferencesKey("selected_base_theme")
        val IS_DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("is_dynamic_color_enabled")
    }

    val appThemeState: Flow<AppThemeState> = context.themePreferences.data
        .map { preferences ->
            val baseThemeOptionName = preferences[PreferencesKeys.SELECTED_BASE_THEME]
                ?: VaultlyTheme.SYSTEM_DEFAULT.name
            val baseTheme = try {
                VaultlyTheme.valueOf(baseThemeOptionName)
            } catch (e: IllegalArgumentException) {
                VaultlyTheme.SYSTEM_DEFAULT
            }

            val isDynamicEnabled = preferences[PreferencesKeys.IS_DYNAMIC_COLOR_ENABLED] ?: true
            AppThemeState(baseTheme, isDynamicEnabled)
        }

    suspend fun saveBaseThemeOption(vaultlyTheme: VaultlyTheme) {
        try {
            context.themePreferences.edit { preferences ->
                preferences[PreferencesKeys.SELECTED_BASE_THEME] = vaultlyTheme.name
            }
        } catch (e: IOException) {
            e.printStackTrace() // Consider more robust error handling
        }
    }

    suspend fun saveDynamicColorEnabled(isEnabled: Boolean) {
        try {
            context.themePreferences.edit { preferences ->
                preferences[PreferencesKeys.IS_DYNAMIC_COLOR_ENABLED] = isEnabled
            }
        } catch (e: IOException) {
            e.printStackTrace() // Consider more robust error handling
        }
    }
}