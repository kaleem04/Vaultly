package com.dapp.vaultly.data.repository

import com.dapp.vaultly.data.local.CredentialsDao
import com.dapp.vaultly.data.local.CredentialEntity
import kotlinx.coroutines.flow.Flow

class CredentialRepository(
    private val dao: CredentialsDao
) {
    fun getCredentials(): Flow<List<CredentialEntity>> = dao.getAll()

    suspend fun insert(entity: CredentialEntity) = dao.insert(entity)

    suspend fun update(entity: CredentialEntity) = dao.update(entity)

    suspend fun deleteByCid(cid: String) = dao.deleteByCid(cid)
}