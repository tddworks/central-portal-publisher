package com.tddworks.sonatype.publish.portal.plugin.dryrun

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import com.tddworks.sonatype.publish.portal.plugin.validation.ValidationEngine
import kotlin.system.measureTimeMillis
import org.gradle.api.Project

/**
 * Engine for simulating publishing operations without performing actual side effects.
 *
 * The DryRunEngine validates configuration and simulates all publishing steps, providing detailed
 * reports of what would happen during actual publishing.
 */
class DryRunEngine(private val dryRunEnabled: Boolean = false) {

    val isDryRunEnabled: Boolean = dryRunEnabled

    /**
     * Simulates the entire publishing process without performing actual operations.
     *
     * @param project The Gradle project
     * @param config The publishing configuration
     * @return Detailed simulation result with all steps and validation results
     */
    fun simulatePublishing(project: Project, config: CentralPublisherConfig): DryRunResult {
        val effectiveDryRun = dryRunEnabled || config.publishing.dryRun
        val simulatedSteps = mutableListOf<SimulatedStep>()
        val validationErrors = mutableListOf<String>()

        var totalDuration = 0L

        // Step 1: Configuration Validation
        totalDuration += measureTimeMillis {
            val validator = ValidationEngine()
            val validationResult = validator.validate(config)

            simulatedSteps.add(
                SimulatedStep(
                    stepName = "Configuration Validation",
                    description = "DRY RUN: Validating publishing configuration",
                    isSimulated = true,
                    duration = 50L,
                )
            )

            if (!validationResult.isValid) {
                validationErrors.addAll(validationResult.getErrors().map { it.message })
            }
        }

        // Step 2: Artifact Preparation
        totalDuration += measureTimeMillis {
            simulatedSteps.add(
                SimulatedStep(
                    stepName = "Artifact Preparation",
                    description = "DRY RUN: Would prepare JAR, sources, and javadoc artifacts",
                    isSimulated = true,
                    duration = 200L,
                )
            )
        }

        // Step 3: POM Generation
        totalDuration += measureTimeMillis {
            simulatedSteps.add(
                SimulatedStep(
                    stepName = "POM Generation",
                    description = "DRY RUN: Would generate POM file with project metadata",
                    isSimulated = true,
                    duration = 100L,
                )
            )
        }

        // Step 4: GPG Signing Simulation
        if (config.signing.keyId.isNotEmpty()) {
            totalDuration += measureTimeMillis {
                simulatedSteps.add(
                    SimulatedStep(
                        stepName = "GPG Signing Simulation",
                        description =
                            "DRY RUN: Would sign artifacts with GPG key ${config.signing.keyId}",
                        isSimulated = true,
                        duration = 300L,
                    )
                )
            }
        }

        // Step 5: Upload Strategy Simulation
        totalDuration += measureTimeMillis {
            if (config.publishing.aggregation) {
                simulatedSteps.add(
                    SimulatedStep(
                        stepName = "Aggregated Upload Simulation",
                        description = "DRY RUN: Would upload all artifacts as single bundle",
                        isSimulated = true,
                        duration = 500L,
                    )
                )
            } else {
                simulatedSteps.add(
                    SimulatedStep(
                        stepName = "Individual Upload Simulation",
                        description = "DRY RUN: Would upload each artifact individually",
                        isSimulated = true,
                        duration = 800L,
                    )
                )
            }
        }

        // Step 6: Publishing Decision Simulation
        totalDuration += measureTimeMillis {
            if (config.publishing.autoPublish) {
                simulatedSteps.add(
                    SimulatedStep(
                        stepName = "Auto-Publish Simulation",
                        description = "DRY RUN: Would automatically publish to Maven Central",
                        isSimulated = true,
                        duration = 1000L,
                    )
                )
            } else {
                simulatedSteps.add(
                    SimulatedStep(
                        stepName = "Manual Review Required",
                        description =
                            "DRY RUN: Would stage artifacts for manual review and publishing",
                        isSimulated = true,
                        duration = 100L,
                    )
                )
            }
        }

        // Final Publishing Simulation
        totalDuration += measureTimeMillis {
            simulatedSteps.add(
                SimulatedStep(
                    stepName = "Publishing Simulation",
                    description = "DRY RUN: Would complete publishing process to Maven Central",
                    isSimulated = true,
                    duration = 200L,
                )
            )
        }

        val isSuccess = validationErrors.isEmpty()

        return DryRunResult(
            isSuccess = isSuccess,
            isDryRun = effectiveDryRun,
            wouldPublish = isSuccess,
            validationErrors = validationErrors,
            simulatedSteps = simulatedSteps,
            totalSteps = simulatedSteps.size,
            totalDuration = totalDuration,
        )
    }
}

/** Result of a dry run simulation containing all details about what would happen. */
data class DryRunResult(
    val isSuccess: Boolean,
    val isDryRun: Boolean,
    val wouldPublish: Boolean,
    val validationErrors: List<String>,
    val simulatedSteps: List<SimulatedStep>,
    val totalSteps: Int,
    val totalDuration: Long,
)

/** Represents a single simulated step in the publishing process. */
data class SimulatedStep(
    val stepName: String,
    val description: String,
    val isSimulated: Boolean,
    val duration: Long,
)
