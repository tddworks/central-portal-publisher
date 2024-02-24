//package com.tddworks.sonatype.publish.portal.plugin
//
//import org.gradle.testfixtures.ProjectBuilder
//import org.junit.jupiter.api.Assertions.*
//import org.junit.jupiter.api.Test
//
//internal class SonatypePortalPublisherPluginTest {
//
//
//    @Test
//    fun `should create zip all publications task with default values`() {
//        val project = ProjectBuilder.builder().build()
//
//        val plugin = SonatypePortalPublisherPlugin()
//
//        plugin.apply(project)
//
//        // internally it calls project.evaluate()
//        // when: "triggering a project.evaluate"
//        project.getTasksByName("tasks", false)
//
//        assertNotNull(project.tasks.findByName("zipAllPublications"))
//    }
//
//    @Test
//    fun `publish all publications task should depends on zipAllPublications with default values`() {
//        val project = ProjectBuilder.builder().build()
//
//        val plugin = SonatypePortalPublisherPlugin()
//
//        plugin.apply(project)
//
//        // internally it calls project.evaluate()
//        // when: "triggering a project.evaluate"
//        project.getTasksByName("tasks", false)
//
//        val task = project.tasks.findByName("publishAllPublicationsToSonatypePortalRepository")
//        assertTrue(task?.taskDependencies?.getDependencies(task)?.find { it.name == "zipAllPublications" } != null)
//    }
//
//    @Test
//    fun `should create publish all publications task with default values`() {
//        val project = ProjectBuilder.builder().build()
//
//        val plugin = SonatypePortalPublisherPlugin()
//
//        plugin.apply(project)
//
//        // internally it calls project.evaluate()
//        // when: "triggering a project.evaluate"
//        project.getTasksByName("tasks", false)
//
//        val task = project.tasks.findByName("publishAllPublicationsToSonatypePortalRepository")
//        assertNotNull(task)
//    }
//
//    @Test
//    fun `should add project as dependency project to configuration when aggregation is true`() {
//        val project = ProjectBuilder.builder().build()
//
//        project.plugins.apply(SonatypePortalPublisherPlugin::class.java)
//
//        project.sonatypePortalPublisherExtension.apply {
//            project.settings {
//                aggregation = true
//            }
//        }
//
//        project.getTasksByName("tasks", false)
//
//        val dependencies = project.configurations.getByName(ZIP_CONFIGURATION_CONSUMER).dependencies
//
//        assertEquals(1, dependencies.size)
//    }
//
//
//    @Test
//    fun `should create publish aggregation publications task when aggregation is true`() {
//        val project = ProjectBuilder.builder().build()
//
//        val plugin = SonatypePortalPublisherPlugin()
//
//        plugin.apply(project)
//
//        project.sonatypePortalPublisherExtension.apply {
//            project.settings {
//                aggregation = true
//            }
//        }
//
//        // internally it calls project.evaluate()
//        // when: "triggering a project.evaluate"
//        project.getTasksByName("tasks", false)
//
//        assertNotNull(project.tasks.findByName("publishAggregationPublicationsToSonatypePortalRepository"))
//    }
//
//    @Test
//    fun `should not create publish all publications task for with default values`() {
//        val project = ProjectBuilder.builder().build()
//
//        val plugin = SonatypePortalPublisherPlugin()
//
//        plugin.apply(project)
//
//        // internally it calls project.evaluate()
//        // when: "triggering a project.evaluate"
//        project.getTasksByName("tasks", false)
//
//        assertNull(project.tasks.findByName("publishAggregationPublicationsToSonatypePortalRepository"))
//    }
//
//
//    @Test
//    fun `should not create zip aggregation publication task with default values`() {
//        val project = ProjectBuilder.builder().build()
//
//        project.plugins.apply(SonatypePortalPublisherPlugin::class.java)
//
//        // internally it calls project.evaluate()
//        // when: "triggering a project.evaluate"
//        project.getTasksByName("tasks", false)
//
//        assertNull(project.tasks.findByName("zipAggregationPublications"))
//    }
//
//
//    @Test
//    fun `should create zip aggregation publication task when aggregation is true`() {
//        val project = ProjectBuilder.builder().build()
//
//        val extension = SonatypePortalPublisherPlugin()
//
//        extension.apply(project)
//
//        project.sonatypePortalPublisherExtension.apply {
//            project.settings {
//                aggregation = true
//            }
//        }
//
//        // internally it calls project.evaluate()
//        // when: "triggering a project.evaluate"
//        project.getTasksByName("tasks", false)
//
//        assertNotNull(project.tasks.findByName("zipAggregationPublications"))
//    }
//
//
//    @Test
//    fun `should apply the sonatype portal publisher plugin`() {
//        val project = ProjectBuilder.builder().build()
//        val plugin = SonatypePortalPublisherPlugin()
//
//        plugin.apply(project)
//
//        assertNotNull(project.sonatypePortalPublisherExtension)
//    }
//}
//
//
