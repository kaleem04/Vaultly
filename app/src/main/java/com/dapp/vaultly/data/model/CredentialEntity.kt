package com.dapp.vaultly.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class CredentialEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val website: String, // optional for UI search
    val cid: String,
    val createdAt : Long = System.currentTimeMillis()
)
