package com.dapp.vaultly.util

import com.reown.appkit.client.AppKit
import com.reown.appkit.client.models.request.Request
import com.reown.appkit.client.models.request.SentRequestResult
import org.json.JSONArray
import javax.crypto.SecretKey

object VaultKeyManager {
    @Volatile
    private var secretKey: SecretKey? = null

    fun setKey(key: SecretKey) {
        secretKey = key
    }

    fun getKey(): SecretKey? = secretKey

    fun clear() {
        secretKey = null
    }

    fun isUnlocked(): Boolean = secretKey != null

    fun requestPersonalSign(account: String?, message: String = "Vaultly unlock request") {

        val params = JSONArray()
            .put(message)   // data to sign
            .put(account)   // wallet address
            .toString()

        val request = Request(
            method = "personal_sign",
            params = params
        )

        AppKit.request(
            request,
            onSuccess = { result: SentRequestResult ->
                println("✅ Sign request sent to wallet")
            },
            onError = { error: Throwable ->
                println("❌ Error sending sign request: ${error.localizedMessage}")
            }
        )
    }
}