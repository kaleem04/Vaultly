package com.dapp.vaultly.util

import com.reown.appkit.client.AppKit
import com.reown.appkit.client.models.request.Request
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

object SmartContractManager {

    private const val CONTRACT_ADDRESS = "0xYourContractAddress"
    private const val RPC_URL =
        "https://your-amoy-rpc-endpoint" // for polling / eth_getTransactionReceipt
    private const val AMOY_CHAIN_ID = 80002 // (Amoy testnet chain id you mentioned earlier)

    suspend fun saveCid(account: String, contractAddress: String, cid: String): String =
        suspendCancellableCoroutine { cont ->
            val functionSelector = "0x3fa4f245"
            val cidHex = "0x" + cid.toByteArray().joinToString("") { "%02x".format(it) }.padEnd(64, '0')
            val data = functionSelector + cidHex.removePrefix("0x")

            val txObject = mapOf(
                "from" to account,
                "to" to contractAddress,
                "data" to data,
                "value" to "0x0"
            )

            val params = "[${org.json.JSONObject(txObject).toString()}]"

            val request = Request(
                method = "eth_sendTransaction",
                params = params
            )

            AppKit.request(
                request,
                onSuccess = { txHash ->
                    cont.resume(txHash.toString()) {}
                },
                onError = { error ->
                    cont.resumeWithException(Exception(error.localizedMessage))
                }
            )
        }


    suspend fun getCid(account: String, contractAddress: String): String =
        suspendCancellableCoroutine { cont ->
            val functionSelector = "0x4e67d5a5"
            val addrHex = account.removePrefix("0x").padStart(64, '0')
            val data = functionSelector + addrHex

            val txObject = mapOf(
                "to" to contractAddress,
                "data" to data
            )

            val params = "[${org.json.JSONObject(txObject).toString()}, \"latest\"]"

            val request = Request(
                method = "eth_call",
                params = params
            )

            AppKit.request(
                request,
                onSuccess = { result ->
                    try {
                        val hexString = result.toString().removePrefix("0x")
                        val bytes = hexString.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                        val cid = String(bytes).trim { it <= ' ' || it == '\u0000' }
                        cont.resume(cid) {}
                    } catch (e: Exception) {
                        cont.resumeWithException(e)
                    }
                },
                onError = { error ->
                    cont.resumeWithException(Exception(error.localizedMessage))
                }
            )
        }

}