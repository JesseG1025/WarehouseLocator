package com.example.warehouselocationscanner.ui.scanner

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.warehouselocationscanner.R

class RecentScansActivity : AppCompatActivity() {

    private lateinit var lvRecentScans: ListView
    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recent_scans)

        lvRecentScans = findViewById(R.id.lvRecentScans)
        btnBack = findViewById(R.id.btnBack)

        // Retrieve the recent scans from the intent
        val recentScans = intent.getStringArrayListExtra("RECENT_SCANS") ?: arrayListOf()

        // Set up the ListView with the recent scans
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, recentScans)
        lvRecentScans.adapter = adapter

        // Finish activity on back button press
        btnBack.setOnClickListener {
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.recent_scans_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
