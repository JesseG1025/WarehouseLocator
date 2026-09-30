package com.example.warehouselocationscanner.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit API Service defining the network endpoints.
 * Includes the /api/login endpoint as requested.
 */
interface ApiService {
    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}
