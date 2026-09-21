package com.example.warehouselocationscanner.ui.scanner

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.warehouselocationscanner.R
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etWorkerId = findViewById<TextInputEditText>(R.id.etWorkerId)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        // Restrict input to 20 characters maximum
        etWorkerId.filters = arrayOf(InputFilter.LengthFilter(20))

        btnLogin.setOnClickListener {
            val rawInput = etWorkerId.text.toString().trim()

            // Regex to allow only letters, numbers, hyphens, and underscores
            val sanitizedName = rawInput.replace(Regex("[^a-zA-Z0-9_-]"), "")

            if (sanitizedName.isNotEmpty()) {
                if (sanitizedName != rawInput) {
                    Toast.makeText(this, "Special characters were removed", Toast.LENGTH_SHORT).show()
                }

                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("USER_SIGNATURE", sanitizedName)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Please enter a valid alphanumeric ID", Toast.LENGTH_SHORT).show()
            }
        }
    }
}