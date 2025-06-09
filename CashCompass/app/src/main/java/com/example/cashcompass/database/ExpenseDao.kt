package com.example.cashcompass.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

// Data Access Object for handling operations related to the ExpenseEntity table
@Dao
interface ExpenseDao {

    // Inserts a new expense record into the expenses table
    @Insert
    suspend fun insertExpense(expense: ExpenseEntity)

    // Retrieves all expenses for a specific user based on their userId
    @Query("SELECT * FROM expenses WHERE userId = :userId")
    suspend fun getExpensesForUser(userId: String): List<ExpenseEntity>

    // Deletes a specific expense record from the expenses table
    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)
}
