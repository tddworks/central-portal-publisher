package com.tddworks.sonatype.portal.plugin.task

import com.tddworks.sonatype.publish.portal.api.Authentication
import com.tddworks.sonatype.publish.portal.api.PublicationType
import com.tddworks.sonatype.publish.portal.plugin.tasks.BundlePublishTaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class BundlePublishTaskProviderTest {

    @Test
    fun `should create a publish task provider with manual`() {
        val project = ProjectBuilder.builder().build()
        val zipTaskProvider = project.tasks.register("zipTask", Zip::class.java)
        val authentication = Authentication("username", "password")

        val taskProvider = BundlePublishTaskProvider.publishTaskProvider(
            project,
            "Test",
            zipTaskProvider,
            authentication,
            false
        )

        assertNotNull(taskProvider)
        assertEquals("publishTestPublicationToSonatypePortal", taskProvider.name)
        assertEquals(PublicationType.USER_MANAGED, taskProvider.get().publicationType.get())
    }

    @Test
    fun `should create a publish task provider with automatic`() {
        val project = ProjectBuilder.builder().build()
        val zipTaskProvider = project.tasks.register("zipTask", Zip::class.java)
        val authentication = Authentication("username", "password")

        val taskProvider = BundlePublishTaskProvider.publishTaskProvider(
            project,
            "Test",
            zipTaskProvider,
            authentication,
            true
        )

        assertNotNull(taskProvider)
        assertEquals("publishTestPublicationToSonatypePortal", taskProvider.name)
        assertEquals(false, taskProvider.get().inputFile.isPresent)
        assertEquals(authentication.username, taskProvider.get().username.get())
        assertEquals(authentication.password, taskProvider.get().password.get())
        assertEquals(PublicationType.AUTOMATIC, taskProvider.get().publicationType.get())
    }
}
