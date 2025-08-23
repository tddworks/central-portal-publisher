package com.example.multimodule.client

import com.example.multimodule.api.InMemoryUserService
import com.example.multimodule.api.UserService
import com.example.multimodule.core.Result
import com.example.multimodule.core.User
import kotlinx.coroutines.delay

/**
 * High-level client for user operations. This provides a simplified interface for common use cases.
 */
class UserClient(private val userService: UserService = InMemoryUserService()) {

    /** Creates a new user with validation. */
    suspend fun registerUser(name: String, email: String): User? {
        // Add some client-side validation
        if (name.length < 2) {
            throw IllegalArgumentException("Name must be at least 2 characters")
        }

        if (!email.matches(Regex(".+@.+\\..+"))) {
            throw IllegalArgumentException("Invalid email format")
        }

        return when (val result = userService.createUser(name, email)) {
            is Result.Success -> result.data
            is Result.Error -> {
                println("Failed to register user: ${result.message}")
                null
            }
        }
    }

    /** Batch creates multiple users. */
    suspend fun registerUsers(users: List<Pair<String, String>>): List<User> {
        val registered = mutableListOf<User>()

        for ((name, email) in users) {
            delay(10) // Simulate some processing time
            registerUser(name, email)?.let { registered.add(it) }
        }

        return registered
    }

    /** Finds users matching a query with retry logic. */
    suspend fun findUsers(query: String, maxRetries: Int = 3): List<User> {
        var attempt = 0

        while (attempt < maxRetries) {
            when (val result = userService.searchUsers(query)) {
                is Result.Success -> return result.data
                is Result.Error -> {
                    attempt++
                    if (attempt < maxRetries) {
                        delay(100L * attempt) // Exponential backoff
                    }
                }
            }
        }

        return emptyList()
    }

    /** Gets user statistics. */
    suspend fun getUserStats(): UserStats {
        return when (val result = userService.listUsers()) {
            is Result.Success -> {
                val users = result.data
                UserStats(
                    totalUsers = users.size,
                    averageNameLength =
                        if (users.isNotEmpty()) {
                            users.map { it.name.length }.average()
                        } else 0.0,
                    uniqueDomains = users.map { it.email.substringAfter("@") }.toSet().size,
                )
            }
            is Result.Error -> UserStats()
        }
    }

    /** Statistics about users in the system. */
    data class UserStats(
        val totalUsers: Int = 0,
        val averageNameLength: Double = 0.0,
        val uniqueDomains: Int = 0,
    )
}

/** DSL for building user operations. */
class UserOperations {
    private val operations = mutableListOf<suspend (UserService) -> Unit>()

    fun create(name: String, email: String) {
        operations.add { service -> service.createUser(name, email) }
    }

    fun delete(userId: String) {
        operations.add { service -> service.deleteUser(userId) }
    }

    suspend fun execute(service: UserService) {
        operations.forEach { operation -> operation(service) }
    }
}

/** DSL function for user operations. */
suspend fun userOperations(
    service: UserService = InMemoryUserService(),
    block: UserOperations.() -> Unit,
) {
    val ops = UserOperations().apply(block)
    ops.execute(service)
}
