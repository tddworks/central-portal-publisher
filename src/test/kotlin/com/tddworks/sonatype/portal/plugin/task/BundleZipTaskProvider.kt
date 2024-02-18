package com.tddworks.sonatype.portal.plugin.task


import com.tddworks.sonatype.publish.portal.plugin.tasks.BundleZipTaskProvider
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.provider.Provider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BundleZipTaskProviderTest {

    @Test
    fun `should create a zip task provider`() {
        val project: Project = ProjectBuilder.builder().build()

        val publishToSonatypeTaskProvider: TaskProvider<Task> =
            project.tasks.register("publishToSonatypeTask", Task::class.java)

        val sonatypeDestinationPath: Provider<Directory> = project.objects.directoryProperty()

        val zipTaskProvider = BundleZipTaskProvider.zipTaskProvider(
            project,
            "Test",
            publishToSonatypeTaskProvider,
            sonatypeDestinationPath
        )

        assertNotNull(zipTaskProvider)
        assertEquals("zipTestPublication", zipTaskProvider.name)
        assertTrue(zipTaskProvider.get() is Zip)
    }
}


