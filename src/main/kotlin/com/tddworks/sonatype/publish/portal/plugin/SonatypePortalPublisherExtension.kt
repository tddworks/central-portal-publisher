package com.tddworks.sonatype.publish.portal.plugin

import com.tddworks.sonatype.publish.portal.api.Authentication
import com.tddworks.sonatype.publish.portal.api.AuthenticationBuilder
import com.tddworks.sonatype.publish.portal.api.internal.ModulesAggregation
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.getByType

/**
 * Extension for the Sonatype Portal Publisher plugin.
 * This extension is used to configure the Sonatype Portal Publisher plugin.
 * The extension can be configured in the build.gradle file.
 * The extension can be used to configure the authentication, auto publish and modules.
 * The authentication is used to authenticate the user to the Sonatype Portal.
 * The auto publish is used to publish the modules automatically.
 * The modules are used to specify the modules to be published.
 * The modules are specified as a list of projects.
 */
interface SonatypePortalPublisherExtension {

    /**
     * The auto publish property.
     * This property is used to publish the modules automatically.
     * The default value is false.
     */
    val autoPublish: Property<Boolean>

    /**
     * The authentication property.
     * This property is used to authenticate the user to the Sonatype Portal.
     */
    val authenticationProp: Property<Authentication>

    /**
     * The modules property.
     * This property is used to specify the modules to be published.
     */
    val modulesProp: ListProperty<ModulesAggregation>

    /**
     * Configures the authentication.
     * This method is used to configure the authentication.
     * The authentication is used to authenticate the user to the Sonatype Portal.
     * @param authentication The authentication configuration.
     */
    fun Project.authentication(authentication: AuthenticationBuilder.() -> Unit) {
        authenticationProp.setAndFinalize(AuthenticationBuilder().apply(authentication).build())
    }

    /**
     * Configures the modules.
     * This method is used to configure the modules.
     * The modules are used to specify the modules to be published.
     * @param modules The modules configuration.
     */
    fun Project.modules(modules: List<ModulesAggregation>.() -> Unit) {
        modulesProp.set(mutableListOf<ModulesAggregation>().apply(modules))
        modulesProp.finalizeValue()
    }

    /**
     * Extension for the set and finalize method for the property.
     */
    private fun <T> Property<T>.setAndFinalize(value: T) {
        this.set(value)
        this.finalizeValue()
    }
}

internal val Project.publishingExtension get() = extensions.getByType<PublishingExtension>()