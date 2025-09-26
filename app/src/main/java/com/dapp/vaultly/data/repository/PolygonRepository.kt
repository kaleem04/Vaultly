package com.dapp.vaultly.data.repository

import android.util.Log
import com.dapp.vaultly.data.model.PolygonResponse
import com.dapp.vaultly.data.remote.VaultlyApi
import com.dapp.vaultly.util.Constants.API_KEY
import com.dapp.vaultly.util.Constants.CONTRACT_ADDRESS
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.Utf8String

class PolygonRepository(
    private val vaultlyApi: VaultlyApi
) {
    companion object {
        private const val TAG = "PolygonRepository"
    }

    /**
     * Encode the getCID(address) function call.
     */
    private fun encodeGetCid(userWalletAddress: String): String {
        val function = Function(
            "getCID",
            listOf(Address(userWalletAddress)),
            listOf(TypeReference.create(Utf8String::class.java))
        )
        return FunctionEncoder.encode(function)
    }

    /**
     * Fetch CID from Polygon via PolygonScan API (eth_call).
     */
    suspend fun getCid(userWalletAddress: String): String {
            var cid  = ""
         try {
            val data = encodeGetCid(userWalletAddress)

            // Call polygonscan (eth_call)
            val response: PolygonResponse = vaultlyApi.getCidFromPolygon(
                to = CONTRACT_ADDRESS,
                data = data,
                apiKey = API_KEY
            )
            val utf8Ref: TypeReference<Utf8String> = TypeReference.create(Utf8String::class.java)
            // Decode CID from hex
            val decoded = FunctionReturnDecoder.decode(
                response.result,
                listOf(object : TypeReference<Utf8String>() {}) as List<TypeReference<Type<*>>>

            )
            cid = decoded.firstOrNull()?.value as? String ?: ""


        } catch (e: Exception) {
            Log.e(TAG, "getCid error: ${e.localizedMessage}", e)

        }
        return cid
    }
}
