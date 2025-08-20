package com.tddworks.sonatype.publish.portal.plugin.error

import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach

class ErrorSystemTest {

    private lateinit var errorSystem: PublishingErrorSystem

    @BeforeEach
    fun setup() {
        errorSystem = PublishingErrorSystem()
    }

    @Test
    fun `should create error with code and message`() {
        // Given/When
        val error = errorSystem.createError(
            ErrorCode.MISSING_CREDENTIALS,
            "Username is not configured"
        )

        // Then
        assertThat(error.code).isEqualTo(ErrorCode.MISSING_CREDENTIALS)
        assertThat(error.message).isEqualTo("Username is not configured")
        assertThat(error.category).isEqualTo(ErrorCategory.CONFIGURATION)
        assertThat(error.severity).isEqualTo(ErrorSeverity.ERROR)
    }

    @Test
    fun `should provide fix suggestions for errors`() {
        // Given
        val error = errorSystem.createError(
            ErrorCode.MISSING_CREDENTIALS,
            "Username is not configured"
        )

        // When
        val suggestions = errorSystem.getSuggestionsFor(error)

        // Then
        assertThat(suggestions).hasSize(3) // We provide 3 suggestions now
        assertThat(suggestions[0].description).contains("Set SONATYPE_USERNAME")
        assertThat(suggestions[1].description).contains("gradle.properties")
        assertThat(suggestions[2].description).contains("documentation")
        assertThat(suggestions[0].command).isEqualTo("export SONATYPE_USERNAME=your-username")
    }

    @Test
    fun `should format error for display`() {
        // Given
        val error = errorSystem.createError(
            ErrorCode.MISSING_SIGNING_KEY,
            "GPG signing key is not configured"
        )

        // When
        val formatted = errorSystem.formatError(error)

        // Then
        assertThat(formatted).contains("ERROR")
        assertThat(formatted).contains("PUB-001")
        assertThat(formatted).contains("GPG signing key is not configured")
        assertThat(formatted).contains("How to fix:")
        assertThat(formatted).contains("gpg --gen-key")
    }

    @Test
    fun `should create error report with multiple errors`() {
        // Given
        val errors = listOf(
            errorSystem.createError(ErrorCode.MISSING_CREDENTIALS, "No username"),
            errorSystem.createError(ErrorCode.MISSING_SIGNING_KEY, "No signing key")
        )

        // When
        val report = errorSystem.createErrorReport(errors)

        // Then
        assertThat(report.errors).hasSize(2)
        assertThat(report.summary.totalErrors).isEqualTo(2)
        assertThat(report.summary.totalWarnings).isEqualTo(0)
        assertThat(report.formattedOutput).contains("2 error(s) found")
        assertThat(report.formattedOutput).contains("PUB-002") // MISSING_CREDENTIALS
        assertThat(report.formattedOutput).contains("PUB-001") // MISSING_SIGNING_KEY
    }

    @Test
    fun `should categorize errors correctly`() {
        // Given/When
        val configError = errorSystem.createError(ErrorCode.MISSING_CREDENTIALS, "Config issue")
        val networkError = errorSystem.createError(ErrorCode.UPLOAD_FAILED, "Network issue")
        val validationError = errorSystem.createError(ErrorCode.INVALID_POM, "Validation issue")

        // Then
        assertThat(configError.category).isEqualTo(ErrorCategory.CONFIGURATION)
        assertThat(networkError.category).isEqualTo(ErrorCategory.NETWORK)
        assertThat(validationError.category).isEqualTo(ErrorCategory.VALIDATION)
    }

    @Test
    fun `should support different severity levels`() {
        // Given/When
        val error = errorSystem.createError(ErrorCode.MISSING_CREDENTIALS, "Test")
        val warning = errorSystem.createWarning(ErrorCode.MISSING_DESCRIPTION, "Test warning")

        // Then
        assertThat(error.severity).isEqualTo(ErrorSeverity.ERROR)
        assertThat(warning.severity).isEqualTo(ErrorSeverity.WARNING)
    }

    @Test
    fun `should provide contextual information for errors`() {
        // Given
        val context = ErrorContext(
            projectPath = ":example-project",
            taskName = "publishToCentral",
            additionalInfo = mapOf("publication" to "maven")
        )

        // When
        val error = errorSystem.createError(
            ErrorCode.UPLOAD_FAILED,
            "Upload failed",
            context
        )

        // Then
        assertThat(error.context?.projectPath).isEqualTo(":example-project")
        assertThat(error.context?.taskName).isEqualTo("publishToCentral")
        assertThat(error.context?.additionalInfo?.get("publication")).isEqualTo("maven")
    }

    @Test
    fun `should provide documentation links for errors`() {
        // Given
        val error = errorSystem.createError(ErrorCode.MISSING_CREDENTIALS, "Test")

        // When
        val suggestions = errorSystem.getSuggestionsFor(error)

        // Then
        val docSuggestion = suggestions.find { it.type == FixSuggestionType.DOCUMENTATION }
        assertThat(docSuggestion).isNotNull
        assertThat(docSuggestion?.url).startsWith("https://")
        assertThat(docSuggestion?.description).contains("documentation")
    }

    @Test
    fun `should handle unknown errors gracefully`() {
        // Given
        val unknownError = PublishingError(
            code = ErrorCode.UNKNOWN,
            message = "Something went wrong",
            category = ErrorCategory.UNKNOWN,
            severity = ErrorSeverity.ERROR
        )

        // When
        val suggestions = errorSystem.getSuggestionsFor(unknownError)
        val formatted = errorSystem.formatError(unknownError)

        // Then
        assertThat(suggestions).isNotEmpty
        assertThat(suggestions[0].type).isEqualTo(FixSuggestionType.GENERIC)
        assertThat(formatted).contains("PUB-999") // The error code shows as PUB-999
        assertThat(formatted).contains("Please report this issue")
    }

    @Test
    fun `should validate error code uniqueness`() {
        // Given/When/Then
        val codes = ErrorCode.values()
        val codeValues = codes.map { it.code }
        
        assertThat(codeValues).doesNotHaveDuplicates()
        codes.forEach { code ->
            assertThat(code.code).startsWith("PUB-")
            assertThat(code.code).hasSize(7) // PUB-XXX format
        }
    }
}