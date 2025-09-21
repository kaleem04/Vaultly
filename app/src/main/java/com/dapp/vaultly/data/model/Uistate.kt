package com.dapp.vaultly.data.model

sealed class UiState<out T> {
    object Idle : UiState<Nothing>()          // default state
    object Loading : UiState<Nothing>()       // show progress indicator
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

