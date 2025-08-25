package com.tddworks.sonatype.publish.portal.api.validation

import com.tddworks.sonatype.publish.portal.api.DeploymentBundle

/**
 * Validates deployment bundles for Sonatype Portal publishing. Follows Chicago School TDD
 * principles with state-based validation.
 */
class DeploymentBundleValidator {

    fun validate(deploymentBundle: DeploymentBundle): DeploymentBundleValidationResult {
        val violations = mutableListOf<DeploymentBundleViolation>()

        val file = deploymentBundle.file

        // Validate file existence
        if (!file.exists()) {
            violations.add(
                DeploymentBundleViolation(
                    field = "file",
                    message = "Deployment file does not exist: ${file.absolutePath}",
                    code = "BUNDLE-001",
                )
            )
        } else {
            // Only check readability if file exists
            if (!file.canRead()) {
                violations.add(
                    DeploymentBundleViolation(
                        field = "file",
                        message = "Deployment file is not readable: ${file.absolutePath}",
                        code = "BUNDLE-002",
                    )
                )
            }

            // Check if file is empty
            if (file.length() == 0L) {
                violations.add(
                    DeploymentBundleViolation(
                        field = "file",
                        message = "Deployment file is empty: ${file.absolutePath}",
                        code = "BUNDLE-003",
                    )
                )
            }
        }

        return DeploymentBundleValidationResult(
            deploymentBundle = deploymentBundle,
            violations = violations,
            isValid = violations.isEmpty(),
        )
    }
}

data class DeploymentBundleValidationResult(
    val deploymentBundle: DeploymentBundle,
    val violations: List<DeploymentBundleViolation>,
    val isValid: Boolean,
) {
    fun getFirstError(): String? = violations.firstOrNull()?.message
}

data class DeploymentBundleViolation(val field: String, val message: String, val code: String)
