package com.dapp.vaultly.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dapp.vaultly.data.local.AesKeyStorage
import com.dapp.vaultly.data.model.WalletUiState
import com.dapp.vaultly.util.CryptoUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.system.measureTimeMillis

@HiltViewModel
class AuthViewmodel @Inject constructor(
    private val context: Application,
) : ViewModel() {
    private val _uiState =
        MutableStateFlow<WalletUiState>(WalletUiState.Loading) // Initial state as Loading
    val uiState: StateFlow<WalletUiState> = _uiState

    init {
        // Check if AES key exists at startup
        viewModelScope.launch {
            // OPTIMIZATION: Switched to a background thread for the initial key check.
            // This prevents blocking the main thread while reading from storage, which could be slow.
            // Using firstOrNull() is more efficient as we only need the first emission to decide the initial state.
            val key = withContext(Dispatchers.IO) {
                AesKeyStorage.readKey(context).firstOrNull()
            }
            _uiState.value = if (key != null) {
                WalletUiState.DashboardReady // go directly
            } else {
                WalletUiState.Welcome
            }
        }
    }

    fun onWalletConnected(address: String) {
        // Navigate to dashboard after wallet connect
        _uiState.value = WalletUiState.DashboardPendingSignature(address)
    }

    fun onSignatureApproved(signature: String) {
        // OPTIMIZATION: Moved cryptographic key derivation and file I/O to a background thread.
        // `deriveAesKeyFromSignature` could be CPU-intensive, and `saveKey` involves disk I/O.
        // Performing these off the main thread ensures the UI remains responsive.
        viewModelScope.launch {
            val executionTime = measureTimeMillis {
                withContext(Dispatchers.IO) {
                    val aesKey = CryptoUtil.deriveAesKeyFromSignature(signature)
                    AesKeyStorage.saveKey(context, aesKey)
                    Log.d("AuthViewModelPerformance", "AES Key derivation and storage complete.")
                }
            }
            // PERFORMANCE METRIC: Logging the time taken for the operation.
            Log.d("AuthViewModelPerformance", "onSignatureApproved took ${executionTime}ms")

            // Update UI state on the main thread after background work is complete.
            _uiState.value = WalletUiState.DashboardReady
        }
    }

    fun onLogout() {
        // OPTIMIZATION: Moved file I/O for clearing the key to a background thread.
        // This prevents potential UI stutters during the logout process.
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                AesKeyStorage.clearKey(context)
            }
            _uiState.value = WalletUiState.Welcome
        }
    }
}
