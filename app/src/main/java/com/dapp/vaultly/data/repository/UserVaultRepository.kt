package com.dapp.vaultly.data.repository

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.flow.firstOrNull
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
            Log.e(
                "UserVaultRepository",
                "Secret key not found. Cannot perform cryptographic operations."
            )
        }
        return secretKey
    }

    // Get all credentials for user
    suspend fun getCredentials(userId: String): List<Credential> {
        val vault = vaultDao.getVault(userId) ?: return emptyList()
        if (vault.cid.isNotEmpty()) {

        }
        val secretKey = getSecretKey() ?: return emptyList()

        val decryptedJson = CryptoUtil.decryptBlob(vault.encryptedBlob, secretKey)
        val jsonArray = JSONArray(decryptedJson)
        Log.d("@@", jsonArray.toString())
        return List(jsonArray.length()) { i ->
            val obj = jsonArray.getJSONObject(i)
            Credential(
                website = obj.getString("website"),
                username = obj.getString("username"),
                password = obj.getString("password"),
                note = obj.getString("note")
            )
        }
    }

    suspend fun getContentFromPinata(cid: String): String {
        val response = ipfsGatewayService.getJsonFromIpfs(cid)
        Log.d("@@", response.toString())
        return response.vault.content
    }

    // Add or update a credential
    suspend fun addOrUpdateCredential(
        userId: String,
        credential: Credential
    ): Pair<String, String> {
        val vault = vaultDao.getVault(userId)
        val secretKey = getSecretKey() ?: return Pair("", "")
        val currentList = vault?.let {

            val decryptedJson = CryptoUtil.decryptBlob(it.encryptedBlob, secretKey)
            val arr = JSONArray(decryptedJson)
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                Credential(
                    website = obj.getString("website"),
                    username = obj.getString("username"),
                    password = obj.getString("password"),
                    note = obj.getString("note")
                )
            }
        } ?: emptyList()

        // Merge/update credentials
        val updatedList = currentList.toMutableList()
        val index = updatedList.indexOfFirst { it.website == credential.website }
        if (index >= 0) updatedList[index] = credential
        else updatedList.add(credential)

        // Convert back to JSON array
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
        Log.d("@@", "encrypt secret : $jsonString")
        val encryptedBlob = CryptoUtil.encryptBlob(jsonString, secretKey)

        // The content that will be pinned to IPFS
        val vaultlyContent = VaultlyContent(encryptedBlob)
        val content = mapOf(
            "wallet" to WALLET_ADDRESS,
            "vault" to vaultlyContent
        )
        val pinataMetadata = PinataMetadata(
            name = "vault_${WALLET_ADDRESS}.json",
            keyValues = mapOf(
                "wallet" to WALLET_ADDRESS,
                "type" to "vault"
            )
        )
        val request = PinataRequest(content, pinataMetadata)

        // Pin the new content to IPFS
        val response = pinata.pinJsonToIpfs(request)

        // This database insertion is now redundant because it's handled in the ViewModel.
        // You can leave it for now or remove it if you're confident in the new flow.
        vaultDao.insertOrUpdate(
            UserVaultEntity(
                userId = userId,
                cid = response.ipfsHash,
                encryptedBlob = encryptedBlob // Storing the newly created blob
            )
        )

        // Optionally: remove old CID
        vault?.cid?.let { oldCid ->
            if (oldCid.isNotEmpty() && oldCid != response.ipfsHash) {
                try {
                    pinata.unpin(oldCid)
                } catch (e: Exception) {
                    Log.e("UserVaultRepository", "Failed to unpin old CID: $oldCid", e)
                }
            }
        }

        // Return both the new CID and the encrypted content
        return Pair(response.ipfsHash, encryptedBlob)
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
    suspend fun deleteCredential(userId: String, website: String) {
        val secretKey = getSecretKey() ?: return
        val vault = vaultDao.getVault(userId) ?: return
        val decryptedJson = CryptoUtil.decryptBlob(vault.encryptedBlob, secretKey)
        val jsonArray = JSONArray(decryptedJson)

        val newArray = JSONArray()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            if (obj.getString("website") != website) newArray.put(obj)
        }

        val encryptedBlob = CryptoUtil.encryptBlob(newArray.toString(), secretKey)
        val vaultlycontent = VaultlyContent(encryptedBlob)
        val response = pinata.pinJsonToIpfs(PinataRequest(vaultlycontent))

        vaultDao.insertOrUpdate(
            UserVaultEntity(
                userId = userId,
                cid = response.ipfsHash,
                encryptedBlob = encryptedBlob
            )
        )

        pinata.unpin(vault.cid) // clean up old
    }

    suspend fun getCid() : String {
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

