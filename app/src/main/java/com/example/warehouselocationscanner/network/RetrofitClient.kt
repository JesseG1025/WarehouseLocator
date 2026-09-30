package com.example.warehouselocationscanner.network

import com.example.warehouselocationscanner.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Retrofit Client singleton to provide the ApiService instance.
 * Uses BuildConfig.SERVER_IP to avoid hardcoding IP addresses per security guidelines.
 */
object RetrofitClient {
    // Dynamically construct the base URL using BuildConfig.SERVER_IP
    private val BASE_URL = "http://${BuildConfig.SERVER_IP}:5000/"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
