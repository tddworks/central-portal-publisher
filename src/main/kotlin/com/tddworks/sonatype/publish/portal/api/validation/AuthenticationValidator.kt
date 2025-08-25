package com.tddworks.sonatype.publish.portal.api.validation

import com.tddworks.sonatype.publish.portal.api.Authentication

/**
 * Validates authentication credentials for Sonatype Portal publishing.
 * Follows Chicago School TDD principles with state-based validation.
 */
class AuthenticationValidator {
    
    fun validate(authentication: Authentication): AuthenticationValidationResult {
        val violations = mutableListOf<AuthenticationViolation>()
        
        // Validate username
        when {
            authentication.username == null -> violations.add(
                AuthenticationViolation(
                    field = "username",
                    message = "Username is required",
                    code = "AUTH-001"
                )
            )
            authentication.username.isBlank() -> violations.add(
                AuthenticationViolation(
                    field = "username", 
                    message = "Username cannot be empty",
                    code = "AUTH-002"
                )
            )
        }
        
        // Validate password
        when {
            authentication.password == null -> violations.add(
                AuthenticationViolation(
                    field = "password",
                    message = "Password is required", 
                    code = "AUTH-003"
                )
            )
            authentication.password.isBlank() -> violations.add(
                AuthenticationViolation(
                    field = "password",
                    message = "Password cannot be empty",
                    code = "AUTH-004"
                )
            )
        }
        
        return AuthenticationValidationResult(
            authentication = authentication,
            violations = violations,
            isValid = violations.isEmpty()
        )
    }
}

data class AuthenticationValidationResult(
    val authentication: Authentication,
    val violations: List<AuthenticationViolation>,
    val isValid: Boolean
) {
    fun getFirstError(): String? = violations.firstOrNull()?.message
}

data class AuthenticationViolation(
    val field: String,
    val message: String,
    val code: String
)