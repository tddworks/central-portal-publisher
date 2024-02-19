package com.tddworks.sonatype.publish.portal.plugin

import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.HasConfigurableAttributes
import org.gradle.api.attributes.Usage


internal const val ZIP_CONFIGURATION_CONSUMER = "zipConfigurationConsumer"
internal const val ZIP_CONFIGURATION_PRODUCER = "zipConfigurationProducer"

internal const val ATTRIBUTE = "com.tddworks.sonatypePortalPublisher"
internal const val ATTRIBUTE_VALUE = "deployment-bundle"
internal const val USAGE_VALUE = "sonatypePortalPublisher"

internal fun HasConfigurableAttributes<*>.configureAttributes(project: Project) {
    attributes {
        attribute(
            Attribute.of(ATTRIBUTE, Named::class.java),
            project.objects.named(Named::class.java, ATTRIBUTE_VALUE)
        )
        attribute(
            Usage.USAGE_ATTRIBUTE,
            project.objects.named(Usage::class.java, USAGE_VALUE)
        )
    }
}
