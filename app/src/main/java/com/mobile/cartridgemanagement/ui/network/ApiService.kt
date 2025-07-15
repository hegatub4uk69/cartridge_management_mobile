package com.mobile.cartridgemanagement.ui.network

import com.mobile.cartridgemanagement.ui.network.requests.LoginRequest
import com.mobile.cartridgemanagement.ui.network.responses.CartridgeModels
import com.mobile.cartridgemanagement.ui.network.responses.LoginResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @POST("authorize")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("logout-user")
    suspend fun logout()

    @POST("get-cartridge-models")
    suspend fun getCartridgeModels(): CartridgeModels

//    @GET("api/user/")
//    suspend fun getUserProfile(): UserProfileResponse
//
//    @POST("api/logout/")
//    suspend fun logout(): EmptyResponse
}