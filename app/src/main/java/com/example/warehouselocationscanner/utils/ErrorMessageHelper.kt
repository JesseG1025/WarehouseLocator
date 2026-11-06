package com.example.warehouselocationscanner.utils

import com.google.gson.JsonParser
import retrofit2.Response
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Utility to extract user-friendly, descriptive messages from network responses and exceptions.
 * Replaces generic HTTP error codes (e.g. 401, 500) and raw stack trace messages with actionable language.
 */
object ErrorMessageHelper {

    /**
     * Extracts a friendly error message from a Retrofit Response error body or status code.
     * Looks for server-provided JSON error fields first ("message", "error", "detail"),
     * then falls back to human-readable explanations based on the HTTP status code.
     */
    fun getFriendlyApiErrorMessage(response: Response<*>?, isLoginRequest: Boolean = false): String {
        if (response == null) return "An unknown error occurred. Please try again."

        // 1. Try to inspect any JSON payload returned by the server in errorBody
        val errorBodyString = try {
            response.errorBody()?.string()
        } catch (_: Exception) {
            null
        }

        if (!errorBodyString.isNullOrBlank()) {
            val serverMessage = parseJsonErrorMessage(errorBodyString)
            if (!serverMessage.isNullOrBlank()) {
                return serverMessage
            }
        }

        // 2. Map standard HTTP status codes to friendly, actionable messages
        return when (response.code()) {
            400 -> "Invalid request. Please check your input and try again."
            401, 403 -> {
                if (isLoginRequest) {
                    "Incorrect username or password. Please try again."
                } else {
                    "Session expired or unauthorized. Please log in again."
                }
            }
            404 -> "The requested item or service could not be found."
            408 -> "The server took too long to respond. Please try again."
            429 -> "Too many attempts. Please wait a moment and try again."
            in 500..599 -> "The server encountered a problem. Please try again later or contact your supervisor."
            else -> "Server error (${response.code()}). Please try again."
        }
    }

    /**
     * Maps throwables (connection timeouts, offline state, etc.) to friendly strings.
     */
    fun getFriendlyNetworkErrorMessage(throwable: Throwable?): String {
        return when (throwable) {
            is UnknownHostException, is ConnectException ->
                "Cannot connect to the server. Please check your Wi-Fi or network connection."
            is SocketTimeoutException ->
                "Connection timed out. The server is taking too long to respond."
            else ->
                "Network connection issue. Please verify Wi-Fi and try again."
        }
    }

    /**
     * Attempts to parse common error JSON keys like {"error": "..."}, {"message": "..."}, etc.
     */
    private fun parseJsonErrorMessage(jsonString: String): String? {
        return try {
            val jsonElement = JsonParser.parseString(jsonString)
            if (jsonElement.isJsonObject) {
                val obj = jsonElement.asJsonObject
                when {
                    obj.has("message") && !obj.get("message").isJsonNull -> obj.get("message").asString
                    obj.has("error") && !obj.get("error").isJsonNull -> obj.get("error").asString
                    obj.has("detail") && !obj.get("detail").isJsonNull -> obj.get("detail").asString
                    else -> null
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
