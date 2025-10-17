package com.dapp.vaultly.ui.viewmodels

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
@HiltViewModel
class AutofillSettingsViewModel @Inject constructor(
    private val context: Context
) : ViewModel() {
    private val _isAutofillEnabled = MutableStateFlow(false)
    val isAutofillEnabled: StateFlow<Boolean> = _isAutofillEnabled

    init {
        checkAutofillStatus()
    }

    private fun checkAutofillStatus() {
        // Check if Vaultly is the active autofill service
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE
        ) ?: ""

        val isEnabled = enabledServices.contains("com.dapp.vaultly/.autofill.VaultlyAutofillService")
        _isAutofillEnabled.value = isEnabled
    }

    fun openAutofillSettings(context: Context) {
        val intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
            data = android.net.Uri.parse("package:com.dapp.vaultly")
        }
        context.startActivity(intent)
        // Recheck status after returning
        checkAutofillStatus()
    }
}