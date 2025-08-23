package com.tddworks.sonatype.publish.portal.plugin.tasks

import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TaskAliasTest {

    private lateinit var project: Project

    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder().build()
    }

    @Test
    fun `should provide correct task alias mappings`() {
        // Given/When
        val mappings = SimplifiedTaskRegistry.getTaskMappings()

        // Then
        assertThat(mappings).hasSize(8) // 8 total mappings including publication-specific ones

        // Main task mappings
        assertThat(
                SimplifiedTaskRegistry.getNewTaskName(
                    "publishAllPublicationsToSonatypePortalRepository"
                )
            )
            .isEqualTo("publishToCentral")
        assertThat(
                SimplifiedTaskRegistry.getNewTaskName(
                    "publishAggregationPublicationsToSonatypePortalRepository"
                )
            )
            .isEqualTo("publishToCentral")
        assertThat(SimplifiedTaskRegistry.getNewTaskName("zipAllPublications"))
            .isEqualTo("bundleArtifacts")
        assertThat(SimplifiedTaskRegistry.getNewTaskName("zipAggregationPublications"))
            .isEqualTo("bundleArtifacts")

        // Publication-specific mappings
        assertThat(
                SimplifiedTaskRegistry.getNewTaskName(
                    "publishMavenPublicationToSonatypePortalRepository"
                )
            )
            .isEqualTo("publishMaven")
        assertThat(
                SimplifiedTaskRegistry.getNewTaskName(
                    "publishKotlinMultiplatformPublicationToSonatypePortalRepository"
                )
            )
            .isEqualTo("publishKMP")
        assertThat(SimplifiedTaskRegistry.getNewTaskName("zipMavenPublication"))
            .isEqualTo("bundleMaven")
        assertThat(SimplifiedTaskRegistry.getNewTaskName("zipKotlinMultiplatformPublication"))
            .isEqualTo("bundleKMP")
    }

    @Test
    fun `should return original task name when no alias exists`() {
        // Given/When/Then
        assertThat(SimplifiedTaskRegistry.getNewTaskName("someUnknownTask"))
            .isEqualTo("someUnknownTask")
        assertThat(SimplifiedTaskRegistry.getNewTaskName("build")).isEqualTo("build")
    }

    @Test
    fun `should have consistent naming convention`() {
        // Given
        val mappings = SimplifiedTaskRegistry.getTaskMappings()
        val newTaskNames = mappings.values

        // Then
        // All publish tasks should start with 'publish'
        val publishTasks = newTaskNames.filter { it.contains("publish", ignoreCase = true) }
        publishTasks.forEach { taskName -> assertThat(taskName).startsWith("publish") }

        // All bundle tasks should start with 'bundle'
        val bundleTasks = newTaskNames.filter { it.contains("bundle", ignoreCase = true) }
        bundleTasks.forEach { taskName -> assertThat(taskName).startsWith("bundle") }

        // Task names should be camelCase
        newTaskNames.forEach { taskName -> assertThat(taskName).matches("[a-z][a-zA-Z]*") }
    }

    @Test
    fun `should provide reverse lookup functionality`() {
        // Given
        val mappings = SimplifiedTaskRegistry.getTaskMappings()

        // When/Then - verify we can go from old to new
        mappings.forEach { (oldName, newName) ->
            assertThat(SimplifiedTaskRegistry.getNewTaskName(oldName)).isEqualTo(newName)
        }
    }
}
