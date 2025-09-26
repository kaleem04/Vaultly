package com.dapp.vaultly.data.repository

import android.util.Log
import com.dapp.vaultly.data.local.UserVaultDao
import com.dapp.vaultly.data.local.UserVaultEntity
import com.dapp.vaultly.data.model.Credential
import com.dapp.vaultly.data.model.PinataRequest
import com.dapp.vaultly.data.model.VaultlyContent
import com.dapp.vaultly.data.remote.IpfsGatewayService
import com.dapp.vaultly.data.remote.PinataApiService
import com.dapp.vaultly.util.Constants.getFunctionSelector
import com.dapp.vaultly.util.CryptoUtil
import com.reown.appkit.client.AppKit
import com.reown.appkit.client.models.request.Request
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Utf8String
import javax.crypto.SecretKey
import kotlin.coroutines.resumeWithException

class UserVaultRepository(
    private val vaultDao: UserVaultDao,
    private val pinata: PinataApiService,
    private val ipfsGatewayService: IpfsGatewayService,
    private val secretKey: SecretKey
) {

    // Get all credentials for user
    suspend fun getCredentials(userId: String): List<Credential> {
        val vault = vaultDao.getVault(userId) ?: return emptyList()
        val decryptedJson = CryptoUtil.decryptBlob(vault.encryptedBlob, secretKey)
        val jsonArray = JSONArray(decryptedJson)
        Log.d("@@",jsonArray.toString())
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
        return response.content
    }
    // Add or update a credential
    suspend fun addOrUpdateCredential(userId: String, credential: Credential) : String {
        val vault = vaultDao.getVault(userId)
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

        val encryptedBlob = CryptoUtil.encryptBlob(jsonArray.toString(), secretKey)
        val vaultlycontent = VaultlyContent(encryptedBlob)
        val response = pinata.pinJsonToIpfs(PinataRequest(vaultlycontent))

        vaultDao.insertOrUpdate(
            UserVaultEntity(
                userId = userId,
                cid = response.ipfsHash,
                encryptedBlob = encryptedBlob
            )
        )

        // Optionally: remove old CID
        vault?.let { pinata.unpin(it.cid) }

        return response.ipfsHash
    }

//    suspend fun saveContentInDb(){
//        vaultDao.insertOrUpdate()
//    }

    // Delete a credential
    suspend fun deleteCredential(userId: String, website: String) {
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
    suspend fun saveCid(account: String, contractAddress: String, cid: String): String =
        suspendCancellableCoroutine { cont ->
            try {
                val function = Function(
                    "setCID",
                    listOf(Utf8String(cid)),
                    emptyList()
                )
                val data = FunctionEncoder.encode(function)

                val txObject = mapOf(
                    "from" to account,
                    "to" to contractAddress,
                    "data" to data,
                    "value" to "0x0"
                )

                val params = listOf(txObject).toString()

                val request = Request(
                    method = "eth_sendTransaction",
                    params = params
                )

                AppKit.request(
                    request,
                    onSuccess = { txHash ->
                        cont.resume(txHash.toString()) {}
                    },
                    onError = { error ->
                        cont.resumeWithException(Exception(error))
                    }
                )
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }


}
