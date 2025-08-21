package com.tddworks.sonatype.publish.portal.plugin.error

/**
 * Comprehensive error code system for the Central Portal Publisher plugin.
 * Provides structured error reporting with actionable fix suggestions.
 */
class PublishingErrorSystem {

    fun createError(code: ErrorCode, message: String, context: ErrorContext? = null): PublishingError {
        return PublishingError(
            code = code,
            message = message,
            category = code.category,
            severity = ErrorSeverity.ERROR,
            context = context
        )
    }

    fun createWarning(code: ErrorCode, message: String, context: ErrorContext? = null): PublishingError {
        return PublishingError(
            code = code,
            message = message,
            category = code.category,
            severity = ErrorSeverity.WARNING,
            context = context
        )
    }

    fun getSuggestionsFor(error: PublishingError): List<FixSuggestion> {
        return when (error.code) {
            ErrorCode.MISSING_CREDENTIALS -> listOf(
                FixSuggestion(
                    type = FixSuggestionType.COMMAND,
                    description = "Set SONATYPE_USERNAME as environment variable",
                    command = "export SONATYPE_USERNAME=your-username",
                    priority = 1
                ),
                FixSuggestion(
                    type = FixSuggestionType.CONFIGURATION,
                    description = "Add SONATYPE_USERNAME to gradle.properties",
                    command = "echo 'SONATYPE_USERNAME=your-username' >> gradle.properties",
                    priority = 2
                ),
                FixSuggestion(
                    type = FixSuggestionType.DOCUMENTATION,
                    description = "See authentication documentation",
                    url = "https://github.com/tddworks/central-portal-publisher#authentication",
                    priority = 3
                )
            )

            ErrorCode.MISSING_SIGNING_KEY -> listOf(
                FixSuggestion(
                    type = FixSuggestionType.COMMAND,
                    description = "Generate a new GPG key",
                    command = "gpg --gen-key",
                    priority = 1
                ),
                FixSuggestion(
                    type = FixSuggestionType.COMMAND,
                    description = "Export existing GPG key",
                    command = "gpg --armor --export-secret-keys your-email@example.com",
                    priority = 2
                ),
                FixSuggestion(
                    type = FixSuggestionType.DOCUMENTATION,
                    description = "See signing setup guide",
                    url = "https://github.com/tddworks/central-portal-publisher#signing",
                    priority = 3
                )
            )

            ErrorCode.INVALID_POM -> listOf(
                FixSuggestion(
                    type = FixSuggestionType.VALIDATION,
                    description = "Check required POM fields (name, description, url, licenses, developers, scm)",
                    priority = 1
                ),
                FixSuggestion(
                    type = FixSuggestionType.CONFIGURATION,
                    description = "Run validation to see specific missing fields",
                    command = "./gradlew validatePublishing",
                    priority = 2
                )
            )

            ErrorCode.UPLOAD_FAILED -> listOf(
                FixSuggestion(
                    type = FixSuggestionType.RETRY,
                    description = "Check network connection and retry",
                    command = "./gradlew publishToCentral --rerun-tasks",
                    priority = 1
                ),
                FixSuggestion(
                    type = FixSuggestionType.VALIDATION,
                    description = "Verify Sonatype credentials are correct",
                    priority = 2
                )
            )

            ErrorCode.MISSING_DESCRIPTION -> listOf(
                FixSuggestion(
                    type = FixSuggestionType.CONFIGURATION,
                    description = "Add project description",
                    command = "echo 'POM_DESCRIPTION=Your project description' >> gradle.properties",
                    priority = 1
                )
            )

            ErrorCode.UNKNOWN -> listOf(
                FixSuggestion(
                    type = FixSuggestionType.GENERIC,
                    description = "Please report this issue with logs",
                    url = "https://github.com/tddworks/central-portal-publisher/issues",
                    priority = 1
                ),
                FixSuggestion(
                    type = FixSuggestionType.COMMAND,
                    description = "Run with verbose logging",
                    command = "./gradlew publishToCentral --info --stacktrace",
                    priority = 2
                )
            )
        }
    }

