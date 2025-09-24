package com.dapp.vaultly.data.model

sealed class WalletUiState {
    object Loading : WalletUiState()
    object Welcome : WalletUiState()
    data class DashboardPendingSignature(val walletAddress: String) : WalletUiState()
    object DashboardReady : WalletUiState()
}
