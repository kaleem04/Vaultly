package com.dapp.vaultly.data.remote

import com.dapp.vaultly.data.model.PinataFetchResponse
import com.dapp.vaultly.data.model.PinataRequest
import com.dapp.vaultly.data.model.PinataResponse
import com.dapp.vaultly.data.model.PolygonResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface VaultlyApi {
    @POST("pinning/pinJSONToIPFS")
    suspend fun pinJsonToIpfs(@Body body: PinataRequest): PinataResponse

    @GET("ipfs/{cid}")
    suspend fun getFromIpfs(@Path("cid") cid: String): PinataFetchResponse

    @GET("api")
    suspend fun getCidFromPolygon(
        @Query("chainid")
        chainId: String = "80002", // Polygon Amoy
        @Query("module") module: String = "proxy",
        @Query("action") action: String = "eth_call",
        @Query("to") to: String,
        @Query("data") data: String,
        @Query("tag") tag: String = "latest",
        @Query("apikey") apiKey: String // put your key here
    ): PolygonResponse

    @DELETE("pinning/unpin/{cid}")
    suspend fun unpin(@Path("cid") cid: String): Response<Unit>
}