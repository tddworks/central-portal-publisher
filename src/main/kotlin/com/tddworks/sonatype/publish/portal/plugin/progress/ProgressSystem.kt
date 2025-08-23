package com.tddworks.sonatype.publish.portal.plugin.progress

import java.io.PrintWriter
import kotlin.math.min
import org.gradle.api.logging.Logger

/**
 * Progress tracking system for long-running publishing operations. Provides visual progress bars,
 * time estimates, and detailed feedback.
 */
class ProgressSystem(
    private val logger: Logger,
    private val output: PrintWriter = PrintWriter(System.out),
) {

    fun createTracker(
        taskName: String,
        steps: List<String>,
        verbose: Boolean = false,
    ): ProgressTracker {
        return ProgressTracker(
            taskName = taskName,
            steps = steps,
            logger = logger,
            output = output,
            verbose = verbose,
        )
    }

    fun createFileUploadTracker(fileName: String, totalBytes: Long): FileUploadTracker {
        return FileUploadTracker(
            fileName = fileName,
            totalBytes = totalBytes,
            logger = logger,
            output = output,
        )
    }
}

/** Tracks progress through a series of named steps. */
class ProgressTracker(
    val taskName: String,
    private val steps: List<String>,
    private val logger: Logger,
    private val output: PrintWriter,
    private val verbose: Boolean = false,
) {
    private var _currentStep = 0
    private var _startTime = System.currentTimeMillis()
    private var _stepStartTime = System.currentTimeMillis()
    private var _isCompleted = false
    private var _hasFailed = false

    val totalSteps: Int = steps.size
    val currentStep: Int
        get() = _currentStep

    val isCompleted: Boolean
        get() = _isCompleted

    val hasFailed: Boolean
        get() = _hasFailed

    val progressPercentage: Int
        get() = if (totalSteps == 0) 100 else (currentStep * 100) / totalSteps

    val currentStepName: String
        get() = if (currentStep > 0 && currentStep <= steps.size) steps[currentStep - 1] else ""

    val estimatedTimeRemaining: Long
        get() {
            if (currentStep == 0) return 0
            val elapsedTime = System.currentTimeMillis() - _startTime
            val avgTimePerStep = elapsedTime / currentStep
            val remainingSteps = totalSteps - currentStep
            return avgTimePerStep * remainingSteps
        }

    fun nextStep(message: String = "") {
        if (_isCompleted || _hasFailed) return

        _currentStep++
        _stepStartTime = System.currentTimeMillis()

        val stepName = if (currentStep <= steps.size) steps[currentStep - 1] else "Final step"
        val progress =
            ProgressUtils.createProgressBar(currentStep, totalSteps, ProgressBarStyle.BLOCKS)

        logger.lifecycle("[$taskName] $progress")
        logger.lifecycle("  Step $currentStep/$totalSteps: $stepName")

        if (message.isNotBlank()) {
            if (verbose) {
                logger.info("    Details: $message")
            } else {
                logger.lifecycle("    $message")
            }
        }

        if (currentStep > 1) {
            val eta = estimatedTimeRemaining
            if (eta > 0) {
                logger.lifecycle("    ETA: ${ProgressUtils.formatDuration(eta)}")
            }
        }

        output.flush()
    }

    fun complete(message: String = "Task completed successfully") {
        if (_hasFailed) return

        _isCompleted = true
        _currentStep = totalSteps

        val totalTime = System.currentTimeMillis() - _startTime
        val progressBar =
            ProgressUtils.createProgressBar(totalSteps, totalSteps, ProgressBarStyle.BLOCKS)

        logger.lifecycle("[$taskName] $progressBar")
        logger.lifecycle("  ✅ $message")
        logger.lifecycle("  Total time: ${ProgressUtils.formatDuration(totalTime)}")

        output.flush()
    }

    fun error(message: String, exception: Throwable? = null) {
        _hasFailed = true

        logger.error("[$taskName] ❌ $message")
        exception?.let {
            logger.error("  Error details: ${it.message}")
            if (verbose) {
                logger.error("  Stack trace:", it)
            }
        }

        output.flush()
    }

    fun updateCurrentStep(message: String) {
        if (_isCompleted || _hasFailed) return

        if (verbose) {
            logger.info("    $message")
        } else {
            logger.lifecycle("    $message")
        }

        output.flush()
    }
}

