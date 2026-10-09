package com.example.warehouselocationscanner.ui.scanner

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.warehouselocationscanner.R
import com.google.android.material.textfield.TextInputEditText
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etWorkerId = findViewById<TextInputEditText>(R.id.etWorkerId)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        // Restrict input to 20 characters maximum
        etWorkerId.filters = arrayOf(InputFilter.LengthFilter(20))

        btnLogin.setOnClickListener {
            val rawInput = etWorkerId.text.toString().trim()
            val sanitizedName = rawInput.replace(Regex("[^a-zA-Z0-9_-]"), "")
            val passwordInput = etPassword.text.toString()

            if (sanitizedName.isNotEmpty() && passwordInput.isNotEmpty()) {
                if (sanitizedName != rawInput) {
                    Toast.makeText(this, "Special characters were removed", Toast.LENGTH_SHORT).show()
                }

                // Call the /api/login endpoint in a coroutine
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val request = com.example.warehouselocationscanner.network.LoginRequest(
                            username = sanitizedName,
                            password = passwordInput
                        )
                        val apiService = com.example.warehouselocationscanner.network.RetrofitClient.getApiService(this@LoginActivity)

                        // Dynamically route this specific request to the main Flask server
                        val authUrl = "http://192.168.1.205:5000/api/login"
                        val response = apiService.login(authUrl, request)

                        if (response.isSuccessful && response.body() != null) {
                            val token = response.body()!!.token

                            // Save the token using SharedPrefsManager
                            val sharedPrefsManager = com.example.warehouselocationscanner.utils.SharedPrefsManager(this@LoginActivity)
                            sharedPrefsManager.saveToken(token)

                            withContext(Dispatchers.Main) {
                                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                intent.putExtra("USER_SIGNATURE", sanitizedName)
                                startActivity(intent)
                                finish()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@LoginActivity, "Login failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@LoginActivity, "Network Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Please enter both ID and Password", Toast.LENGTH_SHORT).show()
            }
        }
    }
}