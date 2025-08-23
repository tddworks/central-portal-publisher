package com.tddworks.sonatype.publish.portal.plugin.autodetection

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import org.gradle.api.Project

/** Interface for auto-detecting configuration values from project files, git repository, etc. */
interface AutoDetector {
    /** The name of this detector for diagnostics */
    val name: String

    /** Whether this detector is enabled by default */
    val enabledByDefault: Boolean
        get() = true

    /**
     * Detect configuration values and return a partial configuration. Returns null if this detector
     * cannot provide any useful information.
     */
    fun detect(project: Project): DetectionResult?
}

/** Result of auto-detection with metadata about what was detected */
data class DetectionResult(
    val config: CentralPublisherConfig,
    val detectedValues: Map<String, DetectedValue>,
    val warnings: List<String> = emptyList(),
)

/** Information about a single detected value */
data class DetectedValue(
    // e.g. "projectInfo.name"
    val path: String,
    // The detected value
    val value: String,
    // e.g. "build.gradle.kts", ".git/config"
    val source: String,
    val confidence: Confidence,
)

/** Confidence level of auto-detection */
enum class Confidence {
    /** High confidence - almost certainly correct (e.g., from build.gradle.kts) */
    HIGH,
    /**
     * Medium confidence - likely correct but might need verification (e.g., inferred from directory
     * name)
     */
    MEDIUM,
    /** Low confidence - best guess, user should verify (e.g., guessed from file patterns) */
    LOW,
}

/** Manager for running multiple auto-detectors */
class AutoDetectionManager(private val detectors: List<AutoDetector>) {

    /** Run all enabled detectors and merge their results */
    fun detectConfiguration(project: Project): AutoDetectionSummary {
        val results = mutableListOf<DetectionResult>()
        val allDetectedValues = mutableMapOf<String, DetectedValue>()
        val allWarnings = mutableListOf<String>()

        detectors
            .filter { it.enabledByDefault }
            .forEach { detector ->
                try {
                    detector.detect(project)?.let { result ->
                        results.add(result)

                        // Merge detected values, keeping higher confidence values
                        result.detectedValues.forEach { (path, detectedValue) ->
                            val existing = allDetectedValues[path]
                            if (
                                existing == null ||
                                    detectedValue.confidence.ordinal < existing.confidence.ordinal
                            ) {
                                allDetectedValues[path] = detectedValue
                            }
                        }

                        allWarnings.addAll(result.warnings)
                    }
                } catch (e: Exception) {
                    allWarnings.add("Detector '${detector.name}' failed: ${e.message}")
                }
            }

        // Merge all configurations
        val finalConfig =
            results.fold(
                com.tddworks.sonatype.publish.portal.plugin.config
                    .CentralPublisherConfigBuilder()
                    .build()
            ) { acc, result ->
                acc.mergeWith(result.config)
            }

        return AutoDetectionSummary(
            config = finalConfig,
            detectedValues = allDetectedValues,
            warnings = allWarnings,
            detectorsRun = detectors.filter { it.enabledByDefault }.map { it.name },
        )
    }
}

/** Summary of all auto-detection results */
data class AutoDetectionSummary(
    val config: CentralPublisherConfig,
    val detectedValues: Map<String, DetectedValue>,
    val warnings: List<String>,
    val detectorsRun: List<String>,
) {
    val hasDetectedValues: Boolean
        get() = detectedValues.isNotEmpty()

    val hasWarnings: Boolean
        get() = warnings.isNotEmpty()

    fun getDetectedValuesByConfidence(confidence: Confidence): Map<String, DetectedValue> {
        return detectedValues.filterValues { it.confidence == confidence }
    }
}
