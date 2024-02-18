package com.tddworks.sonatype.publish.portal.plugin.tasks

import com.tddworks.sonatype.publish.portal.api.Authentication
import com.tddworks.sonatype.publish.portal.api.PublicationType
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip

/**
 * Provides a task to publish a bundle to the Sonatype Portal.
 * The reason for this class is to provide an easy way for testing purposes.
 * It would be changed in the future.
 */
object BundlePublishTaskProvider {
    fun publishTaskProvider(
        project: Project,
        capitalized: String,
        zipTaskProvider: TaskProvider<Zip>,
        authentication: Authentication,
        autoPublish: Boolean?,
    ): TaskProvider<PublishTask> =
        project.tasks.register(
            taskName(capitalized),
            PublishTask::class.java
        ) {
            inputFile.set(zipTaskProvider.flatMap { it.archiveFile })
            username.set(authentication.username)
            password.set(authentication.password)
            publicationType.set(
                if (autoPublish == true) {
                    PublicationType.AUTOMATIC
                } else {
                    PublicationType.USER_MANAGED
                }
            )
        }

    private fun taskName(capitalized: String) = "publish${capitalized}PublicationToSonatypePortal"
}