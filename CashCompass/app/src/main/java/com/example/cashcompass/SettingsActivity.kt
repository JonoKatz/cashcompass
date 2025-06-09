package com.example.cashcompass

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.cashcompass.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    private val currencies = listOf(
        "ZAR - South African Rand",
        "USD - US Dollar",
        "EUR - Euro",
        "GBP - British Pound",
        "JPY - Japanese Yen",
        "AUD - Australian Dollar",
        "CAD - Canadian Dollar",
        "INR - Indian Rupee",
        "CNY - Chinese Yuan",
        "CHF - Swiss Franc"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Handle full screen layout insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settingsLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        // Initialize UI components
        val currencyInput = findViewById<AutoCompleteTextView>(R.id.spinnerCurrency)
        val budgetInput = findViewById<EditText>(R.id.editTextMonthlyBudget)
        val minGoalInput = findViewById<EditText>(R.id.editTextMinGoal)
        val saveButton = findViewById<Button>(R.id.buttonSaveSettings)
        val logoutButton = findViewById<Button>(R.id.buttonLogout)
        val backButton = findViewById<Button>(R.id.buttonBack)
        val oldPasswordInput = findViewById<EditText>(R.id.editTextOldPassword)
        val newPasswordInput = findViewById<EditText>(R.id.editTextNewPassword)

        // Set up currency dropdown
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, currencies)
        currencyInput.setAdapter(adapter)

        // Populate saved values
        val savedCurrency = prefs.getString("currency", "R") ?: "R"
        val savedMinGoal = prefs.getFloat("minGoal", 0f)

        currencyInput.setText(savedCurrency)
        budgetInput.hint = "Enter amount ($savedCurrency)"
        minGoalInput.setText(if (savedMinGoal > 0f) savedMinGoal.toString() else "")

        // Change hint dynamically based on currency selection
        currencyInput.setOnItemClickListener { _, _, _, _ ->
            val selectedCurrency = currencyInput.text.toString().take(3)
            budgetInput.hint = "Enter amount ($selectedCurrency)"
        }

        // Save button: budget + currency + minGoal
        saveButton.setOnClickListener {
            val selectedCurrency = currencyInput.text.toString().take(3)

            val currentBudget = prefs.getFloat("budget", 0f)
            val currentMinGoal = prefs.getFloat("minGoal", 0f)

            val budget = budgetInput.text.toString().toFloatOrNull() ?: currentBudget
            val minGoal = minGoalInput.text.toString().toFloatOrNull() ?: currentMinGoal

            prefs.edit()
                .putString("currency", selectedCurrency)
                .putFloat("budget", budget)
                .putFloat("minGoal", minGoal)
                .apply()

            Toast.makeText(this, "Settings saved.", Toast.LENGTH_SHORT).show()
        }


        // Save Password Button dynamically added
        val savePasswordButton = Button(this).apply { text = "Save Password" }
        val scrollView = findViewById<ScrollView>(R.id.settingsLayout)
        val linearLayout = scrollView.getChildAt(0) as LinearLayout
        linearLayout.addView(savePasswordButton, linearLayout.childCount - 3)

        savePasswordButton.setOnClickListener {
            val oldPassword = oldPasswordInput.text.toString().trim()
            val newPassword = newPasswordInput.text.toString().trim()
            val username = prefs.getString("loggedInUser", null)

            if (username.isNullOrBlank()) {
                Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (oldPassword.isEmpty() || newPassword.isEmpty()) {
                Toast.makeText(this, "Please enter both current and new passwords.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (oldPassword == newPassword) {
                Toast.makeText(this, "New password must be different from current password.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check password and update if valid
            lifecycleScope.launch {
                val userDao = AppDatabase.getDatabase(applicationContext).userDao()
                val user = withContext(Dispatchers.IO) {
                    userDao.getUserByUsername(username)
                }

                if (user != null && user.password == oldPassword) {
                    val updatedUser = user.copy(password = newPassword)
                    withContext(Dispatchers.IO) {
                        userDao.updateUser(updatedUser)
                    }
                    runOnUiThread {
                        Toast.makeText(this@SettingsActivity, "Password updated successfully", Toast.LENGTH_SHORT).show()
                        oldPasswordInput.text.clear()
                        newPasswordInput.text.clear()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@SettingsActivity, "Incorrect current password", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        logoutButton.setOnClickListener {
            prefs.edit().remove("loggedInUser").apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        backButton.setOnClickListener {
            finish()
        }
    }
}
