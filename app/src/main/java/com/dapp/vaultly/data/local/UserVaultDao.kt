package com.dapp.vaultly.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserVaultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(vault: UserVaultEntity)

    @Query("SELECT * FROM user_vaults WHERE userId = :userId ORDER BY updatedAt ASC")
    fun getVault(userId: String): Flow<UserVaultEntity?>

    @Query("SELECT cid FROM user_vaults WHERE userId = :userId")
    suspend fun getCid(userId: String) : String
    @Query("DELETE FROM user_vaults WHERE userId = :userId")
    suspend fun deleteVault(userId: String)
}