package com.tddworks.sonatype.publish.portal.plugin.modules

import com.tddworks.sonatype.publish.portal.plugin.autodetection.Confidence
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Tests for ModuleStructureDetector that identifies multi-module Gradle project structures
 * and provides configuration inheritance capabilities.
 */
class ModuleStructureDetectorTest {

    private lateinit var detector: ModuleStructureDetector
    private lateinit var rootProject: Project

    @TempDir
    lateinit var tempDir: File

    @BeforeEach
    fun setup() {
        detector = ModuleStructureDetector()
        rootProject = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()
    }

    @Test
    fun `should detect single module project`() {
        // Given - Single module project with just build.gradle.kts
        File(tempDir, "build.gradle.kts").writeText("""
            plugins {
                kotlin("jvm")
            }
        """.trimIndent())

        // When
        val result = detector.detect(rootProject)
        val moduleStructure = detector.analyzeModuleStructure(rootProject.projectDir)

        // Then
        assertThat(result).isNotNull()
        assertThat(moduleStructure.isMultiModule).isFalse()
        assertThat(moduleStructure.rootModule.path).isEqualTo(":")
        assertThat(moduleStructure.subModules).isEmpty()
    }

    @Test
    fun `should detect multi-module project with settings gradle`() {
        // Given - Multi-module project structure
        File(tempDir, "settings.gradle.kts").writeText("""
            rootProject.name = "my-project"
            include("core")
            include("api")
            include("cli")
        """.trimIndent())
        
        // Create module directories
        listOf("core", "api", "cli").forEach { moduleName ->
            val moduleDir = File(tempDir, moduleName)
            moduleDir.mkdirs()
            File(moduleDir, "build.gradle.kts").writeText("""
                plugins {
                    kotlin("jvm")
                }
            """.trimIndent())
        }

        // When
        val result = detector.detect(rootProject)
        val moduleStructure = detector.analyzeModuleStructure(rootProject.projectDir)

        // Then
        assertThat(result).isNotNull()
        assertThat(moduleStructure.isMultiModule).isTrue()
        assertThat(moduleStructure.subModules).hasSize(3)
        assertThat(moduleStructure.subModules.map { it.name }).containsExactlyInAnyOrder("core", "api", "cli")
        
        // Verify detected values
        assertThat(result!!.detectedValues).containsKey("modules.structure")
        assertThat(result.detectedValues["modules.structure"]?.confidence).isEqualTo(Confidence.HIGH)
    }

    @Test
    fun `should handle nested module structures`() {
        // Given - Nested module structure
        File(tempDir, "settings.gradle.kts").writeText("""
            rootProject.name = "complex-project"
            include("backend:core")
            include("backend:api")
            include("frontend:web")
            include("shared:common")
        """.trimIndent())

        // Create nested directories
        val modules = mapOf(
            "backend/core" to "backend:core",
            "backend/api" to "backend:api", 
            "frontend/web" to "frontend:web",
            "shared/common" to "shared:common"
        )
        
        modules.forEach { (path, _) ->
            val moduleDir = File(tempDir, path)
            moduleDir.mkdirs()
            File(moduleDir, "build.gradle.kts").writeText("plugins { kotlin(\"jvm\") }")
        }

        // When
        val result = detector.detect(rootProject)
        val moduleStructure = detector.analyzeModuleStructure(rootProject.projectDir)

        // Then
        assertThat(result).isNotNull()
        assertThat(moduleStructure.isMultiModule).isTrue()
        assertThat(moduleStructure.subModules).hasSize(4)
        assertThat(moduleStructure.subModules.map { it.path }).containsExactlyInAnyOrder(
            ":backend:core", ":backend:api", ":frontend:web", ":shared:common"
        )
    }

