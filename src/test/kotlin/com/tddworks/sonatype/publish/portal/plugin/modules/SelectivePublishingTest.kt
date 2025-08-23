package com.tddworks.sonatype.publish.portal.plugin.modules

import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Tests for Selective Publishing functionality that allows fine-grained control over which modules
 * get published in multi-module projects.
 *
 * This includes include/exclude patterns, module filters, selection DSL, module groups, and
 * validation.
 */
class SelectivePublishingTest {

    private lateinit var selectivePublisher: SelectivePublisher
    private lateinit var moduleStructureDetector: ModuleStructureDetector
    private lateinit var rootProject: Project

    @TempDir lateinit var tempDir: File

    @BeforeEach
    fun setup() {
        selectivePublisher = SelectivePublisher()
        moduleStructureDetector = ModuleStructureDetector()
        rootProject = ProjectBuilder.builder().withProjectDir(tempDir).build()
    }

    @Test
    fun `should support include patterns for module selection`() {
        // Given - Multi-module project with various modules
        createMultiModuleProject(
            modules = listOf("core", "api", "ui", "test-utils", "integration-tests")
        )

        val moduleStructure = moduleStructureDetector.analyzeModuleStructure(tempDir)
        val selectionConfig = ModuleSelectionConfig(includePatterns = listOf("core", "api", "ui"))

        // When
        val selectedModules = selectivePublisher.selectModules(moduleStructure, selectionConfig)

        // Then
        assertThat(selectedModules.map { it.name }).containsExactlyInAnyOrder("core", "api", "ui")
        assertThat(selectedModules).allSatisfy { module ->
            assertThat(module.isPublishable).isTrue()
        }
    }

    @Test
    fun `should support exclude patterns for module selection`() {
        // Given - Multi-module project
        createMultiModuleProject(
            modules = listOf("core", "api", "ui", "test-utils", "benchmarks"),
            publishableModules = listOf("core", "api", "ui", "test-utils", "benchmarks"),
        )

        val moduleStructure = moduleStructureDetector.analyzeModuleStructure(tempDir)
        val selectionConfig =
            ModuleSelectionConfig(excludePatterns = listOf("*test*", "benchmarks"))

        // When
        val selectedModules = selectivePublisher.selectModules(moduleStructure, selectionConfig)

        // Then
        assertThat(selectedModules.map { it.name }).containsExactlyInAnyOrder("core", "api", "ui")
    }

    @Test
    fun `should combine include and exclude patterns correctly`() {
        // Given - Multi-module project
        createMultiModuleProject(
            modules =
                listOf(
                    "frontend-core",
                    "frontend-ui",
                    "backend-core",
                    "backend-api",
                    "shared-utils",
                ),
            publishableModules =
                listOf(
                    "frontend-core",
                    "frontend-ui",
                    "backend-core",
                    "backend-api",
                    "shared-utils",
                ),
        )

        val moduleStructure = moduleStructureDetector.analyzeModuleStructure(tempDir)
        val selectionConfig =
            ModuleSelectionConfig(
                includePatterns =
                    listOf("frontend-*", "backend-*"), // Include frontend and backend modules
                excludePatterns = listOf("*-ui"), // But exclude UI modules
            )

        // When
        val selectedModules = selectivePublisher.selectModules(moduleStructure, selectionConfig)

        // Then
        assertThat(selectedModules.map { it.name })
            .containsExactlyInAnyOrder("frontend-core", "backend-core", "backend-api")
    }

    @Test
    fun `should support module filters by publishable status`() {
        // Given - Mixed publishable and non-publishable modules
        createMultiModuleProject(
            modules = listOf("core", "api", "test-utils", "docs"),
            publishableModules = listOf("core", "api"), // test-utils and docs are not publishable
        )

        val moduleStructure = moduleStructureDetector.analyzeModuleStructure(tempDir)
        val selectionConfig =
            ModuleSelectionConfig(filters = listOf(ModuleFilter.PublishableOnly(true)))

        // When
        val selectedModules = selectivePublisher.selectModules(moduleStructure, selectionConfig)

        // Then
        assertThat(selectedModules.map { it.name }).containsExactlyInAnyOrder("core", "api")
        assertThat(selectedModules).allSatisfy { module ->
            assertThat(module.isPublishable).isTrue()
        }
    }

    @Test
    fun `should support module filters by dependency presence`() {
        // Given - Modules with different dependency patterns
        createMultiModuleProjectWithDependencies()

        val moduleStructure = moduleStructureDetector.analyzeModuleStructure(tempDir)
        val selectionConfig =
            ModuleSelectionConfig(
                filters = listOf(ModuleFilter.HasDependencies(hasProjectDependencies = true))
            )

        // When
        val selectedModules = selectivePublisher.selectModules(moduleStructure, selectionConfig)

        // Then - Only modules with project dependencies are selected
        assertThat(selectedModules.map { it.name }).containsExactlyInAnyOrder("api", "cli")
        assertThat(selectedModules).allSatisfy { module ->
            assertThat(module.dependencies).isNotEmpty()
        }
    }

