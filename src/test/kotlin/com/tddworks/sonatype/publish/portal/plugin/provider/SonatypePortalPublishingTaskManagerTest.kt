//package com.tddworks.sonatype.publish.portal.plugin.provider
//
//import org.gradle.testfixtures.ProjectBuilder
//import org.junit.jupiter.api.Assertions.assertNotNull
//import org.junit.jupiter.api.DisplayName
//import org.junit.jupiter.api.Nested
//import org.junit.jupiter.api.Test
//
//class SonatypePortalPublishingTaskManagerTest {
//
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
//}