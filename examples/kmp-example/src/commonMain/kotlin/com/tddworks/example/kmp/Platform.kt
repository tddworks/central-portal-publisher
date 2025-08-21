package com.tddworks.example.kmp

/**
 * Platform interface for getting platform-specific information.
 */
expect class Platform() {
    val name: String
}

/**
 * Returns a greeting with platform information.
 */
fun greeting(): String {
    return "Hello from ${Platform().name}!"
}

/**
 * Simple utility class for demonstrating multiplatform functionality.
 */
class KmpUtils {
    /**
     * Formats a message with platform information.
     */
    fun formatMessage(message: String): String {
        return "[${ Platform().name}] $message"
    }
    
    /**
     * Reverses a string - common functionality across all platforms.
     */
    fun reverseString(input: String): String {
        return input.reversed()
    }
}