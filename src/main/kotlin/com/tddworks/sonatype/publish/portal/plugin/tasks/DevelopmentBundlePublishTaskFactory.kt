package com.tddworks.sonatype.publish.portal.plugin.tasks

import com.tddworks.sonatype.publish.portal.api.Authentication
import com.tddworks.sonatype.publish.portal.api.PublicationType
import com.tddworks.sonatype.publish.portal.plugin.SonatypePortalPublisherPlugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.configurationcache.extensions.capitalized

interface DevelopmentBundlePublishTaskFactory {
    fun createTask(
        project: Project,
        publicationName: String,
        dependsOnTask: TaskProvider<Zip>,
        authentication: Authentication?,
        autoPublish: Boolean?,
    )

    fun createAggregationTask(
        project: Project,
        dependsOnTask: TaskProvider<Zip>,
        authentication: Authentication?,
        autoPublish: Boolean?,
    )
}

class SonatypeDevelopmentBundlePublishTaskFactory : DevelopmentBundlePublishTaskFactory {
    override fun createTask(
        project: Project,
        publicationName: String,
        dependsOnTask: TaskProvider<Zip>,
        authentication: Authentication?,
        autoPublish: Boolean?,
    ) {
        project.tasks.register(
            taskName(publicationName),
            PublishTask::class.java
        ) {
            group = "publishing"
            inputFile.set(dependsOnTask.flatMap { it.archiveFile })
            username.set(authentication?.username)
            password.set(authentication?.password)
            publicationType.set(
                if (autoPublish == true) {
                    PublicationType.AUTOMATIC
                } else {
                    PublicationType.USER_MANAGED
                }
            )
        }
    }

    //TODO maybe we can use the same task for aggregation and publication
    override fun createAggregationTask(
        project: Project,
        dependsOnTask: TaskProvider<Zip>,
        authentication: Authentication?,
        autoPublish: Boolean?,
    ) {
        project.tasks.register(
            SonatypePortalPublisherPlugin.PUBLISH_AGGREGATION_PUBLICATIONS_TO_SONATYPE_PORTAL_REPOSITORY,
            PublishTask::class.java
        ) {
            group = "publishing"
            inputFile.set(dependsOnTask.flatMap { it.archiveFile })
            username.set(authentication?.username)
            password.set(authentication?.password)
            publicationType.set(
                if (autoPublish == true) {
                    PublicationType.AUTOMATIC
                } else {
                    PublicationType.USER_MANAGED
                }
            )
        }
    }

    private fun taskName(publicationName: String) =
        "publish${publicationName.capitalized()}PublicationToSonatypePortalRepository"
}