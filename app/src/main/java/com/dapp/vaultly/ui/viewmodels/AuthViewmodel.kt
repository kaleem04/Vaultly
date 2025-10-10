package com.dapp.vaultly.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dapp.vaultly.data.local.AesKeyStorage
import com.dapp.vaultly.data.model.WalletUiState
import com.dapp.vaultly.util.CryptoUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewmodel @Inject constructor(
    private val context: Application,
) : ViewModel() {
    private val _uiState = MutableStateFlow<WalletUiState>(WalletUiState.DashboardReady)
    val uiState: StateFlow<WalletUiState> = _uiState

    init {
        // Check if AES key exists at startup
        viewModelScope.launch {
            AesKeyStorage.readKey(context).collect { key ->
                _uiState.value = if (key != null) {
                    WalletUiState.DashboardReady // go directly
                } else {
                    WalletUiState.Welcome
                }
            }
        }
    }

    fun onWalletConnected(address: String) {
        // Navigate to dashboard after wallet connect

        _uiState.value = WalletUiState.DashboardPendingSignature(address)
    }

    fun onSignatureApproved(signature: String) {
        viewModelScope.launch {
            val aesKey = CryptoUtil.deriveAesKeyFromSignature(signature)
            Log.d("@@","AES Key: $aesKey")
            AesKeyStorage.saveKey(context, aesKey)
            _uiState.value = WalletUiState.DashboardReady
        }
    }

    fun onLogout() {
        viewModelScope.launch {
            AesKeyStorage.clearKey(context)
            _uiState.value = WalletUiState.Welcome
        }
    }
}


