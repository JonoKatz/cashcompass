package com.example.cashcompass.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Defines the Room database with entities for users and expenses
@Database(entities = [UserEntity::class, ExpenseEntity::class], version = 2)
abstract class AppDatabase : RoomDatabase() {

    // DAO access points for interacting with the database
    abstract fun userDao(): UserDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Singleton pattern to ensure a single database instance is used
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Builds the database instance and supports destructive migration
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .fallbackToDestructiveMigration() // Deletes data on version mismatch
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
