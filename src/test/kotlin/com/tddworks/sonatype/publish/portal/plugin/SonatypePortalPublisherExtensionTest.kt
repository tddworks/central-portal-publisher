package com.tddworks.sonatype.publish.portal.plugin

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables
import uk.org.webcompere.systemstubs.jupiter.SystemStub
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension

@ExtendWith(SystemStubsExtension::class)
class SonatypePortalPublisherExtensionTest {
    @SystemStub lateinit var environmentVariables: EnvironmentVariables

    @Test
    fun `should create a sonatype portal publisher extension with authentication from system env`() {
        val project = ProjectBuilder.builder().build()
        val extension = SonatypePortalPublisherExtension(project.objects)

        environmentVariables.set("SONATYPE_USERNAME", "some-user")
        environmentVariables.set("SONATYPE_PASSWORD", "some-password")

        val authentication = extension.getAuthentication(project)

        assertNotNull(authentication)

        assertEquals("some-user", authentication?.username)
        assertEquals("some-password", authentication?.password)

        assertNull(extension.getSettings())
    }

    @Test
    fun `should create a sonatype portal publisher extension with authentication not found`() {
        val project = ProjectBuilder.builder().build()
        val extension = SonatypePortalPublisherExtension(project.objects)

        val authentication = extension.getAuthentication(project)

        assertNotNull(authentication)

        assertEquals("SONATYPE_USERNAME not found", authentication?.username)
        assertEquals("SONATYPE_PASSWORD not found", authentication?.password)

        assertNull(extension.getSettings())
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

        val authentication = extension.getAuthentication(project)

        assertNotNull(authentication)

        assertEquals("some-user", authentication?.username)
        assertEquals("some-password", authentication?.password)

        assertNull(extension.getSettings())
    }

    @Test
    fun `should create a sonatype portal publisher extension with default not found values`() {
        val project = ProjectBuilder.builder().build()
        val extension = SonatypePortalPublisherExtension(project.objects)

        val authentication = extension.getAuthentication(project)
        val settings = extension.getSettings()

        assertEquals("SONATYPE_USERNAME not found", authentication?.username)
        assertEquals("SONATYPE_PASSWORD not found", authentication?.password)
        assertNull(settings)
    }
}
