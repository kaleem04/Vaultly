package com.dapp.vaultly.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "user_vaults")
data class UserVaultEntity(
    @PrimaryKey val userId: String,       // unique per user
    val cid: String,                      // Pinata CID for all credentials
    val encryptedBlob: String,            // encrypted JSON array of credentials
    val updatedAt: Long = System.currentTimeMillis()
)
