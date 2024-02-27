package com.tddworks.sonatype.publish.portal.plugin

import org.gradle.api.Project
import org.gradle.configurationcache.extensions.capitalized
import java.io.File

interface PublishingBuildRepositoryManager {
    fun createBuildRepository(
        repoName: String,
        project: Project,
    ): File
}

class SonatypePortalPublishingBuildRepositoryManager : PublishingBuildRepositoryManager {
    override fun createBuildRepository(repoName: String, project: Project): File {

        val publishing = project.publishingExtension

        // publications are the different artifacts that can be published
        // e.g. maven, kotlinMultiplatform, etc.
        // https://kotlinlang.org/docs/multiplatform-publish-lib.html#host-requirements
        // This kotlinMultiplatform publication includes metadata artifacts and references the other publications as its variants.
        val sonatypeDestinationPath = project.layout.buildDirectory.dir("sonatype/${repoName}-bundle")

        // Add the Sonatype repository to the publishing block
        // each publication has a task to publish it to a repository

        // This step will create publishMavenPublicationToMavenRepository task
        // And save the publication to the destination path
        publishing.apply {
            repositories.apply {
                // default task name will be - publishMavenPublicationToMavenRepository
                // because the publication name could be different, e.g. maven, kotlinMultiplatform, etc.
                // so we need rename the task to ${capitalized}
                // here we use maven as the repository type and save it to the build folder
                // save to example-multi-modules/module-b/build/sonatype/maven-bundle/
                maven {
                    name = repoName
                    url = project.uri(sonatypeDestinationPath)
                }
            }
        }

        return sonatypeDestinationPath.get().asFile
    }

}