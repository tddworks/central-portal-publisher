package com.tddworks.sonatype.publish.portal.plugin.tasks

import com.tddworks.sonatype.publish.portal.api.Authentication
import com.tddworks.sonatype.publish.portal.api.PublicationType
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.configurationcache.extensions.capitalized
import java.io.File

interface DevelopmentBundlePublishTaskFactory {
    fun createTask(
        project: Project,
        publicationName: String,
        dependsOnTask: TaskProvider<Zip>,
        sonatypeBuildRepositoryDirectory: File,
        authentication: Authentication?,
        autoPublish: Boolean?,
    )
}

class SonatypeDevelopmentBundlePublishTaskFactory : DevelopmentBundlePublishTaskFactory {
    override fun createTask(
        project: Project,
        publicationName: String,
        dependsOnTask: TaskProvider<Zip>,
        sonatypeBuildRepositoryDirectory: File,
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

    private fun taskName(publicationName: String) =
        "publish${publicationName.capitalized()}PublicationToSonatypePortalRepository"
}