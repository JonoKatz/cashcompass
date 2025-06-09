package com.example.cashcompass.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// Represents a table called 'users' in the Room database
@Entity(tableName = "users")
data class UserEntity(
    // The username acts as the primary key for this table (each user must have a unique username)
    @PrimaryKey val username: String,

    // Stores the user's password
    val password: String
)
