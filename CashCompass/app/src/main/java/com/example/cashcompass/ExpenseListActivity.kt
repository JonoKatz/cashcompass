package com.example.cashcompass

import ExpenseFirebase
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cashcompass.database.AppDatabase
import com.example.cashcompass.database.ExpenseEntity
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class ExpenseListActivity : AppCompatActivity() {

    private lateinit var dateFilter: EditText
    private lateinit var categoryFilter: Spinner
    private lateinit var listViewExpenses: ListView
    private lateinit var clearFilterButton: Button
    private lateinit var backButton: Button
    private lateinit var deleteButton: Button
    private lateinit var viewReceiptButton: Button
    private lateinit var totalAmountTextView: TextView

    private var selectedExpense: ExpenseEntity? = null
    private var allExpenses: List<ExpenseEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_list)

        // UI components
        dateFilter = findViewById(R.id.editTextDateFilter)
        categoryFilter = findViewById(R.id.spinnerCategoryFilter)
        listViewExpenses = findViewById(R.id.listViewExpenses)
        deleteButton = findViewById(R.id.buttonDeleteExpense)
        viewReceiptButton = findViewById(R.id.buttonViewReceipt)
        clearFilterButton = findViewById(R.id.buttonClearFilters)
        backButton = findViewById(R.id.buttonBackToDashboard)
        totalAmountTextView = findViewById(R.id.textViewTotalAmount)

        // Populate category spinner
        val categories = listOf("All", "Groceries", "Transport", "Entertainment", "Utilities", "Other")
        categoryFilter.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)

        // Open date picker for filtering
        dateFilter.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val date = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                    dateFilter.setText(date)
                    loadExpenses()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        // Trigger category filter
        categoryFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                loadExpenses()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Set selected expense from list
        listViewExpenses.setOnItemClickListener { _, _, position, _ ->
            selectedExpense = allExpenses[position]
        }

        // View attached receipt
        viewReceiptButton.setOnClickListener {
            selectedExpense?.let { expense ->
                if (expense.imageUri.isNullOrBlank()) {
                    Toast.makeText(this, "No receipt attached to this expense.", Toast.LENGTH_SHORT).show()
                } else {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.parse(expense.imageUri), "image/*")
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    startActivity(intent)
                }
            } ?: Toast.makeText(this, "Please select an expense to view receipt.", Toast.LENGTH_SHORT).show()
        }

        // DELETE from Room and Firebase
        deleteButton.setOnClickListener {
            selectedExpense?.let { expense ->
                lifecycleScope.launch {
                    // Delete from local Room database
                    withContext(Dispatchers.IO) {
                        AppDatabase.getDatabase(applicationContext).expenseDao().deleteExpense(expense)
                    }

                    // Delete from Firebase
                    val firebaseRef = FirebaseDatabase.getInstance().getReference("expenses")
                    firebaseRef.orderByChild("description").equalTo(expense.description)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                for (child in snapshot.children) {
                                    val fbExpense = child.getValue(ExpenseFirebase::class.java)
                                    if (fbExpense != null &&
                                        fbExpense.amount == expense.amount &&
                                        fbExpense.category == expense.category &&
                                        fbExpense.date == expense.date
                                    ) {
                                        child.ref.removeValue()
                                        Log.d("FirebaseDelete", "Deleted from Firebase: ${fbExpense.description}")
                                        break
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("FirebaseDelete", "Failed to delete from Firebase", error.toException())
                            }
                        })

                    Toast.makeText(this@ExpenseListActivity, "Expense deleted.", Toast.LENGTH_SHORT).show()
                    selectedExpense = null
                    loadExpenses()
                }
            } ?: Toast.makeText(this, "Please select an expense to delete.", Toast.LENGTH_SHORT).show()
        }

        // Clear filters
        clearFilterButton.setOnClickListener {
            dateFilter.text.clear()
            categoryFilter.setSelection(0)
            loadExpenses()
        }

        // Back to dashboard
        backButton.setOnClickListener {
            finish()
        }

        // Initial expense load
        loadExpenses()
    }

    private fun loadExpenses() {
        val selectedDate = dateFilter.text.toString()
        val selectedCategory = categoryFilter.selectedItem.toString()

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val username = prefs.getString("loggedInUser", "defaultUser") ?: "defaultUser"

        lifecycleScope.launch {
            allExpenses = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(applicationContext).expenseDao().getExpensesForUser(username)
            }.filter {
                (selectedDate.isBlank() || it.date == selectedDate) &&
                        (selectedCategory == "All" || it.category == selectedCategory)
            }

            val currencyCode = prefs.getString("currency", "R") ?: "R"
            val total = allExpenses.sumOf { it.amount }
            totalAmountTextView.text = "Total: $currencyCode${"%.2f".format(total)}"

            val displayList = allExpenses.map {
                "$currencyCode${"%.2f".format(it.amount)} | ${it.date} | ${it.category}\n${it.description}"
            }

            val adapter = ArrayAdapter(this@ExpenseListActivity, android.R.layout.simple_list_item_1, displayList)
            listViewExpenses.adapter = adapter
        }
    }
}
