package com.tddworks.sonatype.publish.portal.plugin.tasks

import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SimplifiedTasksTest {

    private lateinit var project: Project

    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder().build()
    }

    @Test
    fun `should register publishToCentral task`() {
        // Given
        val taskRegistry = SimplifiedTaskRegistry()

        // When
        taskRegistry.registerSimplifiedTasks(project)

        // Then
        assertThat(project.tasks.findByName("publishToCentral")).isNotNull
        assertThat(project.tasks.findByName("publishToCentral")?.group)
            .isEqualTo("Central Publishing")
        assertThat(project.tasks.findByName("publishToCentral")?.description)
            .isEqualTo("Publishes all artifacts to Maven Central")
    }

    @Test
    fun `should register bundleArtifacts task`() {
        // Given
        val taskRegistry = SimplifiedTaskRegistry()

        // When
        taskRegistry.registerSimplifiedTasks(project)

        // Then
        assertThat(project.tasks.findByName("bundleArtifacts")).isNotNull
        assertThat(project.tasks.findByName("bundleArtifacts")?.group)
            .isEqualTo("Central Publishing")
        assertThat(project.tasks.findByName("bundleArtifacts")?.description)
            .isEqualTo("Creates deployment bundles for all artifacts")
    }

    @Test
    fun `should register validatePublishing task`() {
        // Given
        val taskRegistry = SimplifiedTaskRegistry()

        // When
        taskRegistry.registerSimplifiedTasks(project)

        // Then
        assertThat(project.tasks.findByName("validatePublishing")).isNotNull
        assertThat(project.tasks.findByName("validatePublishing")?.group)
            .isEqualTo("Central Publishing")
        assertThat(project.tasks.findByName("validatePublishing")?.description)
            .isEqualTo("Validates publishing configuration and credentials")
    }

    @Test
    fun `publishToCentral should exist and be properly configured`() {
        // Given
        val taskRegistry = SimplifiedTaskRegistry()

        // When
        taskRegistry.registerSimplifiedTasks(project)

        // Then
        val publishToCentral = project.tasks.findByName("publishToCentral")
        assertThat(publishToCentral).isNotNull
        assertThat(publishToCentral?.group).isEqualTo("Central Publishing")
    }

    @Test
    fun `bundleArtifacts should exist and be properly configured`() {
        // Given
        val taskRegistry = SimplifiedTaskRegistry()

        // When
        taskRegistry.registerSimplifiedTasks(project)

        // Then
        val bundleArtifacts = project.tasks.findByName("bundleArtifacts")
        assertThat(bundleArtifacts).isNotNull
        assertThat(bundleArtifacts?.group).isEqualTo("Central Publishing")
    }

    @Test
    fun `should have deprecation mechanism available`() {
        // Given
        val taskRegistry = SimplifiedTaskRegistry()

        // When
        taskRegistry.registerSimplifiedTasks(project)

        // Then
        // Just verify that the registry can be created and has mappings
        assertThat(SimplifiedTaskRegistry.getTaskMappings()).isNotEmpty
    }

    @Test
    fun `should create task mapping for all old task names`() {
        // Given/When
        val mappings = SimplifiedTaskRegistry.getTaskMappings()

        // Then
        assertThat(mappings)
            .containsEntry("publishAllPublicationsToSonatypePortalRepository", "publishToCentral")
        assertThat(mappings)
            .containsEntry(
                "publishAggregationPublicationsToSonatypePortalRepository",
                "publishToCentral",
            )
        assertThat(mappings).containsEntry("zipAllPublications", "bundleArtifacts")
        assertThat(mappings).containsEntry("zipAggregationPublications", "bundleArtifacts")
    }
}
