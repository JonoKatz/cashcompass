package com.example.cashcompass

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cashcompass.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// This activity serves as the main dashboard screen displayed after login.
// It shows the user's budget, recent expenses, spending progress, and navigation buttons.
class DashboardActivity : AppCompatActivity() {

    // UI components
    private lateinit var budgetTextView: TextView
    private lateinit var balanceTextView: TextView
    private lateinit var minGoalTextView: TextView
    private lateinit var listViewTransactions: ListView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize UI elements by ID
        budgetTextView = findViewById(R.id.textViewBudget)
        balanceTextView = findViewById(R.id.textViewBalance)
        minGoalTextView = findViewById(R.id.textViewMinGoal)
        listViewTransactions = findViewById(R.id.listViewTransactions)
        progressBar = findViewById(R.id.progressBarSavings)

        // Set up navigation buttons to other activities
        findViewById<Button>(R.id.buttonAddExpense).setOnClickListener {
            startActivity(Intent(this, ExpenseActivity::class.java))
        }
        findViewById<Button>(R.id.buttonExpenses).setOnClickListener {
            startActivity(Intent(this, ExpenseListActivity::class.java))
        }
        findViewById<Button>(R.id.buttonReports).setOnClickListener {
            startActivity(Intent(this, ReportsActivity::class.java))
        }
        findViewById<Button>(R.id.buttonSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<Button>(R.id.buttonAchievements).setOnClickListener {
            startActivity(Intent(this, AchievementsActivity::class.java))
        }
    }

    // Called when the activity comes into the foreground (e.g. returning from Settings)
    override fun onResume() {
        super.onResume()

        // Load shared preferences for current user
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val currencyCode = prefs.getString("currency", "R") ?: "R"
        val budget = prefs.getFloat("budget", 0f)
        val minGoal = prefs.getFloat("minGoal", 0f)
        val username = prefs.getString("loggedInUser", "defaultUser") ?: "defaultUser"

        // Load user's expense data from database and update UI accordingly
        lifecycleScope.launch {
            val expenses = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(applicationContext).expenseDao().getExpensesForUser(username)
            }.sortedByDescending { it.date } // Sort expenses by most recent

            val totalSpent = expenses.sumOf { it.amount } // Calculate total spent
            val remaining = budget - totalSpent.toFloat() // Calculate balance

            // Display budget, balance, and minimum spend goal
            budgetTextView.text = "Budget: $currencyCode${"%.2f".format(budget)}"
            balanceTextView.text = "Remaining: $currencyCode${"%.2f".format(remaining)}"
            minGoalTextView.text = "Minimum Spend Goal: $currencyCode${"%.2f".format(minGoal)}"

            // Notify user if they haven't reached the minimum spending goal
            if (minGoal > 0f && totalSpent < minGoal) {
                Toast.makeText(
                    this@DashboardActivity,
                    "You haven't reached your minimum spending goal of $currencyCode${"%.2f".format(minGoal)}",
                    Toast.LENGTH_LONG
                ).show()
            }

            // Update progress bar with percentage of budget remaining
            if (budget > 0f) {
                val percentRemaining = ((budget - totalSpent.toFloat()) / budget * 100).coerceIn(0f, 100f)
                progressBar.progress = percentRemaining.toInt()
            } else {
                progressBar.progress = 0
            }

            // Format and display list of recent transactions
            val displayList = expenses.map {
                "$currencyCode${"%.2f".format(it.amount)} | ${it.date} | ${it.category}\n${it.description}"
            }

            val adapter = ArrayAdapter(this@DashboardActivity, android.R.layout.simple_list_item_1, displayList)
            listViewTransactions.adapter = adapter
        }
    }
}
