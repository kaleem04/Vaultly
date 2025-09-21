package com.dapp.vaultly.ui.viewmodels
//
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.dapp.vaultly.data.model.Credential
//import com.dapp.vaultly.data.local.CredentialEntity
//import com.dapp.vaultly.data.model.CredentialUiState
//import com.dapp.vaultly.data.repository.CredentialRepository
//import com.dapp.vaultly.util.CryptoUtil
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.flow.catch
//import kotlinx.coroutines.launch
//import org.json.JSONObject
//import javax.crypto.SecretKey
//import javax.inject.Inject
//@HiltViewModel
//class CredentialViewModel @Inject constructor(
//    private val repo: CredentialRepository,
//    private val secretKey: SecretKey
//) : ViewModel() {
//
//    private val _state = MutableStateFlow<List<Credential>>(emptyList())
//    val state: StateFlow<List<Credential>> = _state
//
//    init {
//        viewModelScope.launch {
//            repo.getCredentials().collect { entities ->
//                // Decrypt each entity for UI
//                val creds = entities.map { entity ->
//                    val decrypted = CryptoUtil.decrypt(entity.iv, entity.cipher, secretKey)
//                    val obj = JSONObject(decrypted)
//                    Credential(
//                        website = obj.getString("website"),
//                        username = obj.getString("username"),
//                        password = obj.getString("password"),
//                        note = obj.getString("note"),
//                        id = entity.id
//                    )
//                }
//                _state.value = creds
//            }
//        }
//    }
//
//    fun addCredential(credential: Credential) {
//        viewModelScope.launch {
//            repo.insert(credential)
//        }
//    }
//
//    fun updateCredential(oldCid: String, updated: Credential) {
//        viewModelScope.launch {
//            repo.updateCredential(oldCid, updated)
//        }
//    }
//
//    fun deleteCredential(cid: String) {
//        viewModelScope.launch {
//            repo.deleteCredential(cid)
//        }
//    }
//}
