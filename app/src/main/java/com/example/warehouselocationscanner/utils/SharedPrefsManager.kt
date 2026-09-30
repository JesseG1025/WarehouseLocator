package com.example.warehouselocationscanner.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Utility class to handle saving and retrieving data from SharedPreferences.
 * Used for securely storing the authentication token after a successful login.
 */
class SharedPrefsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "scanner_prefs"
        private const val KEY_TOKEN = "auth_token"
    }

    /**
     * Saves the authentication token.
     */
    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    /**
     * Retrieves the authentication token. Returns null if not found.
     */
    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    /**
     * Clears the authentication token (e.g., on logout).
     */
    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }
}
