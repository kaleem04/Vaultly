package com.dapp.vaultly.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dapp.vaultly.data.model.Credential
import com.dapp.vaultly.data.model.UiState
import com.dapp.vaultly.data.repository.UserVaultRepository
import com.dapp.vaultly.util.Constants.CONTRACT_ADDRESS
import com.reown.appkit.client.AppKit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewmodel @Inject constructor(
    private val vaultRepo: UserVaultRepository, // optional if needed
) : ViewModel() {

    private val _credentials = MutableStateFlow<UiState<List<Credential>>>(UiState.Idle)
    val credentials: StateFlow<UiState<List<Credential>>> = _credentials.asStateFlow()

    private val _addPasswordUiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val addPasswordUiState: StateFlow<UiState<Unit>> = _addPasswordUiState.asStateFlow()

    private val _blocchainState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val blockchain: StateFlow<UiState<String>> = _blocchainState.asStateFlow()

    init {
        loadCredentials(AppKit.getAccount()?.address ?: "")
    }

    fun loadCredentials(userId: String) {
        viewModelScope.launch {
            _credentials.value = UiState.Loading
            try {

                val list = vaultRepo.getCredentials(userId)
                _credentials.value = UiState.Success(list)
                getCidFromPolygon(userWalletAddress = userId)
            } catch (e: Exception) {
                _credentials.value = UiState.Error(e.message ?: "Failed To Load Credentials")
                Log.e("DashboardVM", "Error loading credentials", e)
            }
        }
    }

    fun addOrUpdateCredential(userId: String, credential: Credential) {
        viewModelScope.launch {
            _addPasswordUiState.value = UiState.Loading
            try {

                val cid = vaultRepo.addOrUpdateCredential(userId, credential)
                loadCredentials(userId)
                _addPasswordUiState.value = UiState.Success(Unit)
                if (cid.isNotEmpty()) {
                    addCidToPolygon(userWalletAddress = userId, cid = cid)
                }
            } catch (e: Exception) {
                _addPasswordUiState.value = UiState.Error(e.message ?: "Failed To Add Credentials")
                Log.e("AddPasswordVM", "Error adding/updating credential", e)

            }
        }
    }


    fun deleteCredential(userId: String, website: String) {
        viewModelScope.launch {
            _credentials.value = UiState.Loading
            try {
                vaultRepo.deleteCredential(userId, website)
                loadCredentials(userId) // refresh list
            } catch (e: Exception) {
                _credentials.value = UiState.Error(e.message ?: "Failed To Delete Credential")
                Log.e("DashboardVM", "Error deleting credential", e)
            }
        }
    }


    fun addCidToPolygon(userWalletAddress: String, cid: String) {
        viewModelScope.launch {
            _blocchainState.value = UiState.Loading
            try {
                vaultRepo.saveCid(account = userWalletAddress, CONTRACT_ADDRESS, cid)
                val success = "Cid Added to Blockchain Successfully"
                _blocchainState.value = UiState.Success(success)
            } catch (e: Exception) {
                Log.d("@@", e.message ?: "Failed To Add Cid To Blockchain")
                _blocchainState.value =
                    UiState.Error(e.message ?: "Failed To Add Cid To Blockchain")
            }
        }
    }

    fun getCidFromPolygon(userWalletAddress: String) {
        viewModelScope.launch {
            _blocchainState.value = UiState.Loading
            try {
                val data = vaultRepo.getCid(userWalletAddress, CONTRACT_ADDRESS)
                _blocchainState.value = UiState.Success(data)
                Log.d("@@","onSUCCESS GETCID Failed To Get Cid From Blockchain")
            } catch (e: Exception) {
                Log.d("@@", e.message ?: "Failed To Get Cid From Blockchain")
                _blocchainState.value =
                    UiState.Error(e.message ?: "Failed To Get Cid From Blockchain")
            }
        }
    }
}
