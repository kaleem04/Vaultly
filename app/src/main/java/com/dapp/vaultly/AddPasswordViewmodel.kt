package com.dapp.vaultly

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.dapp.vaultly.data.model.AddPasswordUiState

class AddPasswordViewmodel : ViewModel() {

    private val _uiState = mutableStateOf(AddPasswordUiState())
    val uiState: State<AddPasswordUiState> = _uiState

    fun onWebsiteChange(newWebsite: String) {
        _uiState.value = _uiState.value.copy(website = newWebsite)
    }

    fun onUsernameChange(newUsername: String) {
        _uiState.value = _uiState.value.copy(username = newUsername)
    }

    fun onPasswordChange(newPassword: String) {
        _uiState.value = _uiState.value.copy(password = newPassword)
    }

    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            showPassword = !_uiState.value.showPassword
        )
    }

    fun clearFields() {
        _uiState.value = AddPasswordUiState()
    }
}