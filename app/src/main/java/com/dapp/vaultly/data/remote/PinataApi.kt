package com.dapp.vaultly.data.remote

import com.dapp.vaultly.data.model.PinataFetchResponse
import com.dapp.vaultly.data.model.PinataRequest
import com.dapp.vaultly.data.model.PinataResponse
import com.dapp.vaultly.data.model.VaultlyContent
import com.dapp.vaultly.util.Constants
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface PinataApi {
    @POST("pinning/pinJSONToIPFS")
    suspend fun pinJsonToIpfs(@Body body: PinataRequest): PinataResponse

    @GET("ipfs/{cid}")
    suspend fun getFromIpfs(@Path("cid") cid: String): PinataFetchResponse

    @DELETE("pinning/unpin/{cid}")
    suspend fun unpin(@Path("cid") cid: String): Response<Unit>
}