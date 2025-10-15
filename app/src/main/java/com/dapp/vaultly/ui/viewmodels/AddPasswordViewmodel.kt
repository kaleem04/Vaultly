package com.dapp.vaultly.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.dapp.vaultly.data.model.AddPasswordUiState
import com.dapp.vaultly.data.model.Credential
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class AddPasswordViewmodel @Inject constructor() : ViewModel() {

    // 1. Use StateFlow for consistency with other ViewModels.
    private val _uiState = MutableStateFlow(AddPasswordUiState())
    val uiState = _uiState.asStateFlow()

    // 2. These properties control the bottom sheet's state and are managed by the UI.
    var openSheet by mutableStateOf(false)
    var editingCredential by mutableStateOf<Credential?>(null)
        private set // The UI can's change this directly, only through selectCredential.

    // 3. This is the primary trigger for the UI to show the sheet for editing.
    fun selectCredential(credential: Credential) {
        editingCredential = credential
        openSheet = true
    }

    // 4. A public function to trigger showing the sheet for creating a new credential.
    fun prepareForNewCredential() {
        editingCredential = null
        clearFields()
        openSheet = true
    }

    /**
     * Pre-fills the text fields when the user wants to edit an existing credential.
     * This is called from the Composable's LaunchedEffect.
     */
    fun loadCredentialForEditing(credential: Credential) {
        _uiState.update {
            it.copy(
                website = credential.website,
                username = credential.username,
                password = credential.password,
                note = credential.note
            )
        }
    }

    // --- State Update Functions for UI Input ---

    fun onWebsiteChange(newWebsite: String) {
        _uiState.update { it.copy(website = newWebsite) }
    }

    fun onUsernameChange(newUsername: String) {
        _uiState.update { it.copy(username = newUsername) }
    }

    fun onPasswordChange(newPassword: String) {
        _uiState.update { it.copy(password = newPassword) }
    }

    fun onNoteChange(newNote: String) {
        _uiState.update { it.copy(note = newNote) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(showPassword = !it.showPassword) }
    }

    /**
     * Validates the current input fields. The UI calls this before performing the save/update action.
     */
    fun credentialsValidation(): Boolean {
        return _uiState.value.website.isNotBlank() &&
                _uiState.value.username.isNotBlank() &&
                _uiState.value.password.isNotBlank()
    }

    /**
     * Clears all input fields and resets the state to default.
     * This is used when preparing for a new credential or when the sheet is dismissed.
     */
    fun clearFields() {
        _uiState.value = AddPasswordUiState()
    }

    // Inside AddPasswordViewmodel
    fun onAddNewPasswordClick() {
        // 1. Prepare fields for a new credential
        // ... logic to clear fields ...

        // 2. Explicitly set the state to show the sheet
        openSheet = true
    }

    // Also, ensure you have a function to handle dismissal
    fun onSheetDismiss() {
        openSheet = false
    }

}
