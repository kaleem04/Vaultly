package com.dapp.vaultly.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dapp.vaultly.data.model.Credential
import com.dapp.vaultly.data.model.CredentialEntity
import com.dapp.vaultly.data.repository.CredentialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewmodel @Inject constructor(
    private val repo: CredentialRepository
) : ViewModel() {

    // List screen state: only website + id + cid (safe to show)
    val credentials: StateFlow<List<CredentialEntity>> =
        repo.getCredentials()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Detail/decrypted state
    private val _selectedCredential = MutableStateFlow<Credential?>(null)
    val selectedCredential: StateFlow<Credential?> = _selectedCredential

    fun selectCredential(entity: CredentialEntity) {
        viewModelScope.launch {
            val decrypted = repo.fetchCredential(entity)
            _selectedCredential.value = decrypted
        }
    }

    fun clearSelection() {
        _selectedCredential.value = null
    }

    fun deleteCredential(cid: String) {
        viewModelScope.launch {
            repo.deleteCredential(cid)
        }
    }
}