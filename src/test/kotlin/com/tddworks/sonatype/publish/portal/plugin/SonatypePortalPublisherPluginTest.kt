package com.tddworks.sonatype.publish.portal.plugin

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class SonatypePortalPublisherPluginTest {

    @Test
    fun `should add root project as dependency project to configuration`() {
        val project = ProjectBuilder.builder().build()
        val plugin = SonatypePortalPublisherPlugin()

        plugin.apply(project)

        project.getTasksByName("tasks", false)

        val dependencies = project.configurations.getByName(ZIP_CONFIGURATION_CONSUMER).dependencies

        assertEquals(1, dependencies.size)
    }

    @Test
    fun `should create publish all publications task for with default values`() {
        val project = ProjectBuilder.builder().build()

        val plugin = SonatypePortalPublisherPlugin()

        plugin.apply(project)

        // internally it calls project.evaluate()
        // when: "triggering a project.evaluate"
        project.getTasksByName("tasks", false)

        assertNotNull(project.tasks.findByName("publishAllPublicationsToSonatypePortalRepository"))
    }


    @Test
    fun `should create zip all publication task for with default values`() {
        val project = ProjectBuilder.builder().build()

        val plugin = SonatypePortalPublisherPlugin()

        plugin.apply(project)

        // internally it calls project.evaluate()
        // when: "triggering a project.evaluate"
        project.getTasksByName("tasks", false)

        assertNotNull(project.tasks.findByName("zipAllPublications"))
    }


    @Test
    fun `should apply the sonatype portal publisher plugin`() {
        val project = ProjectBuilder.builder().build()
        val plugin = SonatypePortalPublisherPlugin()

        plugin.apply(project)

        assertNotNull(project.sonatypePortalPublisherExtension)
    }
}


