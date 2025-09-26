package com.dapp.vaultly.data.remote

import com.dapp.vaultly.data.model.PinataContentResponse
import com.dapp.vaultly.data.model.PinataRequest
import com.dapp.vaultly.data.model.PinataResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PinataApiService {
    @POST("pinning/pinJSONToIPFS")
    suspend fun pinJsonToIpfs(@Body body: PinataRequest): PinataResponse


    @DELETE("pinning/unpin/{cid}")
    suspend fun unpin(@Path("cid") cid: String): Response<Unit>
}