    @Test
    fun `should detect module dependencies`() {
        // Given - Multi-module project with dependencies
        File(tempDir, "settings.gradle.kts").writeText("""
            include("core", "api")
        """.trimIndent())

        File(File(tempDir, "core").apply { mkdirs() }, "build.gradle.kts").writeText("""
            plugins { kotlin("jvm") }
        """.trimIndent())

        File(File(tempDir, "api").apply { mkdirs() }, "build.gradle.kts").writeText("""
            plugins { kotlin("jvm") }
            dependencies {
                implementation(project(":core"))
            }
        """.trimIndent())

        // When
        val result = detector.detect(rootProject)
        val moduleStructure = detector.analyzeModuleStructure(rootProject.projectDir)

        // Then
        assertThat(result).isNotNull()
        val apiModule = moduleStructure.subModules.find { it.name == "api" }
        assertThat(apiModule).isNotNull()
        assertThat(apiModule!!.dependencies).containsExactly(":core")
    }

    @Test
    fun `should provide module configuration inheritance capabilities`() {
        // Given - Multi-module setup
        File(tempDir, "settings.gradle.kts").writeText("include(\"lib1\", \"lib2\")")
        listOf("lib1", "lib2").forEach { module ->
            File(tempDir, module).mkdirs()
            File(File(tempDir, module), "build.gradle.kts").writeText("plugins { kotlin(\"jvm\") }")
        }

        // When
        val result = detector.detect(rootProject)
        val moduleStructure = detector.analyzeModuleStructure(rootProject.projectDir)

        // Then
        assertThat(result).isNotNull()
        assertThat(moduleStructure.canInheritConfiguration).isTrue()
        assertThat(moduleStructure.inheritanceStrategy).isEqualTo(ModuleInheritanceStrategy.ROOT_OVERRIDES)
    }

    @Test
    fun `should handle settings gradle groovy format`() {
        // Given - Groovy settings.gradle
        File(tempDir, "settings.gradle").writeText("""
            rootProject.name = 'groovy-project'
            include 'module-a'
            include 'module-b'
        """.trimIndent())

        listOf("module-a", "module-b").forEach { module ->
            File(tempDir, module).mkdirs()
            File(File(tempDir, module), "build.gradle").writeText("apply plugin: 'java'")
        }

        // When
        val result = detector.detect(rootProject)
        val moduleStructure = detector.analyzeModuleStructure(rootProject.projectDir)

        // Then
        assertThat(result).isNotNull()
        assertThat(moduleStructure.isMultiModule).isTrue()
        assertThat(moduleStructure.subModules).hasSize(2)
        assertThat(moduleStructure.subModules.map { it.name }).containsExactlyInAnyOrder("module-a", "module-b")
    }

    @Test
    fun `should return null for projects without build files`() {
        // Given - Empty directory with no build files
        // (tempDir is empty)

        // When
        val result = detector.detect(rootProject)

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `should detect publishing modules only`() {
        // Given - Mixed project with publishable and non-publishable modules
        File(tempDir, "settings.gradle.kts").writeText("""
            include("core", "test-utils", "api")
        """.trimIndent())

        // Core module - publishable
        val coreDir = File(tempDir, "core").apply { mkdirs() }
        File(coreDir, "build.gradle.kts").writeText("""
            plugins {
                kotlin("jvm")
                `maven-publish`
            }
        """.trimIndent())

        // Test utils - not publishable (no maven-publish)
        val testUtilsDir = File(tempDir, "test-utils").apply { mkdirs() }
        File(testUtilsDir, "build.gradle.kts").writeText("""
            plugins {
                kotlin("jvm")
            }
        """.trimIndent())

        // API module - publishable
        val apiDir = File(tempDir, "api").apply { mkdirs() }
        File(apiDir, "build.gradle.kts").writeText("""
            plugins {
                kotlin("jvm")
                `maven-publish`
            }
        """.trimIndent())

        // When
        val result = detector.detect(rootProject)
        val moduleStructure = detector.analyzeModuleStructure(rootProject.projectDir)

        // Then
        assertThat(result).isNotNull()
        assertThat(moduleStructure.publishableModules).hasSize(2)
        assertThat(moduleStructure.publishableModules.map { it.name }).containsExactlyInAnyOrder("core", "api")
        assertThat(moduleStructure.nonPublishableModules).hasSize(1)
        assertThat(moduleStructure.nonPublishableModules.map { it.name }).containsExactly("test-utils")
    }
}