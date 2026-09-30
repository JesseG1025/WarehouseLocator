package com.example.warehouselocationscanner.network

import android.content.Context
import com.example.warehouselocationscanner.BuildConfig
import com.example.warehouselocationscanner.utils.SharedPrefsManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Retrofit Client singleton to provide the ApiService instance.
 * Uses BuildConfig.SERVER_IP to avoid hardcoding IP addresses per security guidelines.
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
            
            // Add OkHttp client with the AuthInterceptor
            val okHttpClient = OkHttpClient.Builder()
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
