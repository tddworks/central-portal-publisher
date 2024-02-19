package com.tddworks.sonatype.publish.portal.plugin

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.getByType


internal const val EXTENSION_NAME = "sonatypePortalPublisher"

internal val Project.layoutBuildDir get() = layout.buildDirectory.get().asFile
internal val Project.layoutBuildDirectory get() = layout.buildDirectory
internal val Project.publishingExtension get() = extensions.getByType<PublishingExtension>()
internal val Project.sonatypePortalPublisherExtension get() = extensions.getByType<SonatypePortalPublisherExtension>()

internal val Project.createZipConfigurationConsumer
    get() = configurations.create(ZIP_CONFIGURATION_CONSUMER) {
        isCanBeResolved = true
        isCanBeConsumed = false
        configureAttributes(project)
    }

internal val Project.createZipConfigurationProducer
    get() = configurations.create(ZIP_CONFIGURATION_PRODUCER) {
        isCanBeConsumed = true
        isCanBeResolved = false
        configureAttributes(project)
    }
