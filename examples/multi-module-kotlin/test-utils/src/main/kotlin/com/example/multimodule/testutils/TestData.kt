package com.example.multimodule.testutils

import com.example.multimodule.core.User
import com.example.multimodule.core.Message

/**
 * Test data generators for unit testing.
 * This module is internal and not published.
 */
object TestData {
    
    /**
     * Creates a test user with default values.
     */
    fun createTestUser(
        id: String = "test-user-1",
        name: String = "Test User",
        email: String = "test@example.com"
    ): User = User(id, name, email)
    
    /**
     * Creates multiple test users.
     */
    fun createTestUsers(count: Int): List<User> {
        return (1..count).map { i ->
            User(
                id = "test-user-$i",
                name = "Test User $i",
                email = "test$i@example.com"
            )
        }
    }
    
    /**
     * Creates a test message.
     */
    fun createTestMessage(
        from: String = "user1",
        to: String = "user2",
        content: String = "Test message"
    ): Message = Message(
        id = "msg-${System.currentTimeMillis()}",
        fromUserId = from,
        toUserId = to,
        content = content
    )
    
    /**
     * Generates random email addresses for testing.
     */
    fun generateEmails(count: Int): List<String> {
        val domains = listOf("example.com", "test.org", "demo.net")
        return (1..count).map { i ->
            "user$i@${domains.random()}"
        }
    }
}

/**
 * Test assertions for common scenarios.
 */
object TestAssertions {
    
    fun assertValidUser(user: User) {
        require(user.isValid()) { "User is not valid: $user" }
        require(user.id.isNotBlank()) { "User ID is blank" }
        require(user.name.isNotBlank()) { "User name is blank" }
        require(user.email.contains("@")) { "Invalid email format" }
    }
    
    fun assertValidMessage(message: Message) {
        require(message.id.isNotBlank()) { "Message ID is blank" }
        require(message.fromUserId.isNotBlank()) { "From user ID is blank" }
        require(message.toUserId.isNotBlank()) { "To user ID is blank" }
        require(!message.isEmpty()) { "Message content is empty" }
    }
}