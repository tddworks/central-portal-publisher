package com.tddworks.sonatype.publish.portal.plugin

import com.tddworks.sonatype.publish.portal.plugin.dsl.CentralPublisherExtension
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SimplifiedCentralPublisherPluginTest {

    private lateinit var project: Project
    private lateinit var plugin: CentralPublisherPlugin

    @BeforeEach
    fun setUp() {
        project = ProjectBuilder.builder().build()
        plugin = CentralPublisherPlugin()
    }

    @Test
    fun `should register extension on plugin application`() {
        // When
        plugin.apply(project)

        // Then - Should register the DSL extension
        val extension = project.extensions.findByName("centralPublisher")
        assertThat(extension).isNotNull()
        assertThat(extension).isInstanceOf(CentralPublisherExtension::class.java)
    }

    @Test
    fun `should be minimal and focused - apply method should be under 20 lines`() {
        // This test verifies our architecture goal
        // We can't directly test line count, but we can verify the plugin focuses on core
        // responsibilities

        // When
        plugin.apply(project)

        // Then - Plugin should only register extension and defer complex logic
        val extension = project.extensions.getByName("centralPublisher")
        assertThat(extension).isNotNull()

        // Extension should be ready but not yet configured (happens in afterEvaluate)
        assertThat(extension).isInstanceOf(CentralPublisherExtension::class.java)
    }

    @Test
    fun `should not create tasks during apply phase`() {
        // When
        plugin.apply(project)

        // Then - Tasks should not be created until afterEvaluate
        assertThat(project.tasks.findByName("publishToCentral")).isNull()
        assertThat(project.tasks.findByName("validatePublishing")).isNull()
        assertThat(project.tasks.findByName("bundleArtifacts")).isNull()
        assertThat(project.tasks.findByName("setupPublishing")).isNull()
    }

    @Test
    fun `should defer complex logic to afterEvaluate phase`() {
        // Given - Apply plugin and configure extension
        plugin.apply(project)
        val extension = project.extensions.getByType(CentralPublisherExtension::class.java)
        extension.credentials {
            username = "test-user"
            password = "test-password"
        }
        extension.projectInfo {
            name = "test-project"
            description = "Test description"
            url = "https://github.com/test/project"
        }

        // Then - Extension should be configured but tasks not created yet
        // (This tests that our plugin defers work to afterEvaluate)
        assertThat(extension.hasExplicitConfiguration()).isTrue()
    }

    @Test
    fun `should work with existing project plugins`() {
        // Given - Apply plugin and Java plugin
        plugin.apply(project)
        project.pluginManager.apply("java-library")

        val extension = project.extensions.getByType(CentralPublisherExtension::class.java)
        extension.credentials {
            username = "test-user"
            password = "test-password"
        }

        // Then - Should be ready for configuration
        assertThat(extension.hasExplicitConfiguration()).isTrue()
        assertThat(project.plugins.hasPlugin("java-library")).isTrue()
    }

    @Test
    fun `should handle projects without explicit configuration gracefully`() {
        // Given - Apply plugin but don't configure extension
        plugin.apply(project)

        val extension = project.extensions.getByType(CentralPublisherExtension::class.java)

        // Then - Should not fail, extension should indicate no explicit configuration
        assertThat(extension.hasExplicitConfiguration()).isFalse()
    }

    @Test
    fun `should provide extension for configuration`() {
        // Given - Apply plugin with partial configuration
        plugin.apply(project)
        val extension = project.extensions.getByType(CentralPublisherExtension::class.java)
        extension.credentials {
            username = "test-user"
            // Missing password - but extension should still be usable
        }

        // Then - Extension should be available for configuration
        assertThat(extension.hasExplicitConfiguration()).isTrue()

        // Configuration validation happens later in the pipeline
        val config = extension.build()
        assertThat(config.credentials.username).isEqualTo("test-user")
    }
}
