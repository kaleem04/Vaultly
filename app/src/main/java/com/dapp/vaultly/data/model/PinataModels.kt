package com.dapp.vaultly.data.model

import com.google.gson.annotations.SerializedName

data class PinataRequest(
    val pinataContent : PinataContent
)

data class PinataContent(
    val content : String
)

data class PinataResponse(
    @SerializedName("IpfsHash")
    val ipfsHash : String,
    @SerializedName("PinSize")
    val pinSize : Long,
    @SerializedName("TimeStamp")
    val timeStamp : String
)