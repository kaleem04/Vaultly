package com.dapp.vaultly.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dapp.vaultly.data.local.UserVaultEntity
import com.dapp.vaultly.data.model.Credential
import com.dapp.vaultly.data.model.UiState
import com.dapp.vaultly.data.repository.PolygonRepository
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
    private val vaultRepo: UserVaultRepository,
    private val polygonRepository: PolygonRepository
) : ViewModel() {

    private val _credentials = MutableStateFlow<UiState<List<Credential>>>(UiState.Idle)
    val credentials: StateFlow<UiState<List<Credential>>> = _credentials.asStateFlow()

    private val _addPasswordUiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val addPasswordUiState: StateFlow<UiState<Unit>> = _addPasswordUiState.asStateFlow()

    private val _blockchainState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val blockchain: StateFlow<UiState<String>> = _blockchainState.asStateFlow()

    init {
        val userId = AppKit.getAccount()?.address ?: ""
        // 1. Load from Room (offline-first)
        loadCredentials(userId)
        // 2. Refresh from Polygon + Pinata (network sync)
        refreshFromBlockchain(userId)
    }

    /** Load from Room only */
    private fun loadCredentials(userId: String) {
        viewModelScope.launch {
            _credentials.value = UiState.Loading
            try {
                val list = vaultRepo.getCredentials(userId)
                _credentials.value = UiState.Success(list)
            } catch (e: Exception) {
                _credentials.value = UiState.Error(e.message ?: "Failed To Load Credentials")
                Log.e("DashboardVM", "Error loading credentials", e)
            }
        }
    }
    fun getCredentialById(id: String): Credential? {
        return _credentials.value.find { it.id == id }
    }

    /** Force sync: Polygon → Pinata → DB */
     fun refreshFromBlockchain(userId: String) {
        viewModelScope.launch {
            _blockchainState.value = UiState.Loading
            try {
                // 1. Get CID from Polygon
               // val cid = polygonRepository.getCid(userId)
//                _blockchainState.value = UiState.Success(cid)
//                Log.d("DashboardVM", "CID fetched: $cid")
//
//                if (cid.isNotEmpty()) {
//                    // 2. Fetch content from Pinata
//                    val content = vaultRepo.getContentFromPinata(cid)
//                    Log.d("@@",content.toString())
//                    // 3. Store into Room (UI updates automatically from DB observer)
//
//                    vaultRepo.saveContentInDb(userId,cid,content)
//                    Log.d("DashboardVM", "Content saved in DB successfully")
//                    loadCredentials(userId)
//                }
            } catch (e: Exception) {
                Log.e("DashboardVM", "Error syncing from blockchain", e)
                _blockchainState.value =
                    UiState.Error(e.message ?: "Failed To Sync From Blockchain")
            }
        }
    }

    fun addOrUpdateCredential(userId: String, credential: Credential) {
        viewModelScope.launch {
            _addPasswordUiState.value = UiState.Loading
            try {
                val cid = vaultRepo.addOrUpdateCredential(userId, credential)
                _addPasswordUiState.value = UiState.Success(Unit)

                if (cid.isNotEmpty()) {
                    // 2. Fetch content from Pinata
                    val content = vaultRepo.getContentFromPinata(cid)
                    Log.d("@@","$content")
                    // 3. Store into Room (UI updates automatically from DB observer)
                    if(content.isNotEmpty()) {
                        vaultRepo.saveContentInDb(userId, cid, content)
                        Log.d("DashboardVM", "Content saved in DB successfully")
                    }
                }
                loadCredentials(userId)
                _addPasswordUiState.value = UiState.Idle
            } catch (e: Exception) {
                _addPasswordUiState.value = UiState.Error(e.message ?: "Failed To Add Credentials")
                Log.e("DashboardVM", "Error adding/updating credential", e)
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

    private fun addCidToPolygon(userWalletAddress: String, cid: String) {
        viewModelScope.launch {
            try {
                vaultRepo.saveCid(account = userWalletAddress, CONTRACT_ADDRESS, cid)
                Log.d("DashboardVM", "Cid Added to Blockchain Successfully")
            } catch (e: Exception) {
                Log.e("DashboardVM", "Failed To Add Cid To Blockchain", e)
            }
        }
    }
}
