package com.dapp.vaultly.data.model

data class DashboardUiState(
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false, // For background tasks like refresh
    val credentials: List<Credential> = emptyList(),
    val filteredCredentials: List<Credential> = emptyList(),
    val searchQuery: String = "",
    val activeTab: CredentialType? = null, // null means show all
    val userMessage: String? = null,
    val polygonTxHash: String? = null // To show the result of addCidToPolygon
)
