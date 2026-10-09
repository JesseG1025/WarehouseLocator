package com.example.warehouselocationscanner.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Retrofit API Service defining the network endpoints.
 * Includes the /api/login endpoint as requested.
 */
interface ApiService {
    @POST
    suspend fun login(@Url url: String, @Body request: LoginRequest): Response<LoginResponse>
}
