package com.tddworks.sonatype.publish.portal.plugin

import com.tddworks.sonatype.publish.portal.api.Authentication
import com.tddworks.sonatype.publish.portal.api.AuthenticationBuilder
import com.tddworks.sonatype.publish.portal.api.internal.ModulesAggregation
import org.gradle.api.Project
import org.gradle.api.provider.Property

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

    val autoPublish: Property<Boolean>

    fun Project.authentication(authentication: AuthenticationBuilder.() -> Unit): Authentication {
        return AuthenticationBuilder().apply(authentication).build()
    }

    fun Project.modules(modules: List<ModulesAggregation>.() -> Unit): List<ModulesAggregation> {
        return mutableListOf<ModulesAggregation>().apply(modules)
    }
}