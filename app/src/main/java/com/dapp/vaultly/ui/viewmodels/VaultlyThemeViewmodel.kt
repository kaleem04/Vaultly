package com.dapp.vaultly.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dapp.vaultly.data.local.ThemePreferencesManager
import com.dapp.vaultly.data.model.AppThemeState
import com.dapp.vaultly.data.model.VaultlyTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel // Annotate ViewModel to be Hilt-enabled
class VaultlyThemeViewmodel @Inject constructor( // Use @Inject constructor
    private val themePreferencesManager: ThemePreferencesManager // Hilt will inject this
) : ViewModel() { // No longer needs to be AndroidViewModel

    val appThemeState: StateFlow<AppThemeState> = themePreferencesManager.appThemeState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppThemeState() // Default initial state
        )

    fun setBaseThemeOption(vaultlyTheme: VaultlyTheme) {
        viewModelScope.launch {
            themePreferencesManager.saveBaseThemeOption(vaultlyTheme)
        }
    }

    fun setDynamicColorEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            themePreferencesManager.saveDynamicColorEnabled(isEnabled)
        }
    }
}