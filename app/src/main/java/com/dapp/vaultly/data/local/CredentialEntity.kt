package com.dapp.vaultly.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "credentials")
data class CredentialEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val website: String,
    val cid: String,
    val encryptedBlob: String,   // contains iv+cipher (serialized JSON or your format)
    val createdAt: Long = System.currentTimeMillis()
)