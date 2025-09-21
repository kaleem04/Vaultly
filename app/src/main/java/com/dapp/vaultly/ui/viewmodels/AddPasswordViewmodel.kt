package com.dapp.vaultly.ui.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.dapp.vaultly.data.model.AddPasswordUiState
import com.dapp.vaultly.data.repository.UserVaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddPasswordViewmodel @Inject constructor(
    private val vaultRepo: UserVaultRepository
) : ViewModel() {

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

    fun onNoteChange(newNote: String) {
        _uiState.value = _uiState.value.copy(note = newNote)
    }

    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            showPassword = !_uiState.value.showPassword
        )
    }

    fun credentialsValidation(): Boolean {
        return uiState.value.username.isNotBlank() &&
                uiState.value.website.isNotBlank() &&
                uiState.value.password.isNotBlank()


    }

    fun clearFields() {
        _uiState.value = AddPasswordUiState()
    }


}