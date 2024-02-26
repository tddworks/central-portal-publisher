//package com.tddworks.sonatype.publish.portal.plugin
//
//
//import com.tddworks.sonatype.publish.portal.plugin.provider.SonatypePortalPublishingTaskManager
//import org.gradle.api.artifacts.repositories.MavenArtifactRepository
//import org.gradle.api.file.Directory
//import org.gradle.api.internal.project.ProjectInternal
//import org.gradle.api.provider.Provider
//import org.gradle.testfixtures.ProjectBuilder
//import org.junit.jupiter.api.Assertions.assertEquals
//import org.junit.jupiter.api.Test
//import org.mockito.Mockito.RETURNS_DEEP_STUBS
//import org.mockito.kotlin.doReturn
//import org.mockito.kotlin.mock
//import org.mockito.kotlin.verify
//import org.mockito.kotlin.whenever
//import java.io.File
//
//class PublicationTests {
//
//    private val project = ProjectBuilder.builder().build() as ProjectInternal
//
//    init {
////        project.plugins.apply("kotlin-multiplatform")
//        project.plugins.apply("maven-publish")
//
//    }
//
//    @Test
//    fun testRegisterPublishPublicationToMavenRepositoryTask() {
//        // Given
//        project.tasks.register("publishSome-namePublicationToMavenRepository")
//        val sonatypeDestinationPath = mock<Provider<Directory>>(defaultAnswer = RETURNS_DEEP_STUBS)
//        val fileDirectory = mock<File>()
//
//        val directory = mock<Directory> {
//            on { asFile } doReturn fileDirectory
//        }
//
//        whenever(sonatypeDestinationPath.get()).thenReturn(directory)
//
//
//        val sonatypePortalPublishingTaskManager = SonatypePortalPublishingTaskManager()
//
//
//        // When
//        val result = sonatypePortalPublishingTaskManager.registerPublishPublicationToMavenRepositoryTask(
//            project,
//            "some-name",
//            sonatypeDestinationPath
//        )
//
//        // Then
//        assertEquals("publishSome-namePublicationToMavenRepository", result.name)
//    }
//
//    @Test
//    fun `should create maven artifact repository`() {
//        val sonatypePortalPublishingTaskManager = SonatypePortalPublishingTaskManager()
//
//        val artifactRepositoryPath =
//            sonatypePortalPublishingTaskManager.preparePublishingBuildRepository("some-name", project).get().asFile.path
//
//        val repositories = project.publishingExtension.repositories
//
//        val mavenRepository = repositories[0] as MavenArtifactRepository
//
//        assertEquals("Some-name", mavenRepository.name)
//        assertEquals(artifactRepositoryPath, mavenRepository.url.path)
//    }
//}
//