/** Specialized tracker for file upload operations with byte-level progress. */
class FileUploadTracker(
    val fileName: String,
    val totalBytes: Long,
    private val logger: Logger,
    private val output: PrintWriter,
) {
    private var _uploadedBytes = 0L
    private var _startTime = System.currentTimeMillis()
    private var _lastUpdateTime = System.currentTimeMillis()
    private var _isCompleted = false

    val uploadedBytes: Long
        get() = _uploadedBytes

    val isCompleted: Boolean
        get() = _isCompleted

    val progressPercentage: Int
        get() = if (totalBytes == 0L) 100 else ((_uploadedBytes * 100) / totalBytes).toInt()

    val uploadSpeed: Long // bytes per second
        get() {
            val elapsedSeconds = (System.currentTimeMillis() - _startTime) / 1000.0
            return if (elapsedSeconds > 0) (_uploadedBytes / elapsedSeconds).toLong() else 0L
        }

    fun updateProgress(bytesUploaded: Long) {
        if (_isCompleted) return

        _uploadedBytes = min(bytesUploaded, totalBytes)
        _lastUpdateTime = System.currentTimeMillis()

        val progress =
            ProgressUtils.createProgressBar(progressPercentage, 100, ProgressBarStyle.BLOCKS)
        val speed = ProgressUtils.formatBytes(uploadSpeed) + "/s"
        val uploaded = ProgressUtils.formatBytes(_uploadedBytes)
        val total = ProgressUtils.formatBytes(totalBytes)

        logger.lifecycle("Uploading $fileName: $progress")
        logger.lifecycle("  $uploaded / $total ($speed)")

        output.flush()
    }

    fun complete(message: String = "Upload completed") {
        _isCompleted = true
        _uploadedBytes = totalBytes

        val totalTime = System.currentTimeMillis() - _startTime
        val avgSpeed =
            if (totalTime > 0) ProgressUtils.formatBytes((totalBytes * 1000) / totalTime) + "/s"
            else "N/A"

        logger.lifecycle("✅ $fileName: $message")
        logger.lifecycle(
            "  ${ProgressUtils.formatBytes(totalBytes)} in ${ProgressUtils.formatDuration(totalTime)} (avg $avgSpeed)"
        )

        output.flush()
    }
}

/** Utility functions for progress display and formatting. */
object ProgressUtils {

    fun createProgressBar(
        current: Int,
        total: Int,
        style: ProgressBarStyle = ProgressBarStyle.BLOCKS,
    ): String {
        if (total == 0) return "100%"

        val percentage = (current * 100) / total
        val width = 20
        val filled = (current * width) / total

        val bar =
            when (style) {
                ProgressBarStyle.SIMPLE -> {
                    "=" * filled + "-" * (width - filled)
                }
                ProgressBarStyle.BLOCKS -> {
                    "█" * filled + "░" * (width - filled)
                }
                ProgressBarStyle.DOTS -> {
                    "●" * filled + "○" * (width - filled)
                }
            }

        return "[$bar] $percentage%"
    }

    fun formatDuration(milliseconds: Long): String {
        if (milliseconds < 1000) return "< 1s"

        // Round up to nearest second
        val seconds = (milliseconds + 999) / 1000 // This rounds up
        val minutes = seconds / 60
        val hours = minutes / 60

        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m ${seconds % 60}s"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "${bytes}B"

        val kb = bytes / 1024
        if (kb < 1024) return "${kb}KB"

        val mb = kb / 1024
        if (mb < 1024) return "${mb}MB"

        val gb = mb / 1024
        return "${gb}GB"
    }

    private operator fun String.times(count: Int): String {
        return this.repeat(count)
    }
}

enum class ProgressBarStyle {
    SIMPLE, // [====----] 50%
    BLOCKS, // [████░░░░] 50%
    DOTS, // [●●●●○○○○] 50%
}
