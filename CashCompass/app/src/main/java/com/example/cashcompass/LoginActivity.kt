package com.example.cashcompass

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cashcompass.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Activity to handle user login
class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // UI references
        val emailInput = findViewById<EditText>(R.id.editTextEmail)
        val passwordInput = findViewById<EditText>(R.id.editTextPassword)
        val loginButton = findViewById<Button>(R.id.buttonLogin)
        val registerLink = findViewById<TextView>(R.id.textViewRegister)
        val forgotPasswordLink = findViewById<TextView>(R.id.textViewForgotPassword)

        // Login button click logic
        loginButton.setOnClickListener {
            val username = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            // Basic validation
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Perform user authentication using Room database
            lifecycleScope.launch {
                val userDao = AppDatabase.getDatabase(applicationContext).userDao()
                val user = withContext(Dispatchers.IO) {
                    userDao.getUserByUsername(username)
                }

                // Check credentials and proceed
                if (user != null && user.password == password) {
                    // Save logged-in user to shared preferences
                    val prefs = getSharedPreferences("settings", MODE_PRIVATE)
                    prefs.edit().putString("loggedInUser", username).apply()

                    // Redirect to dashboard
                    startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                    finish()
                } else {
                    // Show login error
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Incorrect username or password", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Navigate to register screen
        registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Navigate to forgot password screen
        forgotPasswordLink.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }
}
