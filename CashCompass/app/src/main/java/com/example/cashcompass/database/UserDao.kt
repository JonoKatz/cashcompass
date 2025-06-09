package com.example.cashcompass.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

// DAO (Data Access Object) for interacting with the 'users' table in the Room database
@Dao
interface UserDao {

    // Inserts a new user into the database
    @Insert
    suspend fun insertUser(user: UserEntity)

    // Retrieves a user by their username (used for login and validation)
    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUserByUsername(username: String): UserEntity?

    // Updates an existing user (e.g., for password changes)
    @Update
    suspend fun updateUser(user: UserEntity)
}
