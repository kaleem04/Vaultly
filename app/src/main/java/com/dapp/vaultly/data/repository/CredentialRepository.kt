package com.dapp.vaultly.data.repository

import com.dapp.vaultly.data.local.CredentialsDao
import com.dapp.vaultly.data.model.Credential
import com.dapp.vaultly.data.model.CredentialEntity
import com.dapp.vaultly.data.model.PinataRequest
import com.dapp.vaultly.data.model.VaultlyContent
import com.dapp.vaultly.data.remote.PinataApi
import com.dapp.vaultly.util.CryptoUtil
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import javax.crypto.SecretKey

class CredentialRepository(
    private val dao: CredentialsDao,
    private val pinata: PinataApi,
    private val secretKey: SecretKey
) {
    fun getCredentials(): Flow<List<CredentialEntity>> = dao.getAll()

    suspend fun addCredential(credential: Credential) {
        // 1. Convert to JSON
        val json = JSONObject().apply {
            put("website", credential.website)
            put("username", credential.username)
            put("password", credential.password)
            put("note", credential.note)
        }.toString()

        // 2. Encrypt -> (iv, cipher)
        val (iv, cipher) = CryptoUtil.encrypt(json, secretKey)

        // 3. Upload to Pinata
        val response = pinata.pinJsonToIpfs(
            PinataRequest(VaultlyContent(iv = iv, cipher = cipher))
        )

        // 4. Save in DB
        dao.insert(
            CredentialEntity(
                website = credential.website,
                cid = response.ipfsHash,
                iv = iv,
                cipher = cipher
            )
        )
    }

    suspend fun fetchCredential(entity: CredentialEntity): Credential {
        // 1. Fetch encrypted bundle
        //val encrypted = pinata.getFromIpfs(cid)

        // 2. Decrypt using iv + cipher
        val decryptedJson = CryptoUtil.decrypt(entity.iv, entity.cipher, secretKey)
        val obj = JSONObject(decryptedJson)

        // 3. Map back
        return Credential(
            website = obj.getString("website"),
            username = obj.getString("username"),
            password = obj.getString("password"),
            note = obj.getString("note")
        )
    }

    suspend fun updateCredential(oldCid: String, updated: Credential) {
        // Reuse addCredential logic
        val json = JSONObject().apply {
            put("website", updated.website)
            put("username", updated.username)
            put("password", updated.password)
            put("note", updated.note)
        }.toString()

        val (iv, cipher) = CryptoUtil.encrypt(json, secretKey)
        val response = pinata.pinJsonToIpfs(
            PinataRequest(VaultlyContent(iv = iv, cipher = cipher))
        )


        dao.update(
            CredentialEntity(
                website = updated.website,
                cid = response.ipfsHash,
                iv = iv,
                cipher = cipher
            )
        )

        // optional: remove old pin
        pinata.unpin(oldCid)
    }

    suspend fun deleteCredential(cid: String) {
        pinata.unpin(cid)
        dao.deleteByCid(cid)
    }
}


