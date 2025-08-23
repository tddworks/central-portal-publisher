package com.tddworks.sonatype.publish.portal.plugin.modules

import com.tddworks.sonatype.publish.portal.plugin.autodetection.AutoDetector
import com.tddworks.sonatype.publish.portal.plugin.autodetection.Confidence
import com.tddworks.sonatype.publish.portal.plugin.autodetection.DetectedValue
import com.tddworks.sonatype.publish.portal.plugin.autodetection.DetectionResult
import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfigBuilder
import java.io.File
import org.gradle.api.Project

/**
 * Auto-detector for multi-module Gradle project structures.
 *
 * This detector analyzes the project structure to identify:
 * - Single vs multi-module projects
 * - Module dependencies and relationships
 * - Publishable vs non-publishable modules
 * - Configuration inheritance opportunities
 */
class ModuleStructureDetector : AutoDetector {

    override val name: String = "ModuleStructureDetector"

    override fun detect(
        project: Project
    ): com.tddworks.sonatype.publish.portal.plugin.autodetection.DetectionResult? {
        val projectDir = project.projectDir

        // Check if this is a valid Gradle project (either has build file or settings file for
        // multi-module)
        if (!hasGradleBuildFile(projectDir) && !hasSettingsFile(projectDir)) {
            return null
        }

        val moduleStructure = analyzeModuleStructure(projectDir)
        val detectedValues = mutableMapOf<String, DetectedValue>()

        // Add detection metadata
        detectedValues["modules.structure"] =
            DetectedValue(
                path = "modules.structure",
                value = if (moduleStructure.isMultiModule) "multi-module" else "single-module",
                source =
                    if (moduleStructure.isMultiModule) "settings.gradle[.kts]"
                    else "build.gradle[.kts]",
                confidence = Confidence.HIGH,
            )

        if (moduleStructure.isMultiModule) {
            detectedValues["modules.count"] =
                DetectedValue(
                    path = "modules.count",
                    value = moduleStructure.subModules.size.toString(),
                    source = "settings.gradle[.kts]",
                    confidence = Confidence.HIGH,
                )

            detectedValues["modules.publishable"] =
                DetectedValue(
                    path = "modules.publishable",
                    value = moduleStructure.publishableModules.size.toString(),
                    source = "build files analysis",
                    confidence = Confidence.MEDIUM,
                )
        }

        // Create minimal config - most configuration will be handled by
        // MultiModuleConfigurationManager
        val config = CentralPublisherConfigBuilder().build()

        return com.tddworks.sonatype.publish.portal.plugin.autodetection.DetectionResult(
            config = config,
            detectedValues = detectedValues,
        )
    }

    private fun hasGradleBuildFile(projectDir: File): Boolean {
        return File(projectDir, "build.gradle.kts").exists() ||
            File(projectDir, "build.gradle").exists()
    }

    private fun hasSettingsFile(projectDir: File): Boolean {
        return File(projectDir, "settings.gradle.kts").exists() ||
            File(projectDir, "settings.gradle").exists()
    }

    fun analyzeModuleStructure(projectDir: File): ModuleStructure {
        val settingsKts = File(projectDir, "settings.gradle.kts")
        val settingsGroovy = File(projectDir, "settings.gradle")

        val settingsFile =
            when {
                settingsKts.exists() -> settingsKts
                settingsGroovy.exists() -> settingsGroovy
                else -> null
            }

        return if (settingsFile != null) {
            analyzeMultiModuleStructure(projectDir, settingsFile)
        } else {
            // Single module project
            ModuleStructure(
                isMultiModule = false,
                rootModule =
                    GradleModule(
                        name = projectDir.name,
                        path = ":",
                        directory = projectDir,
                        buildFile = findBuildFile(projectDir)!!,
                        isPublishable = isPublishableModule(projectDir),
                    ),
                subModules = emptyList(),
                canInheritConfiguration = false,
                inheritanceStrategy = ModuleInheritanceStrategy.NO_INHERITANCE,
            )
        }
    }

    private fun analyzeMultiModuleStructure(rootDir: File, settingsFile: File): ModuleStructure {
        val content = settingsFile.readText()
        val includedModules = extractIncludedModules(content)

        val subModules =
            includedModules.mapNotNull { modulePath ->
                val moduleDir = resolveModuleDirectory(rootDir, modulePath)
                val buildFile = findBuildFile(moduleDir)

                if (buildFile != null) {
                    val dependencies = extractModuleDependencies(buildFile)
                    GradleModule(
                        name = modulePath.substringAfterLast(':'),
                        path = if (modulePath.startsWith(':')) modulePath else ":$modulePath",
                        directory = moduleDir,
                        buildFile = buildFile,
                        isPublishable = isPublishableModule(moduleDir),
                        dependencies = dependencies,
                    )
                } else null
            }

        val rootBuildFile = findBuildFile(rootDir)
        val rootModule =
            GradleModule(
                name = rootDir.name,
                path = ":",
                directory = rootDir,
                buildFile = rootBuildFile,
                isPublishable = rootBuildFile?.let { isPublishableModule(rootDir) } ?: false,
            )

        return ModuleStructure(
            isMultiModule = true,
            rootModule = rootModule,
            subModules = subModules,
            canInheritConfiguration = true,
            inheritanceStrategy = ModuleInheritanceStrategy.ROOT_OVERRIDES,
        )
    }

