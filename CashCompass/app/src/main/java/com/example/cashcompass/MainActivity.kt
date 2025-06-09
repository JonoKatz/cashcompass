package com.example.cashcompass

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

// Entry point of the app where users choose to log in or register
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Enables layout to draw behind system bars
        setContentView(R.layout.activity_main)

        // Initialize buttons for navigation
        val registerButton: Button = findViewById(R.id.button)
        val loginButton: Button = findViewById(R.id.button2)

        // Navigate to registration page
        registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Navigate to login page
        loginButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // Adjust view padding to account for system UI (status bar, navigation bar, etc.)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
