package com.dapp.vaultly.ui.viewmodels

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.autofill.AutofillManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AutofillSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _isAutofillEnabled = MutableStateFlow(false)
    val isAutofillEnabled: StateFlow<Boolean> = _isAutofillEnabled

    init {
        checkAutofillStatus()
    }

    fun checkAutofillStatus() {
        viewModelScope.launch {
            try {
                val autofillManager = context.getSystemService(AutofillManager::class.java)
                val isEnabled = autofillManager?.hasEnabledAutofillServices() == true
                _isAutofillEnabled.value = isEnabled
            } catch (e: Exception) {
                _isAutofillEnabled.value = false
            }
        }
    }

    fun openAutofillSettings(context: Context) {
        val intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
            data = android.net.Uri.parse("package:com.dapp.vaultly")
        }
        context.startActivity(intent)
    }

    // Call this when returning from settings
    fun onResume() {
        checkAutofillStatus()
    }
}