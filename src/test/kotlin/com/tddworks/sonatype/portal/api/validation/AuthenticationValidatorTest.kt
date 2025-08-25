package com.tddworks.sonatype.portal.api.validation

import com.tddworks.sonatype.publish.portal.api.Authentication
import com.tddworks.sonatype.publish.portal.api.validation.AuthenticationValidator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class `Authentication Validator` {

    private lateinit var validator: AuthenticationValidator

    @BeforeEach
    fun setUp() {
        validator = AuthenticationValidator()
    }

    @Nested
    inner class `When authentication is valid` {

        @Test
        fun `should pass validation with valid credentials`() {
            val authentication = Authentication("valid-user", "valid-password")

            val result = validator.validate(authentication)

            assertThat(result.isValid).isTrue()
            assertThat(result.violations).isEmpty()
            assertThat(result.getFirstError()).isNull()
        }
    }

    @Nested
    inner class `When username is invalid` {

        @Test
        fun `should fail validation when username is null`() {
            val authentication = Authentication(username = null, password = "valid-password")

            val result = validator.validate(authentication)

            assertThat(result.isValid).isFalse()
            assertThat(result.violations).hasSize(1)
            assertThat(result.violations[0].field).isEqualTo("username")
            assertThat(result.violations[0].message).isEqualTo("Username is required")
            assertThat(result.violations[0].code).isEqualTo("AUTH-001")
        }

        @Test
        fun `should fail validation when username is empty`() {
            val authentication = Authentication(username = "", password = "valid-password")

            val result = validator.validate(authentication)

            assertThat(result.isValid).isFalse()
            assertThat(result.violations).hasSize(1)
            assertThat(result.violations[0].field).isEqualTo("username")
            assertThat(result.violations[0].message).isEqualTo("Username cannot be empty")
            assertThat(result.violations[0].code).isEqualTo("AUTH-002")
        }

        @Test
        fun `should fail validation when username is blank`() {
            val authentication = Authentication(username = "   ", password = "valid-password")

            val result = validator.validate(authentication)

            assertThat(result.isValid).isFalse()
            assertThat(result.violations).hasSize(1)
            assertThat(result.violations[0].message).isEqualTo("Username cannot be empty")
        }
    }

    @Nested
    inner class `When password is invalid` {

        @Test
        fun `should fail validation when password is null`() {
            val authentication = Authentication(username = "valid-user", password = null)

            val result = validator.validate(authentication)

            assertThat(result.isValid).isFalse()
            assertThat(result.violations).hasSize(1)
            assertThat(result.violations[0].field).isEqualTo("password")
            assertThat(result.violations[0].message).isEqualTo("Password is required")
            assertThat(result.violations[0].code).isEqualTo("AUTH-003")
        }

        @Test
        fun `should fail validation when password is empty`() {
            val authentication = Authentication(username = "valid-user", password = "")

            val result = validator.validate(authentication)

            assertThat(result.isValid).isFalse()
            assertThat(result.violations).hasSize(1)
            assertThat(result.violations[0].field).isEqualTo("password")
            assertThat(result.violations[0].message).isEqualTo("Password cannot be empty")
            assertThat(result.violations[0].code).isEqualTo("AUTH-004")
        }

        @Test
        fun `should fail validation when password is blank`() {
            val authentication = Authentication(username = "valid-user", password = "   ")

            val result = validator.validate(authentication)

            assertThat(result.isValid).isFalse()
            assertThat(result.violations).hasSize(1)
            assertThat(result.violations[0].message).isEqualTo("Password cannot be empty")
        }
    }

    @Nested
    inner class `When both credentials are invalid` {

        @Test
        fun `should report all validation errors`() {
            val authentication = Authentication(username = null, password = null)

            val result = validator.validate(authentication)

            assertThat(result.isValid).isFalse()
            assertThat(result.violations).hasSize(2)

            val usernameViolation = result.violations.find { it.field == "username" }
            assertThat(usernameViolation?.message).isEqualTo("Username is required")

            val passwordViolation = result.violations.find { it.field == "password" }
            assertThat(passwordViolation?.message).isEqualTo("Password is required")
        }
    }
}
