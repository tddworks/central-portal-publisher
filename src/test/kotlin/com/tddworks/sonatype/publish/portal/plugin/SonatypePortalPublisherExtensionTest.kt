package com.tddworks.sonatype.publish.portal.plugin

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SonatypePortalPublisherExtensionTest {

    @Test
    fun `should create a sonatype portal publisher extension with settings autoPublish`() {
        val project = ProjectBuilder.builder().build()
        val extension = SonatypePortalPublisherExtension(project.objects)

        extension.apply {
            project.settings {
                autoPublish = true
            }
        }

        val settings = extension.getSettings()

        assertNotNull(settings)
        assertEquals(true, settings?.autoPublish)
        assertEquals(false, settings?.aggregation)

        assertNull(extension.getAuthentication())
    }

    @Test
    fun `should create a sonatype portal publisher extension with authentication`() {
        val project = ProjectBuilder.builder().build()
        val extension = SonatypePortalPublisherExtension(project.objects)

        extension.apply {
            project.authentication {
                username = "some-user"
                password = "some-password"
            }
        }

        val authentication = extension.getAuthentication()

        assertNotNull(authentication)

        assertEquals("some-user", authentication?.username)
        assertEquals("some-password", authentication?.password)

        assertNull(extension.getSettings())
    }


    @Test
    fun `should create a sonatype portal publisher extension with default null values`() {
        val project = ProjectBuilder.builder().build()
        val extension = SonatypePortalPublisherExtension(project.objects)

        val authentication = extension.getAuthentication()
        val settings = extension.getSettings()

        assertNull(authentication)
        assertNull(settings)
    }
}