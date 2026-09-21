package com.example.warehouselocationscanner.ui.scanner

import android.content.Intent
import android.os.Bundle
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

        btnLogin.setOnClickListener {
            val workerName = etWorkerId.text.toString().trim()

            if (workerName.isNotEmpty()) {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("USER_SIGNATURE", workerName)
                startActivity(intent)
                finish() // Prevents the back button from returning to the login screen
            } else {
                Toast.makeText(this, "Please enter a valid Username or ID", Toast.LENGTH_SHORT).show()
            }
        }
    }
}