package com.example.cashcompass.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// Defines the structure of an expense record in the database
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,     // Unique ID for each expense
    val username: String,                                 // Username associated with the expense
    val amount: Double,                                   // Expense amount
    val date: String,                                     // Date of the expense
    val category: String,                                 // Category (e.g., Groceries, Transport)
    val description: String,                              // Optional description of the expense
    val imageUri: String? = null                          // Optional URI for attached receipt/image
)