    fun formatError(error: PublishingError): String {
        val suggestions = getSuggestionsFor(error)
        val contextInfo = error.context?.let { ctx ->
            buildString {
                if (ctx.projectPath.isNotBlank()) append("  Project: ${ctx.projectPath}\n")
                if (ctx.taskName.isNotBlank()) append("  Task: ${ctx.taskName}\n")
                if (ctx.additionalInfo.isNotEmpty()) {
                    ctx.additionalInfo.forEach { (key, value) ->
                        append("  $key: $value\n")
                    }
                }
            }
        } ?: ""

        return buildString {
            append("${error.severity.name} ${error.code.code}: ${error.message}\n")
            if (contextInfo.isNotBlank()) {
                append("\nContext:\n$contextInfo")
            }
            if (suggestions.isNotEmpty()) {
                append("\nHow to fix:\n")
                suggestions.sortedBy { it.priority }.forEach { suggestion ->
                    append("  ${suggestion.priority}. ${suggestion.description}")
                    suggestion.command?.let { append("\n     Command: $it") }
                    suggestion.url?.let { append("\n     See: $it") }
                    append("\n")
                }
            }
        }
    }

    fun createErrorReport(errors: List<PublishingError>): ErrorReport {
        val errorsByCategory = errors.groupBy { it.category }
        val errorCount = errors.count { it.severity == ErrorSeverity.ERROR }
        val warningCount = errors.count { it.severity == ErrorSeverity.WARNING }

        val formattedOutput = buildString {
            append("╔═══════════════════════════════════════════════════════════════╗\n")
            append("║                    PUBLISHING ISSUES REPORT                   ║\n")
            append("╚═══════════════════════════════════════════════════════════════╝\n\n")
            
            if (errorCount > 0) {
                append("❌ $errorCount error(s) found\n")
            }
            if (warningCount > 0) {
                append("⚠️  $warningCount warning(s) found\n")
            }
            append("\n")

            ErrorCategory.values().filter { it != ErrorCategory.UNKNOWN }.forEach { category ->
                val categoryErrors = errorsByCategory[category] ?: emptyList()
                if (categoryErrors.isNotEmpty()) {
                    append("${category.displayName} Issues:\n")
                    append("${"─".repeat(category.displayName.length + 8)}\n")
                    categoryErrors.forEach { error ->
                        append(formatError(error))
                        append("\n")
                    }
                }
            }

            if (errorCount == 0 && warningCount == 0) {
                append("✅ No issues found! Your configuration looks good.\n")
            }
        }

        return ErrorReport(
            errors = errors,
            summary = ErrorSummary(
                totalErrors = errorCount,
                totalWarnings = warningCount,
                categoryCounts = errorsByCategory.mapValues { it.value.size }
            ),
            formattedOutput = formattedOutput
        )
    }
}

/**
 * Error codes with consistent naming and categorization.
 */
enum class ErrorCode(val code: String, val category: ErrorCategory) {
    // Configuration errors (PUB-0xx)
    MISSING_CREDENTIALS("PUB-002", ErrorCategory.CONFIGURATION),
    MISSING_SIGNING_KEY("PUB-001", ErrorCategory.CONFIGURATION), 
    MISSING_DESCRIPTION("PUB-003", ErrorCategory.CONFIGURATION),
    
    // Validation errors (PUB-1xx)
    INVALID_POM("PUB-101", ErrorCategory.VALIDATION),
    
    // Network errors (PUB-2xx)
    UPLOAD_FAILED("PUB-201", ErrorCategory.NETWORK),
    
    // Unknown/Generic (PUB-999)
    UNKNOWN("PUB-999", ErrorCategory.UNKNOWN);
}

enum class ErrorCategory(val displayName: String) {
    CONFIGURATION("Configuration"),
    VALIDATION("Validation"),
    NETWORK("Network"),
    UNKNOWN("Unknown")
}

enum class ErrorSeverity {
    ERROR,
    WARNING
}

enum class FixSuggestionType {
    COMMAND,
    CONFIGURATION,
    DOCUMENTATION,
    VALIDATION,
    RETRY,
    GENERIC
}

data class PublishingError(
    val code: ErrorCode,
    val message: String,
    val category: ErrorCategory,
    val severity: ErrorSeverity,
    val context: ErrorContext? = null
)

data class ErrorContext(
    val projectPath: String = "",
    val taskName: String = "",
    val additionalInfo: Map<String, String> = emptyMap()
)

data class FixSuggestion(
    val type: FixSuggestionType,
    val description: String,
    val command: String? = null,
    val url: String? = null,
    val priority: Int
)

data class ErrorReport(
    val errors: List<PublishingError>,
    val summary: ErrorSummary,
    val formattedOutput: String
)

data class ErrorSummary(
    val totalErrors: Int,
    val totalWarnings: Int,
    val categoryCounts: Map<ErrorCategory, Int>
)