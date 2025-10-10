package com.dapp.vaultly.data.model

import android.R
import com.google.gson.annotations.SerializedName
import com.reown.appkit.client.AppKit

data class PinataRequest(
    val pinataContent : Any,
    val pinataMetadata: PinataMetadata? =null
)

data class PinataMetadata(
    val name: String = "",
    val keyValues: Map<String, String>? = null
)
data class PinataContentResponse(
    val content: String
)

data class IpfsResponse(
    val wallet : String = AppKit.getAccount()?.address ?: "",
    val vault : VaultData
)

data class VaultData(
    val content: String
)
data class VaultlyContent(
    val content : String
)
// What you fetch from IPFS
data class PinataResponse(
    @SerializedName("IpfsHash")
    val ipfsHash : String,
    @SerializedName("PinSize")
    val pinSize : Long,
    @SerializedName("TimeStamp")
    val timeStamp : String
)