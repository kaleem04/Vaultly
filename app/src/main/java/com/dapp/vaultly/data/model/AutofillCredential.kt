package com.dapp.vaultly.data.model

/**
 * Model returned by the autofill repository to the UI.
 */
data class AutofillCredential(
    val id: String,
    val website: String,
    val username: String,
    val password: String,
    val note: String? = null
)

