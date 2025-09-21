package com.dapp.vaultly.data.repository

import com.dapp.vaultly.data.local.CredentialsDao
import com.dapp.vaultly.data.model.Credential
import com.dapp.vaultly.data.local.CredentialEntity
import com.dapp.vaultly.data.model.PinataRequest
import com.dapp.vaultly.data.model.VaultlyContent
import com.dapp.vaultly.data.remote.PinataApi
import com.dapp.vaultly.util.CryptoUtil
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import javax.crypto.SecretKey

class CredentialRepository(
    private val dao: CredentialsDao
) {
    fun getCredentials(): Flow<List<CredentialEntity>> = dao.getAll()

    suspend fun insert(entity: CredentialEntity) = dao.insert(entity)

    suspend fun update(entity: CredentialEntity) = dao.update(entity)

    suspend fun deleteByCid(cid: String) = dao.deleteByCid(cid)
}