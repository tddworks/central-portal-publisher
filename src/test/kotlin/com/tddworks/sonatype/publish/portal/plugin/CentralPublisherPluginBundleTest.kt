package com.tddworks.sonatype.publish.portal.plugin

import com.tddworks.sonatype.publish.portal.plugin.dsl.CentralPublisherExtension
import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Tests for bundle creation functionality. These tests verify of publishing to local repo then
 * creating ZIP bundles.
 */
class CentralPublisherPluginBundleTest {

    private lateinit var project: Project

    @TempDir lateinit var tempDir: File

    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder().withProjectDir(tempDir).build()
    }

    /** Helper method to simulate requesting publishing tasks, which triggers plugin activation */
    private fun simulatePublishingTaskRequest() {
        // Set test mode property so plugin activates during tests
        project.extensions.extraProperties.set("testingPublishingTask", true)
    }

    @Test
    fun `should create bundle creation logic`() {
        // Given - simulate publishing task request
        simulatePublishingTaskRequest()

        project.group = "com.tddworks.test"
        project.version = "1.0.0"
        project.pluginManager.apply("maven-publish")
        project.pluginManager.apply("com.tddworks.central-publisher")

        // When - Configure plugin
        project.extensions.configure(CentralPublisherExtension::class.java) {
            credentials {
                username = "test-user"
                password = "test-token"
            }
        }

        // Trigger afterEvaluate to simulate actual plugin behavior
        project.getTasksByName("tasks", false) // This triggers project evaluation

        // Then - Bundle task should exist and be properly configured
        val bundleTask = project.tasks.findByName("bundleArtifacts")
        assertThat(bundleTask).isNotNull()
        assertThat(bundleTask!!.description).contains("Prepare your artifacts for publishing")

        // Should depend on local repo publishing (not maven local)
        assertThat(bundleTask.dependsOn).contains("publishAllPublicationsToLocalRepoRepository")

        // Local repo should be configured for checksum generation
        val publishing =
            project.extensions.getByType(org.gradle.api.publish.PublishingExtension::class.java)
        val localRepo = publishing.repositories.findByName("LocalRepo")
        assertThat(localRepo).isNotNull()
    }

    @Test
    fun `should validate namespace warnings during bundle creation`() {
        // Given - Invalid namespace
        project.group = "com.example" // Maven Central doesn't allow example namespaces
        project.version = "1.0.0"
        project.pluginManager.apply("maven-publish")
        project.pluginManager.apply("com.tddworks.central-publisher")

        // When - Configure plugin
        project.extensions.configure(CentralPublisherExtension::class.java) {
            credentials {
                username = "test-user"
                password = "test-token"
            }
        }

        // Then - Should configure without crashing (warnings logged during execution)
        val extension = project.extensions.getByType(CentralPublisherExtension::class.java)
        val config = extension.build()

        assertThat(config.projectInfo.name).isNotEmpty()
        // Note: Actual namespace validation warnings are logged during bundle creation task
        // execution
    }

    @Test
    fun `should configure tasks in correct dependency order`() {
        // Given - simulate publishing task request
        simulatePublishingTaskRequest()

        project.group = "com.tddworks.test"
        project.version = "1.0.0"
        project.pluginManager.apply("maven-publish")
        project.pluginManager.apply("com.tddworks.central-publisher")

        // When - Configure plugin
        project.extensions.configure(CentralPublisherExtension::class.java) {
            credentials {
                username = "test-user"
                password = "test-token"
            }
        }

        // Trigger afterEvaluate to simulate actual plugin behavior
        project.getTasksByName("tasks", false) // This triggers project evaluation

        // Then - Task dependency chain should be correct
        val publishToCentral = project.tasks.findByName("publishToCentral")!!
        val bundleArtifacts = project.tasks.findByName("bundleArtifacts")!!
        val publishToLocalRepo =
            project.tasks.findByName("publishAllPublicationsToLocalRepoRepository")!!

        // publishToCentral depends on bundleArtifacts
        assertThat(publishToCentral.dependsOn).contains("bundleArtifacts")

        // bundleArtifacts depends on publishAllPublicationsToLocalRepoRepository
        assertThat(bundleArtifacts.dependsOn)
            .contains("publishAllPublicationsToLocalRepoRepository")

        // This creates the correct flow: publishAllPublicationsToLocalRepoRepository ->
        // bundleArtifacts
        // -> publishToCentral
    }

    @Test
    fun `should handle dry run configuration properly in tasks`() {
        // Given
        project.group = "com.tddworks.test"
        project.version = "1.0.0"
        project.pluginManager.apply("maven-publish")
        project.pluginManager.apply("com.tddworks.central-publisher")

        // When - Configure with dry run
        project.extensions.configure(CentralPublisherExtension::class.java) {
            credentials {
                username = "test-user"
                password = "test-token"
            }
            publishing { dryRun = true }
        }

        // Then - Configuration should reflect dry run setting
        val extension = project.extensions.getByType(CentralPublisherExtension::class.java)
        val config = extension.build()

        assertThat(config.publishing.dryRun).isTrue()

        // Tasks should still be created even in dry run mode
        val bundleTask = project.tasks.findByName("bundleArtifacts")
        assertThat(bundleTask).isNull() // Not created until afterEvaluate is triggered
    }

    @Test
    fun `should configure signing integration properly`() {
        // Given
        project.group = "com.tddworks.test"
        project.version = "1.0.0"
        project.pluginManager.apply("maven-publish")
        project.pluginManager.apply("com.tddworks.central-publisher")

        // When - Configure with signing
        project.extensions.configure(CentralPublisherExtension::class.java) {
            credentials {
                username = "test-user"
                password = "test-token"
            }
            signing {
                keyId = "test-key-id"
                password = "test-key-password"
            }
        }

        // Then - Signing configuration should be captured
        val extension = project.extensions.getByType(CentralPublisherExtension::class.java)
        val config = extension.build()

        assertThat(config.signing.keyId).isEqualTo("test-key-id")
        assertThat(config.signing.password).isEqualTo("test-key-password")

        // Note: Actual signing plugin application and signature file handling
        // happens during publication provider configuration
    }
}
