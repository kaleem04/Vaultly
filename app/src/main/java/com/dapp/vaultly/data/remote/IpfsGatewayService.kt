package com.dapp.vaultly.data.remote

import com.dapp.vaultly.data.model.PinataContentResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface IpfsGatewayService {
    @GET("ipfs/{cid}")
    suspend fun getJsonFromIpfs(@Path("cid") cid: String): PinataContentResponse
}