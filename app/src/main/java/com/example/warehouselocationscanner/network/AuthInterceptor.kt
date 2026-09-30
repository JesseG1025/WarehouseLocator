package com.example.warehouselocationscanner.network

import android.content.Context
import android.content.Intent
import com.example.warehouselocationscanner.ui.scanner.LoginActivity
import com.example.warehouselocationscanner.utils.SharedPrefsManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor to attach the auth token to headers and handle 401 Unauthorized responses.
 */
class AuthInterceptor(
    private val context: Context,
    private val sharedPrefsManager: SharedPrefsManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Retrieve the token
        val token = sharedPrefsManager.getToken()

        // Attach the token if it exists
        val requestBuilder = originalRequest.newBuilder()
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val request = requestBuilder.build()
        val response = chain.proceed(request)

        // Handle 401 Unauthorized
        if (response.code == 401) {
            sharedPrefsManager.clearToken()
            
            // Redirect to LoginActivity
            val intent = Intent(context, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        }

        return response
    }
}
