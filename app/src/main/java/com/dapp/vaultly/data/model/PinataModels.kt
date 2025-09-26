package com.dapp.vaultly.data.model

import android.R
import com.google.gson.annotations.SerializedName

data class PinataRequest(
    val pinataContent : VaultlyContent,
)

data class PinataContentResponse(
    val content: String
)

data class OuterResponse(
    val content: String // Treat it as a string first
)

data class InnerContent(
    val iv: String,
    val cipher: String
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