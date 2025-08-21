package com.example.multimodule.api

import com.example.multimodule.core.User
import com.example.multimodule.core.Result

/**
 * Service interface for user operations.
 * This is the public API that clients will use.
 */
interface UserService {
    
    /**
     * Creates a new user.
     */
    suspend fun createUser(name: String, email: String): Result<User>
    
    /**
     * Retrieves a user by ID.
     */
    suspend fun getUser(userId: String): Result<User>
    
    /**
     * Updates an existing user.
     */
    suspend fun updateUser(user: User): Result<User>
    
    /**
     * Deletes a user by ID.
     */
    suspend fun deleteUser(userId: String): Result<Unit>
    
    /**
     * Lists all users.
     */
    suspend fun listUsers(): Result<List<User>>
    
    /**
     * Searches for users by name.
     */
    suspend fun searchUsers(query: String): Result<List<User>>
}

/**
 * Default implementation of UserService.
 */
class InMemoryUserService : UserService {
    private val users = mutableMapOf<String, User>()
    private var nextId = 1
    
    override suspend fun createUser(name: String, email: String): Result<User> {
        return try {
            val user = User(
                id = "user-${nextId++}",
                name = name,
                email = email
            )
            
            if (!user.isValid()) {
                return Result.Error("Invalid user data")
            }
            
            users[user.id] = user
            Result.Success(user)
        } catch (e: Exception) {
            Result.Error("Failed to create user", e)
        }
    }
    
    override suspend fun getUser(userId: String): Result<User> {
        return users[userId]?.let { Result.Success(it) }
            ?: Result.Error("User not found: $userId")
    }
    
    override suspend fun updateUser(user: User): Result<User> {
        return if (users.containsKey(user.id)) {
            users[user.id] = user
            Result.Success(user)
        } else {
            Result.Error("User not found: ${user.id}")
        }
    }
    
    override suspend fun deleteUser(userId: String): Result<Unit> {
        return if (users.remove(userId) != null) {
            Result.Success(Unit)
        } else {
            Result.Error("User not found: $userId")
        }
    }
    
    override suspend fun listUsers(): Result<List<User>> {
        return Result.Success(users.values.toList())
    }
    
    override suspend fun searchUsers(query: String): Result<List<User>> {
        val results = users.values.filter { user ->
            user.name.contains(query, ignoreCase = true) ||
            user.email.contains(query, ignoreCase = true)
        }
        return Result.Success(results)
    }
}