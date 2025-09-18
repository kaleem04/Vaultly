package com.dapp.vaultly.data.repository

import com.dapp.vaultly.data.local.CredentialsDao
import com.dapp.vaultly.data.model.Credential
import com.dapp.vaultly.data.model.CredentialEntity
import com.dapp.vaultly.util.CryptoUtil
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import javax.crypto.SecretKey

class CredentialRepository(
    private val dao: CredentialsDao,
    private val pinata: PinataDataSource,
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

        // 2. Encrypt
        val encrypted = CryptoUtil.encrypt(json, secretKey)

        // 3. Upload encrypted JSON to Pinata
        val cid = pinata.uploadEncrypted(encrypted)

        // 4. Save minimal info in DB
        dao.insert(
            CredentialEntity(
                website = credential.website,
                cid = cid
            )
        )
    }

    suspend fun fetchCredential(cid: String): Credential {
        val encrypted = pinata.fetchEncrypted(cid)
        val decryptedJson = CryptoUtil.decrypt(encrypted, secretKey)
        val obj = JSONObject(decryptedJson)
        return Credential(
            website = obj.getString("website"),
            username = obj.getString("username"),
            password = obj.getString("password"),
            note = obj.getString("note")
        )
    }
}
