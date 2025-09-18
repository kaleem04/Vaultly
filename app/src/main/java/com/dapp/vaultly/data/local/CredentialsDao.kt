package com.dapp.vaultly.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dapp.vaultly.data.model.CredentialEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CredentialsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(credential: CredentialEntity)

    @Query("SELECT * FROM CredentialEntity")
    fun getAll(): Flow<List<CredentialEntity>>
}