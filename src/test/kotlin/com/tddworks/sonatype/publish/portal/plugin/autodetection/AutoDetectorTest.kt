package com.tddworks.sonatype.publish.portal.plugin.autodetection

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfigBuilder
import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class AutoDetectorTest {

    private lateinit var project: Project

    @TempDir lateinit var tempDir: File

    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder().withProjectDir(tempDir).build()
    }

    @Test
    fun `should create detection result with detected values`() {
        // Given
        val config =
            CentralPublisherConfigBuilder()
                .projectInfo {
                    name = "test-project"
                    description = "A test project"
                }
                .build()

        val detectedValues =
            mapOf(
                "projectInfo.name" to
                    DetectedValue(
                        path = "projectInfo.name",
                        value = "test-project",
                        source = "build.gradle.kts",
                        confidence = Confidence.HIGH,
                    ),
                "projectInfo.description" to
                    DetectedValue(
                        path = "projectInfo.description",
                        value = "A test project",
                        source = "build.gradle.kts",
                        confidence = Confidence.HIGH,
                    ),
            )

        // When
        val result =
            DetectionResult(
                config = config,
                detectedValues = detectedValues,
                warnings = listOf("Warning: No LICENSE file found"),
            )

        // Then
        assertThat(result.config.projectInfo.name).isEqualTo("test-project")
        assertThat(result.config.projectInfo.description).isEqualTo("A test project")
        assertThat(result.detectedValues).hasSize(2)
        assertThat(result.warnings).containsExactly("Warning: No LICENSE file found")
    }

    @Test
    fun `should handle confidence levels correctly`() {
        // Given/When/Then
        assertThat(Confidence.HIGH.ordinal).isLessThan(Confidence.MEDIUM.ordinal)
        assertThat(Confidence.MEDIUM.ordinal).isLessThan(Confidence.LOW.ordinal)
    }

    @Test
    fun `auto detection manager should merge multiple detector results`() {
        // Given
        val detector1 =
            MockDetector(
                "detector1",
                CentralPublisherConfigBuilder().projectInfo { name = "detected-name" }.build(),
                mapOf(
                    "projectInfo.name" to
                        DetectedValue(
                            "projectInfo.name",
                            "detected-name",
                            "build.gradle.kts",
                            Confidence.HIGH,
                        )
                ),
            )

        val detector2 =
            MockDetector(
                "detector2",
                CentralPublisherConfigBuilder()
                    .projectInfo { description = "detected-description" }
                    .build(),
                mapOf(
                    "projectInfo.description" to
                        DetectedValue(
                            "projectInfo.description",
                            "detected-description",
                            "README.md",
                            Confidence.MEDIUM,
                        )
                ),
            )

        val manager = AutoDetectionManager(listOf(detector1, detector2))

        // When
        val summary = manager.detectConfiguration(project)

        // Then
        assertThat(summary.config.projectInfo.name).isEqualTo("detected-name")
        assertThat(summary.config.projectInfo.description).isEqualTo("detected-description")
        assertThat(summary.detectedValues).hasSize(2)
        assertThat(summary.detectorsRun).containsExactlyInAnyOrder("detector1", "detector2")
        assertThat(summary.hasDetectedValues).isTrue()
    }

    @Test
    fun `auto detection manager should prioritize higher confidence values`() {
        // Given - two detectors providing same value with different confidence
        val lowConfidenceDetector =
            MockDetector(
                "low-detector",
                CentralPublisherConfigBuilder()
                    .projectInfo { name = "low-confidence-name" }
                    .build(),
                mapOf(
                    "projectInfo.name" to
                        DetectedValue(
                            "projectInfo.name",
                            "low-confidence-name",
                            "directory-name",
                            Confidence.LOW,
                        )
                ),
            )

        val highConfidenceDetector =
            MockDetector(
                "high-detector",
                CentralPublisherConfigBuilder()
                    .projectInfo { name = "high-confidence-name" }
                    .build(),
                mapOf(
                    "projectInfo.name" to
                        DetectedValue(
                            "projectInfo.name",
                            "high-confidence-name",
                            "build.gradle.kts",
                            Confidence.HIGH,
                        )
                ),
            )

        val manager = AutoDetectionManager(listOf(lowConfidenceDetector, highConfidenceDetector))

        // When
        val summary = manager.detectConfiguration(project)

        // Then
        assertThat(summary.detectedValues["projectInfo.name"]?.value)
            .isEqualTo("high-confidence-name")
        assertThat(summary.detectedValues["projectInfo.name"]?.confidence)
            .isEqualTo(Confidence.HIGH)
    }

    @Test
    fun `auto detection manager should handle detector failures gracefully`() {
        // Given
        val failingDetector =
            object : AutoDetector {
                override val name = "failing-detector"

                override fun detect(project: Project): DetectionResult? {
                    throw RuntimeException("Simulated detector failure")
                }
            }

        val workingDetector =
            MockDetector(
                "working-detector",
                CentralPublisherConfigBuilder().projectInfo { name = "working" }.build(),
                mapOf(
                    "projectInfo.name" to
                        DetectedValue("projectInfo.name", "working", "test", Confidence.HIGH)
                ),
            )

        val manager = AutoDetectionManager(listOf(failingDetector, workingDetector))

        // When
        val summary = manager.detectConfiguration(project)

        // Then
        assertThat(summary.config.projectInfo.name).isEqualTo("working")
        assertThat(summary.warnings)
            .contains("Detector 'failing-detector' failed: Simulated detector failure")
        assertThat(summary.hasWarnings).isTrue()
    }

    @Test
    fun `auto detection summary should filter by confidence level`() {
        // Given
        val detectedValues =
            mapOf(
                "high1" to DetectedValue("high1", "value1", "source", Confidence.HIGH),
                "high2" to DetectedValue("high2", "value2", "source", Confidence.HIGH),
                "medium1" to DetectedValue("medium1", "value3", "source", Confidence.MEDIUM),
                "low1" to DetectedValue("low1", "value4", "source", Confidence.LOW),
            )

        val summary =
            AutoDetectionSummary(
                config = CentralPublisherConfigBuilder().build(),
                detectedValues = detectedValues,
                warnings = emptyList(),
                detectorsRun = emptyList(),
            )

        // When/Then
        assertThat(summary.getDetectedValuesByConfidence(Confidence.HIGH)).hasSize(2)
        assertThat(summary.getDetectedValuesByConfidence(Confidence.MEDIUM)).hasSize(1)
        assertThat(summary.getDetectedValuesByConfidence(Confidence.LOW)).hasSize(1)
    }
}

/** Mock detector for testing */
private class MockDetector(
    override val name: String,
    private val config: CentralPublisherConfig,
    private val detectedValues: Map<String, DetectedValue>,
    private val warnings: List<String> = emptyList(),
) : AutoDetector {

    override fun detect(project: Project): DetectionResult {
        return DetectionResult(config, detectedValues, warnings)
    }
}
