package com.dapp.vaultly.data.repository

import android.content.Context
import android.util.Log
import com.dapp.vaultly.data.local.AesKeyStorage
import com.dapp.vaultly.data.local.UserVaultDao
import com.dapp.vaultly.data.local.UserVaultEntity
import com.dapp.vaultly.data.model.Credential
import com.dapp.vaultly.data.model.PinataMetadata
import com.dapp.vaultly.data.model.PinataRequest
import com.dapp.vaultly.data.model.VaultlyContent
import com.dapp.vaultly.data.remote.IpfsGatewayService
import com.dapp.vaultly.data.remote.PinataApiService
import com.dapp.vaultly.util.Constants.WALLET_ADDRESS
import com.dapp.vaultly.util.CryptoUtil
import com.reown.appkit.client.AppKit
import com.reown.appkit.client.models.request.Request
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Utf8String
import javax.crypto.SecretKey
import kotlin.coroutines.resumeWithException

class UserVaultRepository(
    private val vaultDao: UserVaultDao,
    private val pinata: PinataApiService,
    private val ipfsGatewayService: IpfsGatewayService,
    @ApplicationContext private val context: Context
) {


    private suspend fun getSecretKey(): SecretKey? {
        val secretKey = AesKeyStorage.readKey(context).firstOrNull()
        if (secretKey == null) {
            Log.e("UserVaultRepository", "Secret key not found. Cannot perform cryptographic operations.")
        }
        return secretKey
    }

    // Get all credentials for user

    // THIS IS THE KEY CHANGE
    fun getCredentials(userId: String): Flow<List<Credential>> {
        // 1. Get a Flow of the raw database entity from the DAO.
        return vaultDao.getVault(userId)
            .map { vault -> // 2. Use .map to transform the data inside the flow.
                if (vault == null) {
                    return@map emptyList<Credential>() // If no vault, emit an empty list.
                }

                val secretKey = getSecretKey()
                if (secretKey == null) {
                    Log.e(
                        "UserVaultRepository",
                        "Secret key is missing, cannot decrypt credentials."
                    )
                    return@map emptyList<Credential>() // If no key, emit an empty list.
                }

                try {
                    // 3. Perform the same decryption logic as before.
                    val decryptedJson = CryptoUtil.decryptBlob(vault.encryptedBlob, secretKey)
                    val jsonArray = JSONArray(decryptedJson)
                    Log.d("@@", "Decrypted credentials from DB Flow: $jsonArray")

                    // 4. Transform JSON into a List and this list will be the new emission.
                    List(jsonArray.length()) { i ->
                        val obj = jsonArray.getJSONObject(i)
                        Credential(
                            website = obj.getString("website"),
                            username = obj.getString("username"),
                            password = obj.getString("password"),
                            note = obj.getString("note")
                        )
                    }
                } catch (e: Exception) {
                    Log.e("UserVaultRepository", "Failed to parse decrypted JSON", e)
                    return@map emptyList<Credential>() // If JSON is corrupted, emit an empty list.
                }
            }
    }

    suspend fun getContentFromPinata(cid: String): String {
        val response = ipfsGatewayService.getJsonFromIpfs(cid)
        Log.d("@@", response.toString())
        return response.vault.content
    }

    // Add or update a credential
    // In UserVaultRepository.kt

    suspend fun addOrUpdateCredential(
        userId: String,
        credential: Credential
    ): Pair<String, String> {
        val secretKey = getSecretKey() ?: return Pair("", "")

        // 1. Get the CURRENT list of credentials by taking the first emission from your new reactive flow.
        // This replaces the old way of manually getting the vault and decrypting it here.
        val currentList = getCredentials(userId).firstOrNull() ?: emptyList()

        // 2. Merge the new credential into the list. (This logic is correct)
        val updatedList = currentList.toMutableList()
        val index = updatedList.indexOfFirst { it.website == credential.website }
        if (index >= 0) {
            updatedList[index] = credential
        } else {
            updatedList.add(credential)
        }

        // 3. Convert back to JSON and encrypt. (This logic is correct)
        val jsonArray = JSONArray()
        updatedList.forEach { c ->
            jsonArray.put(JSONObject().apply {
                put("website", c.website)
                put("username", c.username)
                put("password", c.password)
                put("note", c.note)
            })
        }
        val jsonString = jsonArray.toString()
        val newEncryptedBlob = CryptoUtil.encryptBlob(jsonString, secretKey)

        // 4. Create the content to be pinned to IPFS.
        val vaultlyContent = VaultlyContent(newEncryptedBlob)
        val content = mapOf(
            "wallet" to WALLET_ADDRESS, // You should pass this in or get it reliably
            "vault" to vaultlyContent
        )
        val pinataMetadata = PinataMetadata(name = "vault_${userId}.json" /*...*/)
        val request = PinataRequest(content, pinataMetadata)

        // 5. Pin the new content to IPFS.
        val response = pinata.pinJsonToIpfs(request)
        val newCid = response.ipfsHash

        // 6. Get the old CID before we save the new one.
        val oldCid = vaultDao.getCid(userId)

        // 7. Atomically save the NEW state to the database.
        // This single call will trigger your getCredentials flow to automatically update the UI.
        vaultDao.insertOrUpdate(
            UserVaultEntity(
                userId = userId,
                cid = newCid,
                encryptedBlob = newEncryptedBlob
            )
        )

        // 8. Clean up the old IPFS pin.
        if (!oldCid.isNullOrEmpty() && oldCid != newCid) {
            try {
                pinata.unpin(oldCid)
            } catch (e: Exception) {
                Log.e("UserVaultRepository", "Failed to unpin old CID: $oldCid", e)
            }
        }

        // 9. Return the results.
        return Pair(newCid, newEncryptedBlob)
    }


    suspend fun saveContentInDb(userId: String, cid: String, content: String) {
        val userVaultEntity = UserVaultEntity(
            userId = userId,
            cid = cid,
            encryptedBlob = content
        )
        vaultDao.insertOrUpdate(userVaultEntity)
    }

    // Delete a credential
    // In UserVaultRepository.kt

    suspend fun deleteCredential(userId: String, website: String) {
        val secretKey = getSecretKey() ?: return

        // 1. Get the current list from the reactive flow.
        val currentList = getCredentials(userId).firstOrNull() ?: return

        // 2. Filter out the credential to be deleted.
        val updatedList = currentList.filter { it.website != website }

        // 3. Convert the new, smaller list back to JSON and encrypt it.
        val jsonArray = JSONArray()
        updatedList.forEach { c -> /* ... same as addOrUpdateCredential ... */ }
        val newEncryptedBlob = CryptoUtil.encryptBlob(jsonArray.toString(), secretKey)

        // 4. Pin the new state to IPFS.
        val vaultlyContent = VaultlyContent(newEncryptedBlob)
        val request = PinataRequest(vaultlyContent) // Add metadata for better management
        val response = pinata.pinJsonToIpfs(request)
        val newCid = response.ipfsHash

        // 5. Get the old CID before updating.
        val oldCid = vaultDao.getCid(userId) ?: ""

        // 6. Save the new state to the database. This triggers the UI update.
        vaultDao.insertOrUpdate(
            UserVaultEntity(
                userId = userId,
                cid = newCid,
                encryptedBlob = newEncryptedBlob
            )
        )

        // 7. Unpin the old content.
        if (oldCid.isNotEmpty() && oldCid != newCid) {
            pinata.unpin(oldCid)
        }
    }


    suspend fun getCid(): String {
        return vaultDao.getCid(AppKit.getAccount()?.address ?: "")
    }

    suspend fun saveCid(account: String, contractAddress: String, cid: String): String =
        suspendCancellableCoroutine { cont ->
            try {
                val function = Function(
                    "setCID",
                    listOf(Utf8String(cid)),
                    emptyList()
                )
                val data = FunctionEncoder.encode(function)

                // Build tx JSON
                val txObject = JSONObject().apply {
                    put("from", account)
                    put("to", contractAddress)
                    put("data", data)
                    put("value", "0x0")
                    put("gas", "0x2dc6c0") // ~3M gas
                }

                // Params must be a JSON array string: [ { ... } ]
                val params = JSONArray().apply {
                    put(txObject)
                }.toString()

                Log.d("SaveCid", "Transaction params JSON: $params")

                val request = Request(
                    method = "eth_sendTransaction",
                    params = params,   // ✅ pass JSON string
                    expiry = null
                )

                Log.d("SaveCid", "Request: method=${request.method}, params=${request.params}")

                AppKit.request(
                    request,
                    onSuccess = { txHash ->
                        Log.d("SaveCid", "Transaction success: $txHash")
                        cont.resume(txHash.toString()) {}
                    },
                    onError = { error ->
                        Log.e("SaveCid", "Transaction error: $error")
                        cont.resumeWithException(Exception(error))
                    }
                )
            } catch (e: Exception) {
                Log.e("SaveCid", "Exception while sending tx", e)
                cont.resumeWithException(e)
            }
        }


}

