package com.dapp.vaultly.data.model

data class DashboardUiState(
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false, // For background tasks like refresh
    val credentials: List<Credential> = emptyList(),
    val userMessage: String? = null,
    val polygonTxHash: String? = null // To show the result of addCidToPolygon
)
