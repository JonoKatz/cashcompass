package com.example.cashcompass.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// This data class defines the structure of the 'expenses' table in the Room database
@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,   // Auto-generated unique ID for each expense
    val userId: String,                                 // ID or username of the user who created the expense
    val amount: Double,                                 // Amount spent
    val date: String,                                   // Date of the expense
    val category: String,                               // Expense category (e.g., Groceries, Transport)
    val description: String,                            // Optional description of the expense
    val imageUri: String? = null                        // Optional receipt image URI
)
