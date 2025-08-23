package com.tddworks.sonatype.publish.portal.plugin.tasks

import com.tddworks.sonatype.publish.portal.plugin.PublishingBuildRepositoryManager
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.extra

interface PublishPublicationToMavenRepositoryTaskFactory {
    fun createTask(project: Project, publicationName: String): TaskProvider<Task>
}

class SonatypePublishPublicationToMavenRepositoryTaskFactory(
    private val publishingBuildRepositoryManager: PublishingBuildRepositoryManager
) : PublishPublicationToMavenRepositoryTaskFactory {

    companion object {
        const val SONATYPE_BUILD_REPOSITORY_DIRECTORY = "sonatypeBuildRepositoryDirectory"
    }

    override fun createTask(project: Project, publicationName: String): TaskProvider<Task> {

        // Capitalize the publication name
        // e.g. maven -> Maven
        // e.g. kotlinMultiplatform -> KotlinMultiplatform
        val repoName = "Build${publicationName.replaceFirstChar { it.uppercase() }}"

        // create a directory to store the build repository
        // e.g will create publishMavenPublicationToMavenRepository task and save the publication to
        // the
        // destination path
        val sonatypeBuildRepositoryDirectory =
            publishingBuildRepositoryManager.createBuildRepository(repoName, project)

        // reuse the task to publish the publication to the repository
        val tasks = project.tasks
        val publishToTask = tasks.named(taskName(publicationName))

        // remove the destination path before publishing
        publishToTask.configure {
            doFirst {
                sonatypeBuildRepositoryDirectory.apply {
                    deleteRecursively()
                    mkdirs()
                }
            }
            extra.set(SONATYPE_BUILD_REPOSITORY_DIRECTORY, sonatypeBuildRepositoryDirectory)
        }

        return publishToTask
    }

    private fun taskName(publicationName: String): String {
        // publicationName would be like
        // maven, kotlinMultiplatform, jvm,  etc.
        return "publish${publicationName.replaceFirstChar { it.uppercase() }}PublicationToBuild${publicationName.replaceFirstChar { it.uppercase() }}Repository"
    }
}
