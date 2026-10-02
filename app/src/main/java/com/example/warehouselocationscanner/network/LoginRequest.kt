package com.example.warehouselocationscanner.network

/**
 * Data model for the login API request.
 * Encapsulates the credentials needed to authenticate.
 */
data class LoginRequest(
    val username: String,
    val password: String
)
