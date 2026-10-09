package com.example.warehouselocationscanner.network

/**
 * Data model for the login API response.
 * Captures the authentication token returned by the server.
 */
data class LoginResponse(
    val token: String
)
