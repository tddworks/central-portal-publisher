package com.tddworks.sonatype.publish.portal.plugin.provider

import com.tddworks.sonatype.publish.portal.api.Authentication
import com.tddworks.sonatype.publish.portal.api.configurePom
import com.tddworks.sonatype.publish.portal.plugin.SonatypePortalPublisherPlugin
import com.tddworks.sonatype.publish.portal.plugin.ZipPublicationTaskFactory
import com.tddworks.sonatype.publish.portal.plugin.createZipConfigurationProducer
import com.tddworks.sonatype.publish.portal.plugin.publishingExtension
import com.tddworks.sonatype.publish.portal.plugin.tasks.DevelopmentBundlePublishTaskFactory
import com.tddworks.sonatype.publish.portal.plugin.tasks.PublishPublicationToMavenRepositoryTaskFactory
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*

interface PublishingTaskManager {
    /**
     * Register the publishing task for the project
     * e.g
     * - publishMavenPublicationToMavenRepository
     * - publishKotlinMultiplatformPublicationToMavenRepository
     * - publishAllPublicationsToSonatypePortalRepository
     */
    fun registerPublishingTasks(project: Project)
}

internal const val PUBLISHING_GROUP = "publishing"

class SonatypePortalPublishingTaskManager(
    private val publishPublicationToMavenRepositoryTaskFactory: PublishPublicationToMavenRepositoryTaskFactory,
    private val zipPublicationTaskFactory: ZipPublicationTaskFactory,
    private val developmentBundlePublishTaskFactory: DevelopmentBundlePublishTaskFactory,
) : PublishingTaskManager {

    var authentication: Authentication? = null
    var autoPublish: Boolean? = false

    override fun registerPublishingTasks(project: Project) {

        // Create a task to zip all publications
        val zipAllPublications = project.tasks.register(SonatypePortalPublisherPlugin.ZIP_ALL_PUBLICATIONS)

        // Create a task to publish all publications to Sonatype Portal
        project.tasks.register(SonatypePortalPublisherPlugin.PUBLISH_ALL_PUBLICATIONS_TO_SONATYPE_PORTAL_REPOSITORY) {
            group = PUBLISHING_GROUP
            // publish all publications depends on zipAllPublications
            dependsOn(zipAllPublications)
        }

        project.allprojects.forEach { pj ->
            pj.pluginManager.withPlugin("maven-publish") {
                registerPublications(pj)
            }
        }
    }

    private fun registerPublications(
        project: Project,
    ) {


        // register the zip configuration producer
        project.createZipConfigurationProducer

        // need config this before loop through all publications
        preparePublications(project)


        val publishing = project.publishingExtension
        // Configure each publication
        // find all publications
        publishing.publications.configureEach {

            project.logger.quiet("Found publication: $name")

            registerTasks(project, name)
        }
    }

    private fun preparePublications(project: Project) {
        JvmPublicationProvider().preparePublication(project)
    }

    private fun registerTasks(
        project: Project,
        publicationName: String,
    ) {

        // create a task to publish the publication to the repository
        // e.g create publishMavenPublicationToMavenRepository
        val publishToTask = publishPublicationToMavenRepositoryTaskFactory.createTask(
            project,
            publicationName
        )

        // create a task to zip the publication
        val zipTask = zipPublicationTaskFactory.createZipTask(
            project, publicationName, publishToTask
        )

        // create a task to publish the publication to Sonatype Portal
        developmentBundlePublishTaskFactory.createTask(
            project,
            publicationName,
            zipTask,
            authentication,
            autoPublish
        )

        // Add the zip task to the zipAllPublications task
        // zipAllPublications will execute all the zip tasks
        // e.g zipMavenPublication, zipKotlinMultiplatformPublication, etc.
        project.rootProject.tasks.named(SonatypePortalPublisherPlugin.ZIP_ALL_PUBLICATIONS).configure {
            dependsOn(zipTask)
        }
    }
}