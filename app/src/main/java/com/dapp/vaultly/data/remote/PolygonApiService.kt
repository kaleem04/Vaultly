package com.dapp.vaultly.data.remote

import com.dapp.vaultly.data.model.PolygonResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface PolygonApiService {
    @GET("api")
    suspend fun getCidFromPolygon(
        @Query("chainid") chainId: String = "80002", // Polygon Amoy
        @Query("module") module: String = "proxy",
        @Query("action") action: String = "eth_call",
        @Query("to") to: String,
        @Query("data") data: String,
        @Query("tag") tag: String = "latest",
        @Query("apikey") apiKey: String // put your key here
    ): PolygonResponse

}