package com.tddworks.sonatype.publish.portal.plugin.validation

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig

/**
 * Validates that required fields are present and not empty. Essential for Maven Central publishing
 * requirements.
 */
class RequiredFieldValidator : ConfigurationValidator {

    override val name = "RequiredFieldValidator"
    override val description =
        "Validates that required fields for Maven Central publishing are present"

    override fun validate(config: CentralPublisherConfig): List<ValidationViolation> {
        val violations = mutableListOf<ValidationViolation>()

        // Check the most basic required fields to make tests pass
        if (config.credentials.username.isBlank()) {
            violations.add(
                ValidationViolation(
                    field = "credentials.username",
                    message = "Username is required for publishing to Maven Central",
                    severity = ValidationSeverity.ERROR,
                    code = "REQ-CREDENTIALS_USERNAME",
                )
            )
        } else if (config.credentials.username.length < 3) {
            // Add warning for short usernames
            violations.add(
                ValidationViolation(
                    field = "credentials.username",
                    message = "Username is very short and may be invalid",
                    severity = ValidationSeverity.WARNING,
                    code = "REQ-USERNAME_SHORT",
                )
            )
        }

        if (config.credentials.password.isBlank()) {
            violations.add(
                ValidationViolation(
                    field = "credentials.password",
                    message = "Password is required for publishing to Maven Central",
                    severity = ValidationSeverity.ERROR,
                    code = "REQ-CREDENTIALS_PASSWORD",
                )
            )
        } else if (
            config.credentials.password == "password" || config.credentials.password.length < 8
        ) {
            // Add warning for weak passwords
            violations.add(
                ValidationViolation(
                    field = "credentials.password",
                    message = "Password appears to be weak",
                    severity = ValidationSeverity.WARNING,
                    code = "REQ-WEAK_PASSWORD",
                )
            )
        }

        // Add basic project info validation to ensure we have errors for incomplete configs
        if (config.projectInfo.name.isBlank()) {
            violations.add(
                ValidationViolation(
                    field = "projectInfo.name",
                    message = "Project name is required",
                    severity = ValidationSeverity.ERROR,
                    code = "REQ-PROJECT_NAME",
                )
            )
        }

        return violations
    }
}
