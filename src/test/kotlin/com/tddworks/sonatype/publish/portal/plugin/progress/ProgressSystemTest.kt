package com.tddworks.sonatype.publish.portal.plugin.progress

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.logging.Logger
import org.mockito.kotlin.*
import java.io.PrintWriter
import java.io.StringWriter

class ProgressSystemTest {

    private lateinit var mockLogger: Logger
    private lateinit var output: StringWriter
    private lateinit var progressSystem: ProgressSystem

    @BeforeEach
    fun setup() {
        mockLogger = mock()
        output = StringWriter()
        progressSystem = ProgressSystem(mockLogger, PrintWriter(output))
    }

    @Test
    fun `should create progress tracker with steps`() {
        // Given/When
        val tracker = progressSystem.createTracker(
            taskName = "publishToCentral",
            steps = listOf(
                "Validating configuration",
                "Creating deployment bundle", 
                "Uploading to Sonatype",
                "Finalizing publication"
            )
        )

        // Then
        assertThat(tracker.totalSteps).isEqualTo(4)
        assertThat(tracker.currentStep).isEqualTo(0)
        assertThat(tracker.taskName).isEqualTo("publishToCentral")
        assertThat(tracker.isCompleted).isFalse
    }

    @Test
    fun `should update progress and show percentage`() {
        // Given
        val tracker = progressSystem.createTracker(
            taskName = "publishToCentral",
            steps = listOf("Step 1", "Step 2", "Step 3", "Step 4")
        )

        // When
        tracker.nextStep("Working on step 1...")
        tracker.nextStep("Working on step 2...")

        // Then
        assertThat(tracker.currentStep).isEqualTo(2)
        assertThat(tracker.progressPercentage).isEqualTo(50) // 2/4 = 50%
        assertThat(tracker.currentStepName).isEqualTo("Step 2")
    }

    @Test
    fun `should show progress bar in console output`() {
        // Given
        val tracker = progressSystem.createTracker(
            taskName = "publishToCentral",
            steps = listOf("Step 1", "Step 2", "Step 3", "Step 4")
        )

        // When
        tracker.nextStep("Processing...")

        // Then
        verify(mockLogger).lifecycle(argThat { this.contains("publishToCentral") })
        verify(mockLogger).lifecycle(argThat { this.contains("25%") }) // 1/4 = 25%
        verify(mockLogger).lifecycle(argThat { this.contains("█") }) // Should contain progress bar characters
    }

    @Test
    fun `should estimate time remaining`() {
        // Given
        val tracker = progressSystem.createTracker(
            taskName = "upload",
            steps = listOf("Step 1", "Step 2", "Step 3", "Step 4")
        )
        
        // When
        Thread.sleep(100) // Simulate some work
        tracker.nextStep("First step done")
        Thread.sleep(100)
        tracker.nextStep("Second step done")

        // Then
        assertThat(tracker.estimatedTimeRemaining).isGreaterThan(0)
        assertThat(tracker.estimatedTimeRemaining).isLessThan(1000) // Should be reasonable
    }

    @Test
    fun `should handle completion properly`() {
        // Given
        val tracker = progressSystem.createTracker(
            taskName = "test",
            steps = listOf("Step 1", "Step 2")
        )

        // When
        tracker.nextStep("Step 1 done")
        tracker.nextStep("Step 2 done") 
        tracker.complete("All done!")

        // Then
        assertThat(tracker.isCompleted).isTrue
        assertThat(tracker.progressPercentage).isEqualTo(100)
        verify(mockLogger).lifecycle(argThat { this.contains("✅") })
        verify(mockLogger).lifecycle(argThat { this.contains("All done!") })
    }

    @Test
    fun `should handle errors during progress`() {
        // Given
        val tracker = progressSystem.createTracker(
            taskName = "failing-task",
            steps = listOf("Step 1", "Step 2", "Step 3")
        )

        // When
        tracker.nextStep("Step 1 done")
        tracker.error("Something went wrong!", Exception("Test error"))

        // Then
        assertThat(tracker.hasFailed).isTrue
        assertThat(tracker.currentStep).isEqualTo(1) // Should stay at current step
        verify(mockLogger).error(argThat { this.contains("Something went wrong!") })
    }

    @Test
    fun `should support verbose mode with detailed output`() {
        // Given
        val verboseTracker = progressSystem.createTracker(
            taskName = "verbose-task",
            steps = listOf("Step 1", "Step 2"),
            verbose = true
        )

        // When
        verboseTracker.nextStep("Detailed step info...")

        // Then
        verify(mockLogger).info(argThat { this.contains("Detailed step info...") })
    }

    @Test
    fun `should format time estimates nicely`() {
        // Given/When/Then
        assertThat(ProgressUtils.formatDuration(500)).isEqualTo("< 1s")
        assertThat(ProgressUtils.formatDuration(1500)).isEqualTo("2s")
        assertThat(ProgressUtils.formatDuration(65000)).isEqualTo("1m 5s")
        assertThat(ProgressUtils.formatDuration(3665000)).isEqualTo("1h 1m 5s")
    }

    @Test
    fun `should create different types of progress bars`() {
        // Given/When
        val simpleBar = ProgressUtils.createProgressBar(50, 100, ProgressBarStyle.SIMPLE)
        val blockBar = ProgressUtils.createProgressBar(75, 100, ProgressBarStyle.BLOCKS)
        val dotsBar = ProgressUtils.createProgressBar(25, 100, ProgressBarStyle.DOTS)

        // Then
        assertThat(simpleBar).contains("=")
        assertThat(blockBar).contains("█")
        assertThat(dotsBar).contains("●")
        
        // All should show percentage
        assertThat(simpleBar).contains("50%")
        assertThat(blockBar).contains("75%") 
        assertThat(dotsBar).contains("25%")
    }

    @Test
    fun `should track file upload progress`() {
        // Given
        val uploadTracker = progressSystem.createFileUploadTracker(
            fileName = "deployment-bundle.zip",
            totalBytes = 1000000L // 1MB
        )

        // When
        uploadTracker.updateProgress(250000L) // 25% uploaded
        uploadTracker.updateProgress(500000L) // 50% uploaded

        // Then
        assertThat(uploadTracker.progressPercentage).isEqualTo(50)
        assertThat(uploadTracker.uploadSpeed).isGreaterThanOrEqualTo(0) // Speed can be 0 for very fast operations
        verify(mockLogger, atLeast(1)).lifecycle(argThat { this.contains("deployment-bundle.zip") })
        verify(mockLogger, atLeast(1)).lifecycle(argThat { this.contains("50%") })
    }

    @Test
    fun `should handle zero-length files gracefully`() {
        // Given
        val uploadTracker = progressSystem.createFileUploadTracker(
            fileName = "empty.zip", 
            totalBytes = 0L
        )

        // When
        uploadTracker.complete("Upload complete")

        // Then
        assertThat(uploadTracker.isCompleted).isTrue
        verify(mockLogger).lifecycle(argThat { this.contains("✅") })
    }
}