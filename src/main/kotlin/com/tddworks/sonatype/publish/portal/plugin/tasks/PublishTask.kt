package com.tddworks.sonatype.publish.portal.plugin.tasks

import com.tddworks.sonatype.publish.portal.api.Authentication
import com.tddworks.sonatype.publish.portal.api.DeploymentBundle
import com.tddworks.sonatype.publish.portal.api.PublicationType
import com.tddworks.sonatype.publish.portal.api.SonatypePortalPublisher
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault
abstract class PublishTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputFile: RegularFileProperty

    @get:Input
    abstract val username: Property<String>

    @get:Input
    abstract val password: Property<String>

    @get:Input
    @get:Optional
    abstract val publicationType: Property<PublicationType>



    @TaskAction
    fun taskAction() {
        logger.quiet("Publishing to Sonatype Portal")

        val username = username.get()
        val password = password.get()

        check(username.isNotBlank()) {
            "SonatypePortal: username must not be empty"
        }
        check(password.isNotBlank()) {
            "SonatypePortal: password must not be empty"
        }

        //TODO - inject the SonatypePortalPublisher instead of using the default
        val deploymentId = SonatypePortalPublisher.default().deploy(
            Authentication(
                username,
                password
            ),
            DeploymentBundle(
                inputFile.get().asFile,
                publicationType.orNull?.let { it } ?: PublicationType.USER_MANAGED
            )
        )

        logger.quiet("Deployment ID: $deploymentId")
    }

}