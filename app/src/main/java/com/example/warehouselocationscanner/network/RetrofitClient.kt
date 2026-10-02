package com.example.warehouselocationscanner.network

import android.content.Context
import com.example.warehouselocationscanner.BuildConfig
import com.example.warehouselocationscanner.utils.SharedPrefsManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Retrofit Client singleton to provide the ApiService instance.
 * Uses BuildConfig.SERVER_IP to avoid hardcoding IP addresses.
 * Includes HttpLoggingInterceptor to debug raw network traffic.
 */
object RetrofitClient {
    private val BASE_URL = "http://${BuildConfig.SERVER_IP}:5000/"

    private var apiService: ApiService? = null

    /**
     * Initializes and returns the ApiService. Requires Context for the AuthInterceptor.
     */
    fun getApiService(context: Context): ApiService {
        if (apiService == null) {
            val sharedPrefsManager = SharedPrefsManager(context)

            // 1. Create the Logging Interceptor to see under the hood
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY // Logs URL, headers, and full JSON payload
            }

            // 2. Add OkHttp client with the AuthInterceptor AND Logging Interceptor
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor) // Must go before AuthInterceptor to see raw output
                .addInterceptor(AuthInterceptor(context.applicationContext, sharedPrefsManager))
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            apiService = retrofit.create(ApiService::class.java)
        }
        return apiService!!
    }
}