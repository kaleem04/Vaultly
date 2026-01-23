package com.dapp.vaultly.ui.viewmodels

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dapp.vaultly.data.local.SecureStorage
import com.dapp.vaultly.util.BiometricAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LockUiState {
    object Locked : LockUiState()
    object PromptBiometric : LockUiState()
    object Unlocked : LockUiState()
    data class Error(val message: String) : LockUiState()
}

@HiltViewModel
class LockViewModel @Inject constructor(
    private val secureStorage: SecureStorage
) : ViewModel() {

    companion object {
        // Auto-lock timeout (ms): if app was backgrounded longer than this, require unlock on resume.
        // Change this value to suit your desired behavior. Default = 60 seconds.
        private const val AUTO_LOCK_TIMEOUT_MS = 60_000L
    }

    private val _uiState = MutableStateFlow<LockUiState>(LockUiState.Unlocked)
    val uiState: StateFlow<LockUiState> = _uiState

    // Expose lock enabled as an observable StateFlow so UI toggles update instantly
    private val _lockEnabled = MutableStateFlow(secureStorage.isLockEnabled())
    val lockEnabled: StateFlow<Boolean> = _lockEnabled

    fun isLockEnabled(): Boolean = _lockEnabled.value

    fun setLockEnabled(enabled: Boolean) {
        // Update observable immediately so UI reflects the change without waiting for coroutine
        _lockEnabled.value = enabled
        viewModelScope.launch {
            secureStorage.setLockEnabled(enabled)
            if (!enabled) {
                secureStorage.setLastUnlockTs(System.currentTimeMillis())
                _uiState.value = LockUiState.Unlocked
            } else {
                // when enabling, immediately require auth next time app resumes or show locked UI
                _uiState.value = LockUiState.Locked
            }
        }
    }

    fun startAuth(activity: FragmentActivity) {
        if (!isLockEnabled()) {
            _uiState.value = LockUiState.Unlocked
            return
        }

        val biomAvailable = BiometricAuth.isBiometricAvailable(activity)
        if (biomAvailable) {
            _uiState.value = LockUiState.PromptBiometric
            BiometricAuth.promptBiometric(
                activity = activity,
                onSuccess = {
                    secureStorage.setLastUnlockTs(System.currentTimeMillis())
                    _uiState.value = LockUiState.Unlocked
                },
                onFailure = { _ ->
                    // Keep locked and allow retry
                    _uiState.value = LockUiState.Locked
                }
            )
        } else {
            // No biometric or device credential available - surface an error so the UI can guide the user
            _uiState.value = LockUiState.Error("No biometric or device credential available on this device.")
        }
    }

    fun disableLock() {
        // Update observable instantly
        _lockEnabled.value = false
        viewModelScope.launch {
            secureStorage.setLockEnabled(false)
            secureStorage.setLastUnlockTs(System.currentTimeMillis())
            _uiState.value = LockUiState.Unlocked
        }
    }

    // Lock only if lock is enabled and the last unlock time was older than AUTO_LOCK_TIMEOUT_MS
    fun lockIfNeeded() {
        if (!_lockEnabled.value) {
            _uiState.value = LockUiState.Unlocked
            return
        }

        val last = secureStorage.getLastUnlockTs()
        val now = System.currentTimeMillis()
        val elapsed = now - last
        if (elapsed >= AUTO_LOCK_TIMEOUT_MS) {
            _uiState.value = LockUiState.Locked
        } else {
            _uiState.value = LockUiState.Unlocked
        }
    }
}
