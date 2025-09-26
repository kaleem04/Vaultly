package com.dapp.vaultly.data.model

import com.google.gson.annotations.SerializedName

data class PolygonResponse(
    @SerializedName("jsonrpc")
    val jsonRpc: String = "",
    @SerializedName("id")
    val id: Int = 0,
    @SerializedName("result")
    val result: String = ""
)
