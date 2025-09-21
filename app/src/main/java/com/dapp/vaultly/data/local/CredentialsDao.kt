package com.dapp.vaultly.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dapp.vaultly.data.local.CredentialEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CredentialsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(credential: CredentialEntity)

    @Query("SELECT * FROM credentials ORDER BY createdAt DESC")
    fun getAll(): Flow<List<CredentialEntity>>

    @Query("DELETE FROM credentials WHERE cid = :cid")
    suspend fun deleteByCid(cid: String)

    @Update
    suspend fun update(credential: CredentialEntity)
}