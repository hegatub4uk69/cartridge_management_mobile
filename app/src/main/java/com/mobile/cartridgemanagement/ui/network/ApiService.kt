package com.mobile.cartridgemanagement.ui.network

import com.mobile.cartridgemanagement.ui.network.requests.LoginRequest
import com.mobile.cartridgemanagement.ui.network.responses.CartridgeModels
import com.mobile.cartridgemanagement.ui.network.responses.CartridgeResponse
import com.mobile.cartridgemanagement.ui.network.responses.LoginResponse
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("authorize")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("logout-user")
    suspend fun logout()

    @POST("get-cartridge-models")
    suspend fun getCartridgeModels(): CartridgeModels

    @POST("add-new-cartridge")
    suspend fun addCartridge(@Body request: RequestBody): CartridgeResponse

}