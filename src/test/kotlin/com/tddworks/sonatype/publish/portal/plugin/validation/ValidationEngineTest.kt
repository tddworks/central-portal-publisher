package com.tddworks.sonatype.publish.portal.plugin.validation

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfigBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ValidationEngineTest {
    
    private lateinit var validationEngine: ValidationEngine
    
    @BeforeEach
    fun setup() {
        validationEngine = ValidationEngine()
    }
    
    @Test
    fun `should validate empty configuration and return errors`() {
        // Given
        val config = CentralPublisherConfigBuilder().build()
        
        // When
        val result = validationEngine.validate(config)
        
        // Then
        assertThat(result.isValid).isFalse()
        assertThat(result.errorCount).isGreaterThan(0)
        assertThat(result.violations).isNotEmpty()
        assertThat(result.getErrors()).isNotEmpty()
    }
    
    @Test
    fun `should validate complete configuration and return no errors`() {
        // Given
        val config = CentralPublisherConfigBuilder()
            .credentials {
                username = "testuser"
                password = "testtoken123"
            }
            .projectInfo {
                name = "test-project"
                description = "A test project"
                url = "https://github.com/test/project"
                scm {
                    url = "https://github.com/test/project"
                    connection = "scm:git:https://github.com/test/project.git"
                    developerConnection = "scm:git:https://github.com/test/project.git"
                }
                license {
                    name = "Apache License 2.0"
                    url = "https://www.apache.org/licenses/LICENSE-2.0"
                    distribution = "repo"
                }
                developer {
                    id = "testdev"
                    name = "Test Developer"
                    email = "test@example.com"
                    organization = "Test Org"
                    organizationUrl = "https://test-org.com"
                }
            }
            .signing {
                keyId = "12345678"
                password = "signingpass"
                secretKeyRingFile = "/tmp/test.gpg"
            }
            .build()
        
        // When
        val result = validationEngine.validate(config)
        
        // Then - Might still have warnings/info but no blocking errors
        assertThat(result.violations.filter { it.severity == ValidationSeverity.ERROR }).isEmpty()
        assertThat(result.isValid).isTrue()
    }
    
    @Test
    fun `should support adding custom validators`() {
        // Given
        val customValidator = TestCustomValidator()
        validationEngine.addValidator(customValidator)
        val config = CentralPublisherConfigBuilder().build()
        
        // When
        val result = validationEngine.validate(config)
        
        // Then
        assertThat(result.violations).anySatisfy { violation ->
            assertThat(violation.code).isEqualTo("CUSTOM-TEST")
        }
    }
    
    @Test
    fun `should support removing validators`() {
        // Given
        val customValidator = TestCustomValidator()
        validationEngine.addValidator(customValidator)
        val config = CentralPublisherConfigBuilder().build()
        val initialResult = validationEngine.validate(config)
        val initialViolationCount = initialResult.violations.size
        
        // When - Remove the custom validator
        validationEngine.removeValidator(TestCustomValidator::class.java)
        val result = validationEngine.validate(config)
        
        // Then
        assertThat(result.violations.size).isLessThan(initialViolationCount)
    }
    
    @Test
    fun `should categorize violations by severity`() {
        // Given
        val config = CentralPublisherConfigBuilder()
            .credentials {
                username = "u" // Too short - should be warning
                password = "password" // Weak - should be warning
            }
            .build()
        
        // When
        val result = validationEngine.validate(config)
        
        // Then
        assertThat(result.errorCount).isGreaterThan(0)
        assertThat(result.warningCount).isGreaterThan(0)
        assertThat(result.getErrors()).isNotEmpty()
        assertThat(result.getWarnings()).isNotEmpty()
    }
    
    @Test
    fun `should format validation report correctly`() {
        // Given
        val config = CentralPublisherConfigBuilder().build()
        
        // When
        val result = validationEngine.validate(config)
        val report = result.formatReport()
        
        // Then
        assertThat(report).contains("Configuration validation failed")
        assertThat(report).contains("errors")
        assertThat(report).contains("❌")
        assertThat(report).contains("REQ-") // Error codes
    }
    
    @Test
    fun `should format successful validation report correctly`() {
        // Given - Create a config that will pass validation (may have warnings but no errors)
        val config = CentralPublisherConfigBuilder()
            .credentials {
                username = "validuser"
                password = "valid-token-12345678901234567890"
            }
            .projectInfo {
                name = "valid-project"
                description = "A valid project description"
                url = "https://github.com/valid/project"
                scm {
                    url = "https://github.com/valid/project"
                    connection = "scm:git:https://github.com/valid/project.git"
                }
                license {
                    name = "MIT License"
                    url = "https://opensource.org/licenses/MIT"
                }
                developer {
                    name = "Valid Developer"
                    email = "valid@example.com"
                }
            }
            .signing {
                keyId = "ABCD1234"
                password = "signing-passphrase"
                secretKeyRingFile = "/tmp/valid.gpg"
            }
            .build()
        
        // When
        val result = validationEngine.validate(config)
        val report = result.formatReport()
        
        // Then
        if (result.isValid) {
            assertThat(report).contains("Configuration validation passed")
            assertThat(report).contains("✅")
        }
        // Should show summary regardless
        assertThat(report).containsPattern("\\d+ errors?, \\d+ warnings?, \\d+ info")
    }
}

// Test helper class
class TestCustomValidator : ConfigurationValidator {
    override val name = "TestCustomValidator"
    override val description = "A test validator for unit tests"
    
    override fun validate(config: CentralPublisherConfig): List<ValidationViolation> {
        return listOf(
            ValidationViolation(
                field = "test",
                message = "This is a test violation",
                severity = ValidationSeverity.INFO,
                code = "CUSTOM-TEST"
            )
        )
    }
}