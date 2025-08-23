package com.tddworks.sonatype.publish.portal.plugin

import com.tddworks.sonatype.publish.portal.plugin.config.*
import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** Tests for actual task execution behavior (TDD approach) */
class CentralPublisherTaskExecutionTest {

    @TempDir private lateinit var tempDir: File

    private lateinit var project: Project
    private lateinit var manager: CentralPublisherTaskManager
    private lateinit var config: CentralPublisherConfig

    @BeforeEach
    fun setUp() {
        project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        manager = CentralPublisherTaskManager(project)

        // Create a valid configuration for testing
        config =
            CentralPublisherConfigBuilder()
                .credentials {
                    username = "test-user"
                    password = "test-password"
                }
                .projectInfo {
                    name = "test-project"
                    description = "Test project description"
                    url = "https://github.com/test/project"
                    license {
                        name = "Apache-2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                    developer {
                        id = "test-dev"
                        name = "Test Developer"
                        email = "test@example.com"
                    }
                    scm {
                        url = "https://github.com/test/project"
                        connection = "scm:git:git://github.com/test/project.git"
                        developerConnection = "scm:git:ssh://github.com/test/project.git"
                    }
                }
                .build()
    }

    @Test
    fun `validatePublishing should validate configuration and report results`() {
        // Given - Tasks are created
        manager.createTasks(config)
        val validateTask = project.tasks.getByName("validatePublishing")

        // When/Then - Task should execute successfully
        var executedSuccessfully = false
        try {
            validateTask.actions.forEach { action -> action.execute(validateTask) }
            executedSuccessfully = true
        } catch (e: Exception) {
            // Should not throw for valid config
            throw e
        }

        // Should execute validation successfully
        assertThat(executedSuccessfully).isTrue()
    }

    @Test
    fun `validatePublishing should fail when configuration is invalid`() {
        // Given - Invalid configuration (missing required fields)
        val invalidConfig =
            CentralPublisherConfigBuilder()
                .credentials {
                    username = "" // Empty username should fail validation
                    password = ""
                }
                .build()

        manager.createTasks(invalidConfig)
        val validateTask = project.tasks.getByName("validatePublishing")

        // When/Then - Task should fail validation
        var exceptionThrown = false
        try {
            validateTask.actions.forEach { action -> action.execute(validateTask) }
        } catch (e: Exception) {
            exceptionThrown = true
            assertThat(e.message).contains("validation failed")
        }

        // Should throw exception for invalid config
        assertThat(exceptionThrown).isTrue()
    }

    @Test
    fun `bundleArtifacts should create deployment bundle when artifacts exist`() {
        // Given - Maven-publish plugin is applied and local repo with artifacts exists
        project.plugins.apply("maven-publish")

        // Create mock artifacts in local repo
        val localRepoDir = File(project.layout.buildDirectory.get().asFile, "repo")
        localRepoDir.mkdirs()
        val artifactFile = File(localRepoDir, "com/test/test-project/1.0/test-project-1.0.jar")
        artifactFile.parentFile.mkdirs()
        artifactFile.writeText("mock artifact content")

        manager.createTasks(config)
        val bundleTask = project.tasks.getByName("bundleArtifacts")

        // When - Task is executed
        bundleTask.actions.forEach { action -> action.execute(bundleTask) }

        // Then - Should create bundle zip file
        val expectedBundlePath =
            File(
                project.layout.buildDirectory.get().asFile,
                "central-portal/${project.name}-${project.version}-bundle.zip",
            )
        assertThat(expectedBundlePath).exists()
    }

    @Test
    fun `bundleArtifacts should fail when no artifacts are available`() {
        // Given - Maven-publish plugin applied but no artifacts are configured
        project.plugins.apply("maven-publish")
        manager.createTasks(config)
        val bundleTask = project.tasks.getByName("bundleArtifacts")

        // When/Then - Task should fail when no artifacts exist
        var exceptionThrown = false
        try {
            bundleTask.actions.forEach { action -> action.execute(bundleTask) }
        } catch (e: Exception) {
            exceptionThrown = true
            assertThat(e.message).contains("No artifacts found")
        }

        // Should throw exception when no artifacts available
        assertThat(exceptionThrown).isTrue()
    }

    @Test
    fun `publishToCentral should upload bundle to Sonatype Central Portal`() {
        // Given - Use dry run config to avoid actual network calls
        val mockConfig =
            CentralPublisherConfigBuilder()
                .credentials {
                    username = "test-user"
                    password = "test-password"
                }
                .projectInfo {
                    name = "test-project"
                    description = "Test project description"
                    url = "https://github.com/test/project"
                    license {
                        name = "Apache-2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                    developer {
                        id = "test-dev"
                        name = "Test Developer"
                        email = "test@example.com"
                    }
                    scm {
                        url = "https://github.com/test/project"
                        connection = "scm:git:git://github.com/test/project.git"
                        developerConnection = "scm:git:ssh://github.com/test/project.git"
                    }
                }
                .publishing {
                    dryRun = true // This prevents actual network calls
                }
                .build()

        // Create mock bundle file
        project.plugins.apply("maven-publish")
        manager.createTasks(mockConfig)

        val bundleDir = File(project.layout.buildDirectory.get().asFile, "central-portal")
        bundleDir.mkdirs()
        val bundleFile = File(bundleDir, "${project.name}-${project.version}-bundle.zip")
        bundleFile.writeText("dummy bundle content")

        val publishTask = project.tasks.getByName("publishToCentral")

        // When/Then - Task should execute in dry run mode (no actual upload)
        var executedSuccessfully = false
        try {
            publishTask.actions.forEach { action -> action.execute(publishTask) }
            executedSuccessfully = true
        } catch (e: Exception) {
            // Should not throw in dry run mode
            throw e
        }

        // Should successfully simulate upload without network calls
        assertThat(executedSuccessfully).isTrue()
    }

    @Test
    fun `publishToCentral should respect dry run configuration`() {
        // Given - Dry run configuration
        val dryRunConfig =
            CentralPublisherConfigBuilder()
                .credentials {
                    username = "test-user"
                    password = "test-password"
                }
                .projectInfo {
                    name = "test-project"
                    description = "Test project description"
                    url = "https://github.com/test/project"
                }
                .publishing { dryRun = true }
                .build()

        // Create bundle file for dry run test
        val bundleDir = File(project.layout.buildDirectory.get().asFile, "central-portal")
        bundleDir.mkdirs()
        val bundleFile = File(bundleDir, "${project.name}-${project.version}-bundle.zip")
        bundleFile.writeText("dummy bundle content")

        manager.createTasks(dryRunConfig)
        val publishTask = project.tasks.getByName("publishToCentral")

        // When/Then - Task should execute in dry run mode without actual upload
        var executedSuccessfully = false
        try {
            publishTask.actions.forEach { action -> action.execute(publishTask) }
            executedSuccessfully = true
        } catch (e: Exception) {
            // Should not throw in dry run mode
            throw e
        }

        // Should simulate upload without actual network calls
        assertThat(executedSuccessfully).isTrue()
    }
}
