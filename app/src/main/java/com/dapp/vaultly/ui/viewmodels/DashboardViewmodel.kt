package com.dapp.vaultly.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dapp.vaultly.data.model.Credential
import com.dapp.vaultly.data.model.DashboardUiState
import com.dapp.vaultly.data.repository.PolygonRepository
import com.dapp.vaultly.data.repository.UserVaultRepository
import com.dapp.vaultly.util.Constants.CONTRACT_ADDRESS
import com.reown.appkit.client.AppKit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class DashboardViewmodel @Inject constructor(
    private val vaultRepo: UserVaultRepository,
    private val polygonRepository: PolygonRepository
) : ViewModel() {

    // 1. SINGLE SOURCE OF TRUTH: All old UiState flows are removed. This is all you need.
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    // 2. CENTRALIZED ERROR HANDLER: Simplifies every function.
    private val errorHandler = CoroutineExceptionHandler { _, exception ->
        Log.e("DashboardVM", "A coroutine error occurred", exception)
        _uiState.update {
            it.copy(
                isLoading = false,
                isSyncing = false,
                userMessage = exception.message ?: "An unexpected error occurred."
            )
        }
    }

    // 3. UI-DRIVEN INITIALIZATION: The init block is removed. The UI calls onScreenReady() when it's time to load data.
    fun onScreenReady() {
        val userId = AppKit.getAccount()?.address
        if (userId == null) {
            _uiState.update { it.copy(userMessage = "User not logged in.") }
            return
        }
        // These two functions will now manage the screen's state.
        observeLocalCredentials(userId)
        refreshFromBlockchain(userId)
    }

    /**
     * REACTIVE DATA LOADING: Observes the local Room database via a Flow.
     * The UI will automatically update whenever the data changes in the DB.
     * This is the only function that needs to put credentials into the state.
     */
    private fun observeLocalCredentials(userId: String) {
        vaultRepo.getCredentials(userId) // This now returns a Flow from the repo
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onEach { credentials ->
                _uiState.update {
                    it.copy(isLoading = false, credentials = credentials)
                }
            }
            .catch { exception ->
                Log.e("DashboardVM", "Error observing credentials", exception)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = "Failed to load credentials: ${exception.message}"
                    )
                }
            }
            .launchIn(viewModelScope) // Ensures the flow is collected for the ViewModel's lifecycle
    }

    /**
     * NETWORK SYNC: Fetches the latest CID from Polygon, gets content from Pinata,
     * and saves it to the database. The `observeLocalCredentials` flow will automatically handle the UI update.
     */
    fun refreshFromBlockchain(userId: String) {
        // Use the centralized error handler.
        viewModelScope.launch(errorHandler) {
            _uiState.update { it.copy(isSyncing = true, userMessage = null) }

            val cid = polygonRepository.getCid(userId)

            if (cid.isNotEmpty()) {
                val content = vaultRepo.getContentFromPinata(cid)
                vaultRepo.saveContentInDb(userId, cid, content)
                // NO MORE manual call to loadCredentials()! It's automatic now.
            }
            // The isSyncing flag is set to false, and a success message can be shown.
            _uiState.update { it.copy(isSyncing = false, userMessage = "Sync Complete") }
        }
    }

    /**
     * ACTION: Adds or updates a credential. It calls the repository, which updates the DB.
     * The reactive flow does the rest.
     */
    fun addOrUpdateCredential(userId: String, credential: Credential) {
        viewModelScope.launch(errorHandler) {
            _uiState.update { it.copy(isLoading = true, userMessage = null) }
            vaultRepo.addOrUpdateCredential(userId, credential)
            // isLoading will be set to false automatically by observeLocalCredentials.
            _uiState.update { it.copy(userMessage = "Credential saved.") }
        }
    }

    /**
     * ACTION: Deletes a credential. It calls the repository, which updates the DB.
     * The reactive flow does the rest.
     */
    fun deleteCredential(userId: String, website: String) {
        viewModelScope.launch(errorHandler) {
            _uiState.update { it.copy(isLoading = true, userMessage = null) }
            vaultRepo.deleteCredential(userId, website)
            // isLoading will be set to false automatically by observeLocalCredentials.
            _uiState.update { it.copy(userMessage = "Credential deleted.") }
        }
    }

    /**
     * ACTION: Saves the current CID to the blockchain.
     */

    fun addCidToPolygon() {
        val userId = AppKit.getAccount()?.address
        if (userId == null) {
            _uiState.update { it.copy(userMessage = "Cannot sync: User not found.") }
            return
        }

        // This launch block will now correctly handle the UI and background tasks
        viewModelScope.launch(errorHandler) {
            // --- Part 1: Background Prep ---
            // Switch to a background thread to do any heavy prep work.
            val currentCid = withContext(Dispatchers.IO) {
                // Get the CID from the database or wherever it's stored.
                // This is a disk operation, so it belongs on a background thread.
                vaultRepo.getCid()
            }

            // --- Part 2: Main Thread UI Action ---
            // Now we are back on the main thread automatically.
            // It is now safe to call the function that will launch MetaMask.

            // Update the UI right before showing MetaMask.
            // The freeze was happening because the UI couldn't even update to this state.
            _uiState.update { it.copy(isSyncing = true, userMessage = "Waiting for signature...") }

            // This call will now work correctly because it's on the main thread.
            // It will launch MetaMask, and the coroutine will suspend until you return.
            val txHash = vaultRepo.saveCid(userId, CONTRACT_ADDRESS, currentCid)

            // --- Part 3: Main Thread Result Handling ---
            // After MetaMask returns, the 'saveCid' suspend function resumes.
            // We are still on the main thread.
            if (txHash.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        userMessage = "Sync to Polygon successful!",
                        polygonTxHash = txHash
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        userMessage = "Sync failed: Did not receive a transaction hash."
                    )
                }
            }
        }
    }

    /**
     * UI EVENT: The UI calls this function after it has displayed a message,
     * to prevent the message from showing again on configuration change.
     */
    fun userMessageShown() {
        _uiState.update { it.copy(userMessage = null, polygonTxHash = null) }
    }
}
