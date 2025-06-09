package com.example.cashcompass

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cashcompass.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Activity that handles displaying achievement badges based on user behavior
class AchievementsActivity : AppCompatActivity() {

    // Badge and label UI components
    private lateinit var badgeFirstExpense: ImageView
    private lateinit var badgeSavedMoney: ImageView
    private lateinit var badgeBudgetMaster: ImageView

    private lateinit var textFirstExpense: TextView
    private lateinit var textSavedMoney: TextView
    private lateinit var textBudgetMaster: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievements)

        // Bind views
        badgeFirstExpense = findViewById(R.id.badgeFirstExpense)
        badgeSavedMoney = findViewById(R.id.badgeSavedMoney)
        badgeBudgetMaster = findViewById(R.id.badgeBudgetMaster)

        textFirstExpense = findViewById(R.id.textViewFirstExpense)
        textSavedMoney = findViewById(R.id.textViewSavedMoney)
        textBudgetMaster = findViewById(R.id.textViewBudgetMaster)

        // Back button to return to previous screen
        val backButton: Button = findViewById(R.id.buttonBack)
        backButton.setOnClickListener {
            finish()
        }

        // Evaluate and update which achievements are unlocked
        checkAchievements()
    }

    // Determines which achievements should be displayed as unlocked based on user data
    private fun checkAchievements() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val username = prefs.getString("loggedInUser", "defaultUser") ?: "defaultUser"
        val budget = prefs.getFloat("budget", 0f)

        lifecycleScope.launch {
            // Fetch user's expenses from the database
            val expenses = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(applicationContext).expenseDao().getExpensesForUser(username)
            }

            val totalSpent = expenses.sumOf { it.amount }

            // Achievement: First Expense Logged
            val hasLoggedExpense = expenses.isNotEmpty()
            badgeFirstExpense.alpha = if (hasLoggedExpense) 1f else 0.3f
            textFirstExpense.alpha = if (hasLoggedExpense) 1f else 0.3f

            // Achievement: Saved Money (spent less than 80% of budget)
            val savedMoney = budget > 0 && totalSpent < (budget * 0.8)
            badgeSavedMoney.alpha = if (savedMoney) 1f else 0.3f
            textSavedMoney.alpha = if (savedMoney) 1f else 0.3f

            // Achievement: Budget Master (spent <= budget)
            val underBudget = budget > 0 && totalSpent <= budget
            badgeBudgetMaster.alpha = if (underBudget) 1f else 0.3f
            textBudgetMaster.alpha = if (underBudget) 1f else 0.3f
        }
    }
}
