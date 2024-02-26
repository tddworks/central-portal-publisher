package com.tddworks.sonatype.publish.portal.plugin.provider

import com.tddworks.sonatype.publish.portal.api.Authentication
import com.tddworks.sonatype.publish.portal.api.SonatypePublisherSettings
import com.tddworks.sonatype.publish.portal.plugin.*
import com.tddworks.sonatype.publish.portal.plugin.tasks.DevelopmentBundlePublishTaskFactory
import com.tddworks.sonatype.publish.portal.plugin.tasks.PublishPublicationToMavenRepositoryTaskFactory
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension

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
    private val publicationProvider: PublicationProvider,
) : PublishingTaskManager {

    var authentication: Authentication? = null
    var autoPublish: Boolean? = null
    var settings: SonatypePublisherSettings? = null

    override fun registerPublishingTasks(project: Project) {
        // Create a task to zip all publications
        val zipAllPublications = project.tasks.register(SonatypePortalPublisherPlugin.ZIP_ALL_PUBLICATIONS)

        // Create a task to publish all publications to Sonatype Portal
        project.tasks.register(SonatypePortalPublisherPlugin.PUBLISH_ALL_PUBLICATIONS_TO_SONATYPE_PORTAL_REPOSITORY) {
            group = PUBLISHING_GROUP
            // publish all publications depends on zipAllPublications
            dependsOn(zipAllPublications)
        }

        settings?.aggregation?.let {
            registerAggregationPublications(project)
        }

        project.allprojects.forEach { pj ->
            project.addProjectAsRootProjectDependencyIfNecessary(settings?.aggregation, pj)

            pj.pluginManager.withPlugin("maven-publish") {
                registerPublications(pj)
            }
        }
    }

    fun registerPublications(project: Project) {
        // register the zip configuration producer
        project.createZipConfigurationProducer


        // need config this before loop through all publications
        preparePublications(project)

        // central.sonatype.com - Missing signature for file:
        configureSigning(project)

        // make sure the signing task runs before the publishing task
        project.tasks.withType<AbstractPublishToMaven>().configureEach {
            val signingTasks = project.tasks.withType<Sign>()
            mustRunAfter(signingTasks)
        }

        val publishing = project.publishingExtension

        // Configure each publication
        publishing.publications.configureEach {
            project.logger.quiet("Found publication: $name")
            registerTasksForPublication(project, name)
        }
    }

    private fun preparePublications(project: Project) {
        KotlinMultiplatformPublicationProvider().preparePublication(project)
        publicationProvider.preparePublication(project)
    }

    private fun configureSigning(project: Project) {
        project.plugins.apply("signing")
        project.plugins.withId("signing") {
            project.configure<SigningExtension> {
                val publishing = project.extensions.getByName("publishing") as PublishingExtension
                sign(publishing.publications)
            }
        }
    }

    private fun registerAggregationPublications(project: Project) {
        val zipProvider = project.enableZipAggregationPublicationsTaskIfNecessary(settings?.aggregation)
        project.enablePublishAggregationPublicationsTaskIfNecessary(
            settings?.aggregation,
            zipProvider,
            authentication,
            autoPublish
        )
    }

    fun registerTasksForPublication(
        project: Project,
        publicationName: String,
    ) {

        // create a task to publish the publication to the repository
        // e.g create publishMavenPublicationToMavenRepository
        val publishPublicationToTask = publishPublicationToMavenRepositoryTaskFactory.createTask(
            project,
            publicationName
        )

        // create a task to zip the publication
        val zipTask = zipPublicationTaskFactory.createZipTask(
            project,
            publicationName,
            publishPublicationToTask
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

    private fun Project.enablePublishAggregationPublicationsTaskIfNecessary(
        isAggregation: Boolean?,
        zipProvider: TaskProvider<Zip>?,
        authentication: Authentication? = null,
        autoPublish: Boolean? = null,
    ) {
        if (isAggregation == true) {
            logger.quiet("Enabling publishAggregationPublicationsToSonatypePortalRepository task for project: $path")

            //TODO unit test for this
            developmentBundlePublishTaskFactory.createAggregationTask(
                project,
                zipProvider!!,
                authentication,
                autoPublish
            )
        }
    }

    private fun Project.addProjectAsRootProjectDependencyIfNecessary(isAggregation: Boolean?, pj: Project) {
        if (isAggregation == true) {
            logger.quiet("Adding project: ${pj.path} as a dependency to the root project: $path")
            // add the root project as a dependency project to the ZIP_CONFIGURATION_CONSUMER configuration
            dependencies.add(ZIP_CONFIGURATION_CONSUMER, project.dependencies.project(mapOf("path" to pj.path)))
        }
    }

    private fun Project.enableZipAggregationPublicationsTaskIfNecessary(aggregation: Boolean?): TaskProvider<Zip>? {
        if (aggregation == true) {
            createZipConfigurationProducer
            //TODO refactor unit test for this
            return zipPublicationTaskFactory.createZipAggregationPublicationsTask(this)
        }
        return null
    }
}