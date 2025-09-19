package com.dapp.vaultly.ui.viewmodels


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dapp.vaultly.data.model.Credential
import com.dapp.vaultly.data.model.CredentialEntity
import com.dapp.vaultly.data.model.CredentialUiState
import com.dapp.vaultly.data.repository.CredentialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CredentialViewmodel @Inject constructor(
    private val repository: CredentialRepository
) : ViewModel() {

    // Backing state
    private val _uiState = MutableStateFlow<CredentialUiState>(CredentialUiState.Loading)
    val uiState: StateFlow<CredentialUiState> = _uiState.asStateFlow()

    init {
        observeCredentials()
    }

    private fun observeCredentials() {
        viewModelScope.launch {
            repository.getCredentials()
                .catch { e ->
                    _uiState.value = CredentialUiState.Error(e.message ?: "Unknown error")
                }
                .collect { list ->
                    _uiState.value = CredentialUiState.Success(list)
                }
        }
    }

    fun addCredential(credential: Credential) {
        viewModelScope.launch {
            try {
                repository.addCredential(credential)
            } catch (e: Exception) {
                _uiState.value = CredentialUiState.Error(e.message ?: "Failed to add")
            }
        }
    }

    fun updateCredential(updated: CredentialEntity, new: Credential) {
        viewModelScope.launch {
            try {
                repository.updateCredential(updated.cid, new) // ← you’ll add this in repo
            } catch (e: Exception) {
                _uiState.value = CredentialUiState.Error(e.message ?: "Failed to update")
            }
        }
    }

    fun deleteByCid(cid: String) {
        viewModelScope.launch {
            try {
                repository.deleteCredential(cid)
            } catch (e: Exception) {
                _uiState.value = CredentialUiState.Error(e.message ?: "Failed to delete")
            }
        }
    }

    fun fetchCredential(entity: CredentialEntity, onResult: (Credential?) -> Unit) {
        viewModelScope.launch {
            try {
                val credential = repository.fetchCredential(entity)
                onResult(credential)
            } catch (e: Exception) {
                _uiState.value = CredentialUiState.Error(e.message ?: "Failed to fetch")
                onResult(null)
            }
        }
    }
}
