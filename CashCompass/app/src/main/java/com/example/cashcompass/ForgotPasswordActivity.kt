package com.example.cashcompass

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

// Activity to simulate the "Forgot Password" feature
class ForgotPasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        // UI references
        val emailEditText: EditText = findViewById(R.id.editTextEmail)
        val sendResetLinkButton: Button = findViewById(R.id.buttonSendResetLink)
        val backToLoginButton: Button = findViewById(R.id.buttonBackToLogin)

        // Handle reset link click
        sendResetLinkButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()

            // Hide keyboard for better UX
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)

            // Validate email input and show appropriate feedback
            when {
                email.isEmpty() -> {
                    Toast.makeText(this, "Please enter your email address.", Toast.LENGTH_SHORT).show()
                }
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // Simulate sending a reset link (real logic would require backend integration)
                    Toast.makeText(this, "Password reset link sent to $email!", Toast.LENGTH_LONG).show()
                    emailEditText.text.clear()
                }
            }
        }

        // Return to login screen
        backToLoginButton.setOnClickListener {
            finish()
        }
    }
}
