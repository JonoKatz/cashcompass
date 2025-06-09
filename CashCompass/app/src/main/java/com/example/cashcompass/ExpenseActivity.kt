package com.example.cashcompass

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cashcompass.database.AppDatabase
import com.example.cashcompass.database.ExpenseEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import com.google.firebase.database.FirebaseDatabase
import android.util.Log


class ExpenseActivity : AppCompatActivity() {

    // UI components
    private lateinit var amountEditText: EditText
    private lateinit var dateEditText: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var descriptionEditText: EditText
    private lateinit var saveExpenseButton: Button
    private lateinit var cancelButton: Button
    private lateinit var attachReceiptButton: Button

    // Selected image URI for receipt
    private var selectedImageUri: Uri? = null
    private var currencyCode: String = "R"
    private val firebaseDatabase = FirebaseDatabase.getInstance()
    private val firebaseExpensesRef = firebaseDatabase.getReference("expenses")


    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense)

        // Initialize UI elements
        amountEditText = findViewById(R.id.editTextAmount)
        dateEditText = findViewById(R.id.editTextDate)
        categorySpinner = findViewById(R.id.spinnerCategory)
        descriptionEditText = findViewById(R.id.editTextDescription)
        saveExpenseButton = findViewById(R.id.buttonSaveExpense)
        cancelButton = findViewById(R.id.buttonCancel)
        attachReceiptButton = findViewById(R.id.buttonAttachReceipt)

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        currencyCode = prefs.getString("currency", "R") ?: "R"
        amountEditText.hint = "Amount ($currencyCode)"

        // Populate category spinner
        val categories = listOf("Groceries", "Transport", "Entertainment", "Utilities", "Other")
        categorySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)

        // Date picker logic
        dateEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val date = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                    dateEditText.setText(date)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        // Attach receipt button
        attachReceiptButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(Intent.createChooser(intent, "Select Receipt"), PICK_IMAGE_REQUEST)
        }

        // Save expense to Room database
        saveExpenseButton.setOnClickListener {
            val amountText = amountEditText.text.toString()
            val date = dateEditText.text.toString()
            val category = categorySpinner.selectedItem.toString()
            val description = descriptionEditText.text.toString().ifBlank { "" }

            if (amountText.isBlank() || date.isBlank() || category.isBlank()) {
                Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "Enter a valid amount.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val username = prefs.getString("loggedInUser", "defaultUser") ?: "defaultUser"

            // Save to local Room DB
            val expense = ExpenseEntity(
                userId = username,
                amount = amount,
                date = date,
                category = category,
                description = description,
                imageUri = selectedImageUri?.toString()
            )

            // Generate Firebase-compatible object
            val firebaseId = firebaseExpensesRef.push().key ?: UUID.randomUUID().toString()
            val firebaseExpense = mapOf(
                "id" to firebaseId,
                "userId" to username,
                "amount" to amount,
                "date" to date,
                "category" to category,
                "description" to description,
                "imageUri" to selectedImageUri?.toString()
            )

            // Save to Firebase
            firebaseExpensesRef.child(firebaseId).setValue(firebaseExpense)
                .addOnSuccessListener {
                    Log.d("Firebase", "Expense saved to Firebase.")
                }
                .addOnFailureListener {
                    Log.e("Firebase", "Failed to save to Firebase", it)
                }

            // Save to Room DB
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(applicationContext).expenseDao().insertExpense(expense)
                }
                Toast.makeText(this@ExpenseActivity, "Expense saved!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }


        // Cancel button closes the activity
        cancelButton.setOnClickListener {
            finish()
        }
    }

    // Handle image selection from gallery
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data?.data != null) {
            selectedImageUri = data.data
            Toast.makeText(this, "Receipt attached!", Toast.LENGTH_SHORT).show()
        }
    }
}
