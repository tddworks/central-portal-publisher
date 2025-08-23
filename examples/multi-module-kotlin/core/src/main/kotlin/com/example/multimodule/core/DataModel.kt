package com.example.multimodule.core

/**
 * Represents a user in the system.
 *
 * Core data models used across the multi-module project.
 */
data class User(
    val id: String,
    val name: String,
    val email: String,
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun isValid(): Boolean {
        return id.isNotBlank() && name.isNotBlank() && email.contains("@")
    }
}

/** Represents a message that can be sent between users. */
data class Message(
    val id: String,
    val fromUserId: String,
    val toUserId: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
) {
    fun isEmpty(): Boolean = content.isBlank()
}

/** Result wrapper for operations that can fail. */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()

    data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>()

    inline fun <R> map(transform: (T) -> R): Result<R> =
        when (this) {
            is Success -> Success(transform(data))
            is Error -> this
        }

    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (String) -> Unit): Result<T> {
        if (this is Error) action(message)
        return this
    }
}
