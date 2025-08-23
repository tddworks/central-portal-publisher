package com.tddworks.sonatype.publish.portal.plugin.validation

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig

/**
 * Core validation engine that orchestrates all validation rules. Provides structured validation
 * results with severity levels and actionable suggestions.
 */
class ValidationEngine {

    private val validators = mutableListOf<ConfigurationValidator>()

    init {
        // Register built-in validators
        validators.addAll(listOf(RequiredFieldValidator()))
    }

    /** Validates a configuration and returns detailed results */
    fun validate(config: CentralPublisherConfig): ValidationResult {
        val violations = mutableListOf<ValidationViolation>()

        for (validator in validators) {
            violations.addAll(validator.validate(config))
        }

        return ValidationResult(
            config = config,
            violations = violations,
            isValid = violations.none { it.severity == ValidationSeverity.ERROR },
        )
    }

    /** Adds a custom validator to the engine */
    fun addValidator(validator: ConfigurationValidator) {
        validators.add(validator)
    }

    /** Removes a validator from the engine */
    fun removeValidator(validatorClass: Class<out ConfigurationValidator>) {
        validators.removeIf { it::class.java == validatorClass }
    }
}

/** Base interface for all configuration validators */
interface ConfigurationValidator {
    val name: String
    val description: String

    /** Validates a configuration section and returns violations */
    fun validate(config: CentralPublisherConfig): List<ValidationViolation>
}

/** Represents a validation violation with context and suggested fixes */
data class ValidationViolation(
    val field: String,
    val message: String,
    val severity: ValidationSeverity,
    val code: String,
    val suggestion: String? = null,
    val documentationUrl: String? = null,
    val fixCommand: String? = null,
)

/** Severity levels for validation violations */
enum class ValidationSeverity {
    ERROR, // Blocks publishing
    WARNING, // May cause issues but allows publishing
    INFO, // Informational only
}

/** Complete validation result with all findings and metadata */
data class ValidationResult(
    val config: CentralPublisherConfig,
    val violations: List<ValidationViolation>,
    val isValid: Boolean,
    val errorCount: Int = violations.count { it.severity == ValidationSeverity.ERROR },
    val warningCount: Int = violations.count { it.severity == ValidationSeverity.WARNING },
    val infoCount: Int = violations.count { it.severity == ValidationSeverity.INFO },
) {

    /** Returns only error-level violations that block publishing */
    fun getErrors(): List<ValidationViolation> =
        violations.filter { it.severity == ValidationSeverity.ERROR }

    /** Returns warning-level violations that may cause issues */
    fun getWarnings(): List<ValidationViolation> =
        violations.filter { it.severity == ValidationSeverity.WARNING }

    /** Returns info-level violations for optimization suggestions */
    fun getInfo(): List<ValidationViolation> =
        violations.filter { it.severity == ValidationSeverity.INFO }

    /** Formats validation results for console output */
    fun formatReport(): String {
        val builder = StringBuilder()

        if (isValid) {
            builder.appendLine("✅ Configuration validation passed")
        } else {
            builder.appendLine("❌ Configuration validation failed")
        }

        builder.appendLine("Summary: $errorCount errors, $warningCount warnings, $infoCount info")

        if (violations.isNotEmpty()) {
            builder.appendLine()

            // Group by severity
            listOf(
                    ValidationSeverity.ERROR to "❌ Errors",
                    ValidationSeverity.WARNING to "⚠️  Warnings",
                    ValidationSeverity.INFO to "ℹ️  Information",
                )
                .forEach { (severity, header) ->
                    val severityViolations = violations.filter { it.severity == severity }
                    if (severityViolations.isNotEmpty()) {
                        builder.appendLine(header)
                        severityViolations.forEach { violation ->
                            builder.appendLine("  ${violation.code}: ${violation.message}")
                            violation.suggestion?.let { builder.appendLine("    💡 ${it}") }
                            violation.fixCommand?.let { builder.appendLine("    🔧 ${it}") }
                            violation.documentationUrl?.let { builder.appendLine("    📖 ${it}") }
                        }
                        builder.appendLine()
                    }
                }
        }

        return builder.toString().trim()
    }
}