    @Test
    fun `should support module groups for logical organization`() {
        // Given - Multi-module project with logical groups
        createMultiModuleProject(
            modules =
                listOf(
                    "frontend-core",
                    "frontend-ui",
                    "backend-core",
                    "backend-api",
                    "shared-common",
                ),
            publishableModules =
                listOf(
                    "frontend-core",
                    "frontend-ui",
                    "backend-core",
                    "backend-api",
                    "shared-common",
                ),
        )

        val moduleStructure = moduleStructureDetector.analyzeModuleStructure(tempDir)
        val selectionConfig =
            ModuleSelectionConfig(
                groups =
                    mapOf(
                        "frontend" to listOf("frontend-core", "frontend-ui"),
                        "backend" to listOf("backend-core", "backend-api"),
                        "shared" to listOf("shared-common"),
                    ),
                selectedGroups = listOf("backend", "shared"),
            )

        // When
        val selectedModules = selectivePublisher.selectModules(moduleStructure, selectionConfig)

        // Then
        assertThat(selectedModules.map { it.name })
            .containsExactlyInAnyOrder("backend-core", "backend-api", "shared-common")
    }

    @Test
    fun `should validate module selection configuration`() {
        // Given - Module selection with invalid configuration
        createMultiModuleProject(modules = listOf("core", "api"))

        val moduleStructure = moduleStructureDetector.analyzeModuleStructure(tempDir)
        val invalidSelectionConfig =
            ModuleSelectionConfig(
                includePatterns = listOf("nonexistent-module"),
                selectedGroups = listOf("invalid-group"),
            )

        // When
        val validationResult =
            selectivePublisher.validateSelection(moduleStructure, invalidSelectionConfig)

        // Then
        assertThat(validationResult.isValid).isFalse()
        assertThat(validationResult.errors).hasSize(2)
        assertThat(validationResult.errors).anyMatch { it.contains("nonexistent-module") }
        assertThat(validationResult.errors).anyMatch { it.contains("invalid-group") }
    }

    @Test
    fun `should support complex selection with multiple criteria`() {
        // Given - Complex multi-module project
        createComplexMultiModuleProject()

        val moduleStructure = moduleStructureDetector.analyzeModuleStructure(tempDir)
        val selectionConfig =
            ModuleSelectionConfig(
                includePatterns = listOf("*core*", "*api*"),
                excludePatterns = listOf("*test*"),
                filters =
                    listOf(
                        ModuleFilter.PublishableOnly(true),
                        ModuleFilter.HasDependencies(
                            hasProjectDependencies = false
                        ), // Leaf modules only
                    ),
                groups = mapOf("core-libs" to listOf("frontend-core", "backend-core")),
                selectedGroups = listOf("core-libs"),
            )

        // When
        val selectedModules = selectivePublisher.selectModules(moduleStructure, selectionConfig)

        // Then - Only publishable core modules without dependencies
        assertThat(selectedModules.map { it.name })
            .containsExactlyInAnyOrder("frontend-core", "backend-core")
        assertThat(selectedModules).allSatisfy { module ->
            assertThat(module.isPublishable).isTrue()
            assertThat(module.name).contains("core")
            assertThat(module.dependencies).isEmpty()
        }
    }

    @Test
    fun `should handle empty module selection gracefully`() {
        // Given - Module selection that matches no modules
        createMultiModuleProject(modules = listOf("core", "api"))

        val moduleStructure = moduleStructureDetector.analyzeModuleStructure(tempDir)
        val selectionConfig = ModuleSelectionConfig(includePatterns = listOf("nonexistent-*"))

        // When
        val selectedModules = selectivePublisher.selectModules(moduleStructure, selectionConfig)

        // Then
        assertThat(selectedModules).isEmpty()
    }

    @Test
    fun `should support wildcard patterns in include and exclude`() {
        // Given - Multi-module project with various naming patterns
        createMultiModuleProject(
            modules =
                listOf(
                    "lib-core",
                    "lib-api",
                    "lib-ui",
                    "app-main",
                    "test-integration",
                    "test-unit",
                ),
            publishableModules = listOf("lib-core", "lib-api", "lib-ui", "app-main"),
        )

        val moduleStructure = moduleStructureDetector.analyzeModuleStructure(tempDir)
        val selectionConfig =
            ModuleSelectionConfig(
                includePatterns = listOf("lib-*", "app-*"),
                excludePatterns = listOf("*-ui", "test-*"),
            )

        // When
        val selectedModules = selectivePublisher.selectModules(moduleStructure, selectionConfig)

        // Then
        assertThat(selectedModules.map { it.name })
            .containsExactlyInAnyOrder("lib-core", "lib-api", "app-main")
    }

    // Helper methods for test setup

