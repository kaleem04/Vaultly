package com.dapp.vaultly.data.remote

import com.dapp.vaultly.data.model.PinataRequest
import com.dapp.vaultly.data.model.PinataResponse
import com.dapp.vaultly.util.Constants
import retrofit2.http.Body
import retrofit2.http.POST

interface PinataApi {

    @POST(Constants.PINATA_ENDPOINT)
    suspend fun uploadJson(
        @Body body: PinataRequest
    ): PinataResponse
}