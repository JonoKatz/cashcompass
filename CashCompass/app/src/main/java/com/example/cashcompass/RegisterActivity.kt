package com.example.cashcompass

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cashcompass.database.AppDatabase
import com.example.cashcompass.database.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// This activity allows new users to register an account
class RegisterActivity : AppCompatActivity() {

    // UI elements
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var loginLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize views
        usernameEditText = findViewById(R.id.editTextUsername)
        passwordEditText = findViewById(R.id.editTextPassword)
        confirmPasswordEditText = findViewById(R.id.editTextConfirmPassword)
        registerButton = findViewById(R.id.buttonRegister)
        loginLink = findViewById(R.id.textViewLogin)

        // Handle registration logic
        registerButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            // Basic validation checks
            if (username.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Register the user using Room database
            lifecycleScope.launch {
                val userDao = AppDatabase.getDatabase(applicationContext).userDao()
                val existingUser = withContext(Dispatchers.IO) {
                    userDao.getUserByUsername(username)
                }

                if (existingUser != null) {
                    Toast.makeText(this@RegisterActivity, "Username already taken", Toast.LENGTH_SHORT).show()
                } else {
                    val newUser = UserEntity(username = username, password = password)

                    // Insert new user into database
                    withContext(Dispatchers.IO) {
                        userDao.insertUser(newUser)
                    }

                    // Save default budget and currency settings for new user
                    val prefs = getSharedPreferences("settings", MODE_PRIVATE)
                    prefs.edit()
                        .putFloat("budget_$username", 0f)
                        .putString("currency_$username", "R")
                        .apply()

                    Toast.makeText(this@RegisterActivity, "Registration Successful!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                    finish()
                }
            }
        }

        // Navigate to login screen if user already has an account
        loginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
