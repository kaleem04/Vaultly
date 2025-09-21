package com.dapp.vaultly.data.model

data class Credential(
    val id: Int = 0,
    val website: String,
    val username: String,
    val password: String,
    val note: String,
    val createdAt : Long = System.currentTimeMillis()
)