    private fun extractIncludedModules(settingsContent: String): List<String> {
        val modules = mutableListOf<String>()

        // Handle both Kotlin DSL format: include("module") and Groovy format: include 'module'
        val kotlinIncludePattern = """include\s*\(\s*([^)]+)\s*\)""".toRegex()
        val groovyIncludePattern = """include\s+['"]([^'"]+)['"]""".toRegex()

        // Process Kotlin DSL includes with parentheses
        kotlinIncludePattern.findAll(settingsContent).forEach { match ->
            val args = match.groupValues[1]
            args.split(",").forEach { module ->
                val cleanModule =
                    module.trim().removeSurrounding("\"").removeSurrounding("'").trim()
                if (cleanModule.isNotEmpty()) {
                    modules.add(cleanModule)
                }
            }
        }

        // Process Groovy includes without parentheses
        groovyIncludePattern.findAll(settingsContent).forEach { match ->
            val moduleName = match.groupValues[1].trim()
            if (moduleName.isNotEmpty()) {
                modules.add(moduleName)
            }
        }

        return modules.distinct()
    }

    private fun resolveModuleDirectory(rootDir: File, modulePath: String): File {
        val cleanPath = modulePath.removePrefix(":")
        return if (cleanPath.contains(':')) {
            // Nested module like "backend:core" -> "backend/core"
            File(rootDir, cleanPath.replace(':', '/'))
        } else {
            // Simple module like "core" -> "core"
            File(rootDir, cleanPath)
        }
    }

    private fun findBuildFile(moduleDir: File): File? {
        return when {
            File(moduleDir, "build.gradle.kts").exists() -> File(moduleDir, "build.gradle.kts")
            File(moduleDir, "build.gradle").exists() -> File(moduleDir, "build.gradle")
            else -> null
        }
    }

    private fun isPublishableModule(moduleDir: File): Boolean {
        val buildFile = findBuildFile(moduleDir) ?: return false
        val content = buildFile.readText()

        // Check for maven-publish plugin
        return content.contains("`maven-publish`") ||
            content.contains("'maven-publish'") ||
            content.contains("\"maven-publish\"") ||
            content.contains("id 'maven-publish'") ||
            content.contains("id \"maven-publish\"")
    }

    private fun extractModuleDependencies(buildFile: File): List<String> {
        val content = buildFile.readText()
        val projectDependencyRegex = """project\s*\(\s*["']([^"']+)["']\s*\)""".toRegex()

        return projectDependencyRegex.findAll(content).map { it.groupValues[1] }.toList()
    }
}

/** Represents the structure of a Gradle project's modules */
data class ModuleStructure(
    val isMultiModule: Boolean,
    val rootModule: GradleModule,
    val subModules: List<GradleModule>,
    val canInheritConfiguration: Boolean,
    val inheritanceStrategy: ModuleInheritanceStrategy,
) {
    val publishableModules: List<GradleModule>
        get() = (listOf(rootModule) + subModules).filter { it.isPublishable }

    val nonPublishableModules: List<GradleModule>
        get() =
            if (isMultiModule) {
                // For multi-module projects, only consider submodules for non-publishable filtering
                subModules.filter { !it.isPublishable }
            } else {
                // For single module projects, include the root
                (listOf(rootModule) + subModules).filter { !it.isPublishable }
            }

    val allModules: List<GradleModule>
        get() = listOf(rootModule) + subModules
}

/** Represents a single Gradle module */
data class GradleModule(
    val name: String,
    val path: String, // Gradle path like ":core" or ":backend:api"
    val directory: File,
    val buildFile: File?,
    val isPublishable: Boolean,
    val dependencies: List<String> = emptyList(), // Gradle paths of dependent modules
)

/** Strategy for configuration inheritance in multi-module projects */
enum class ModuleInheritanceStrategy {
    /** No inheritance - each module configured independently */
    NO_INHERITANCE,
    /** Root configuration provides defaults, modules can override */
    ROOT_OVERRIDES,
    /** Modules inherit all root configuration unless explicitly overridden */
    ROOT_DEFAULTS,
    /** Custom inheritance rules defined by user */
    CUSTOM,
}

/** Extended DetectionResult that includes module structure information */
data class DetectionResult(
    val config: com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig,
    val detectedValues: Map<String, DetectedValue>,
    val warnings: List<String> = emptyList(),
    val moduleStructure: ModuleStructure? = null,
) {
    // Convert to standard DetectionResult for compatibility
    fun toStandardDetectionResult():
        com.tddworks.sonatype.publish.portal.plugin.autodetection.DetectionResult {
        return com.tddworks.sonatype.publish.portal.plugin.autodetection.DetectionResult(
            config = config,
            detectedValues = detectedValues,
            warnings = warnings,
        )
    }
}
