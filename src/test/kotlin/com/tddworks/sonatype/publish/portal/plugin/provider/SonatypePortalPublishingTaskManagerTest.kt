package com.tddworks.sonatype.publish.portal.plugin.provider

import com.tddworks.sonatype.publish.portal.api.Authentication
import com.tddworks.sonatype.publish.portal.plugin.ZipPublicationTaskFactory
import com.tddworks.sonatype.publish.portal.plugin.tasks.DevelopmentBundlePublishTaskFactory
import com.tddworks.sonatype.publish.portal.plugin.tasks.PublishPublicationToMavenRepositoryTaskFactory
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever


@ExtendWith(MockitoExtension::class)
class SonatypePortalPublishingTaskManagerTest {
    @InjectMocks
    lateinit var target: SonatypePortalPublishingTaskManager

    @Mock
    lateinit var publishPublicationToMavenRepositoryTaskFactory: PublishPublicationToMavenRepositoryTaskFactory

    @Mock
    lateinit var zipPublicationTaskFactory: ZipPublicationTaskFactory

    @Mock
    lateinit var developmentBundlePublishTaskFactory: DevelopmentBundlePublishTaskFactory

    @Mock
    lateinit var publicationProvider: PublicationProvider


    private val project = ProjectBuilder.builder().build()

    init {
//        project.plugins.apply("kotlin-multiplatform")
        project.plugins.apply("maven-publish")
    }

    @Test
    fun `should register tasks for maven publication`() {
        // Create a task to zip all publications for testing
        project.tasks.register("zipAllPublications")

        val taskTaskProvider = mock<TaskProvider<Task>>()

        val zipTaskTaskProvider = mock<TaskProvider<Zip>>()

        val auth = mock<Authentication>()

        target.apply {
            autoPublish = true

            authentication = auth
        }

        whenever(publishPublicationToMavenRepositoryTaskFactory.createTask(project, "maven")).thenReturn(
            taskTaskProvider
        )

        whenever(zipPublicationTaskFactory.createZipTask(project, "maven", taskTaskProvider)).thenReturn(
            zipTaskTaskProvider
        )

        target.registerTasksForPublication(project, "maven")

        verify(developmentBundlePublishTaskFactory).createTask(project, "maven", zipTaskTaskProvider, auth, true)
    }

    @Test
    fun `should register the zip configuration producer`() {
        target.registerPublications(project)

        assertNotNull(project.configurations.findByName("zipConfigurationProducer"))

        verify(publicationProvider).preparePublication(project)
    }

    @Test
    fun `should create zip all and publish all tasks`() {

        target.registerPublishingTasks(project)

        assertNotNull(project.tasks.findByName("zipAllPublications"))
        assertNotNull(project.tasks.findByName("publishAllPublicationsToSonatypePortalRepository"))
    }

//    @Nested
//    @DisplayName("Register Publish Task For Single Project")
//    inner class RegisterPublishTaskForSingleProject {
//        @Test
//        fun `should register publish all task with zip all publications task as dependency`() {
//            val project = ProjectBuilder.builder().build()
//
//            val publishingTaskManager = SonatypePortalPublishingTaskManager()
//
//            publishingTaskManager.registerPublishingTask(project)
//            val publishAllTask = project.tasks.findByName("publishAllPublicationsToSonatypePortalRepository")
//
//            val zipAllTask = project.tasks.findByName("zipAllPublications")
//
//            assertNotNull(zipAllTask)
//            assertNotNull(publishAllTask)
//
//            // make sure publish all publications depends on zipAllPublications
//            assertNotNull(publishAllTask?.taskDependencies?.getDependencies(publishAllTask)?.find { it.name == "zipAllPublications" })
//        }
//    }
}