    private fun createMultiModuleProject(
        modules: List<String>,
        publishableModules: List<String> = modules,
    ) {
        // Create settings.gradle.kts
        File(tempDir, "settings.gradle.kts")
            .writeText(
                """
            rootProject.name = "test-project"
            ${modules.joinToString("\n") { "include(\"$it\")" }}
            """
                    .trimIndent()
            )

        // Create module directories and build files
        modules.forEach { moduleName ->
            val moduleDir = File(tempDir, moduleName)
            moduleDir.mkdirs()

            val isPublishable = publishableModules.contains(moduleName)
            val buildFileContent =
                if (isPublishable) {
                    """
                plugins {
                    kotlin("jvm")
                    `maven-publish`
                }
                """
                        .trimIndent()
                } else {
                    """
                plugins {
                    kotlin("jvm")
                }
                """
                        .trimIndent()
                }

            File(moduleDir, "build.gradle.kts").writeText(buildFileContent)
        }
    }

    private fun createMultiModuleProjectWithDependencies() {
        File(tempDir, "settings.gradle.kts")
            .writeText(
                """
            rootProject.name = "dependency-test"
            include("core")
            include("api")
            include("cli")
        """
                    .trimIndent()
            )

        // Core module - no dependencies
        val coreDir = File(tempDir, "core").apply { mkdirs() }
        File(coreDir, "build.gradle.kts")
            .writeText(
                """
            plugins {
                kotlin("jvm")
                `maven-publish`
            }
        """
                    .trimIndent()
            )

        // API module - depends on core
        val apiDir = File(tempDir, "api").apply { mkdirs() }
        File(apiDir, "build.gradle.kts")
            .writeText(
                """
            plugins {
                kotlin("jvm")
                `maven-publish`
            }
            dependencies {
                implementation(project(":core"))
            }
        """
                    .trimIndent()
            )

        // CLI module - depends on api
        val cliDir = File(tempDir, "cli").apply { mkdirs() }
        File(cliDir, "build.gradle.kts")
            .writeText(
                """
            plugins {
                kotlin("jvm")
                `maven-publish`
            }
            dependencies {
                implementation(project(":api"))
            }
        """
                    .trimIndent()
            )
    }

    private fun createComplexMultiModuleProject() {
        File(tempDir, "settings.gradle.kts")
            .writeText(
                """
            rootProject.name = "complex-project"
            include("frontend-core")
            include("frontend-ui")
            include("backend-core")
            include("backend-api")
            include("test-utils")
        """
                    .trimIndent()
            )

        // Frontend-core - publishable, no dependencies
        val frontendCoreDir = File(tempDir, "frontend-core").apply { mkdirs() }
        File(frontendCoreDir, "build.gradle.kts")
            .writeText(
                """
            plugins { kotlin("jvm"); `maven-publish` }
        """
                    .trimIndent()
            )

        // Frontend-ui - publishable, depends on frontend-core
        val frontendUiDir = File(tempDir, "frontend-ui").apply { mkdirs() }
        File(frontendUiDir, "build.gradle.kts")
            .writeText(
                """
            plugins { kotlin("jvm"); `maven-publish` }
            dependencies { implementation(project(":frontend-core")) }
        """
                    .trimIndent()
            )

        // Backend-core - publishable, no dependencies
        val backendCoreDir = File(tempDir, "backend-core").apply { mkdirs() }
        File(backendCoreDir, "build.gradle.kts")
            .writeText(
                """
            plugins { kotlin("jvm"); `maven-publish` }
        """
                    .trimIndent()
            )

        // Backend-api - publishable, depends on backend-core
        val backendApiDir = File(tempDir, "backend-api").apply { mkdirs() }
        File(backendApiDir, "build.gradle.kts")
            .writeText(
                """
            plugins { kotlin("jvm"); `maven-publish` }
            dependencies { implementation(project(":backend-core")) }
        """
                    .trimIndent()
            )

        // Test-utils - not publishable
        val testUtilsDir = File(tempDir, "test-utils").apply { mkdirs() }
        File(testUtilsDir, "build.gradle.kts")
            .writeText(
                """
            plugins { kotlin("jvm") }
        """
                    .trimIndent()
            )
        createModuleWithBuildFile(
            "frontend-core",
            """
            plugins { kotlin("jvm"); `maven-publish` }
        """
                .trimIndent(),
        )

        // Frontend-ui - publishable, depends on frontend-core
        createModuleWithBuildFile(
            "frontend-ui",
            """
            plugins { kotlin("jvm"); `maven-publish` }
            dependencies { implementation(project(":frontend-core")) }
        """
                .trimIndent(),
        )

        // Backend-core - publishable, no dependencies
        createModuleWithBuildFile(
            "backend-core",
            """
            plugins { kotlin("jvm"); `maven-publish` }
        """
                .trimIndent(),
        )

        // Backend-api - publishable, depends on backend-core
        createModuleWithBuildFile(
            "backend-api",
            """
            plugins { kotlin("jvm"); `maven-publish` }
            dependencies { implementation(project(":backend-core")) }
        """
                .trimIndent(),
        )

        // Test-utils - not publishable
        createModuleWithBuildFile(
            "test-utils",
            """
            plugins { kotlin("jvm") }
        """
                .trimIndent(),
        )
    }

    /** Helper to create a module directory and write its build.gradle.kts file. */
    private fun createModuleWithBuildFile(moduleName: String, buildFileContent: String) {
        val moduleDir = File(tempDir, moduleName).apply { mkdirs() }
        File(moduleDir, "build.gradle.kts").writeText(buildFileContent)
    }
}
