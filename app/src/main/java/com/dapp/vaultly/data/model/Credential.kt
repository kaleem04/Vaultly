package com.dapp.vaultly.data.model

/**
 * Type of credential stored in the vault.
 */
enum class CredentialType {
    PASSWORD,
    NOTE
}

/**
 * Minimal credential model used across the autofill repository and UI.
 */
data class Credential(
    val id: Int = 0,
    val website: String,
    val username: String,
    val password: String,
    val note: String,
    val type: CredentialType = CredentialType.PASSWORD
)
