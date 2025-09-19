package com.dapp.vaultly.data.model

sealed class CredentialUiState {
    object Loading : CredentialUiState()
    data class Success(val credentials: List<CredentialEntity>) : CredentialUiState()
    data class Error(val message: String) : CredentialUiState()
}
