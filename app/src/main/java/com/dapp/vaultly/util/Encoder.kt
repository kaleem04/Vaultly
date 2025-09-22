package com.dapp.vaultly.util
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Utf8String
import org.web3j.abi.TypeReference
object Encoder {

    // Encode setCID(string)
    fun encodeSetCID(cid: String): String {
        val function = Function(
            "setCID",
            listOf(Utf8String(cid)),
            emptyList()
        )
        return FunctionEncoder.encode(function)
    }

    // Encode getCID(address)
    fun encodeGetCID(address: String): String {
        val function = Function(
            "getCID",
            listOf(Address(address)),
            listOf(object : TypeReference<Utf8String>() {})
        )
        return FunctionEncoder.encode(function)
    }

}