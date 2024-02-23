package com.tddworks.sonatype.publish.portal.plugin

import com.tddworks.sonatype.publish.portal.api.Authentication
import com.tddworks.sonatype.publish.portal.api.AuthenticationBuilder
import com.tddworks.sonatype.publish.portal.api.SonatypePublisherSettings
import com.tddworks.sonatype.publish.portal.api.SonatypePublisherSettingsBuilder
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
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
open class SonatypePortalPublisherExtension(objects: ObjectFactory) {
    /**
     * The authentication property.
     * This property is used to authenticate the user to the Sonatype Portal.
     */
    private val authentication: Property<Authentication> = objects.property(Authentication::class.java)


    /**
     * The settings property.
     * This property is used to configure the settings.
     */
    private val sonatypePublisherSettings: Property<SonatypePublisherSettings> = objects.property(SonatypePublisherSettings::class.java)

    /**
     * Configures the authentication.
     * This method is used to configure the authentication.
     * The authentication is used to authenticate the user to the Sonatype Portal.
     * @param authentication The authentication configuration.
     */
    fun Project.authentication(authentication: AuthenticationBuilder.() -> Unit) {
        this@SonatypePortalPublisherExtension.authentication.setAndFinalize(
            AuthenticationBuilder().apply(authentication).build()
        )
    }

    fun getAuthentication(): Authentication? {
        return authentication.orNull
    }

    /**
     * Configures the settings.
     * This method is used to configure the settings.
     * The settings are used to configure the Sonatype Portal Publisher plugin.
     * @param settings The settings configuration.
     * @see SonatypePublisherSettings
     */
    fun Project.settings(settings: SonatypePublisherSettingsBuilder.() -> Unit) {
        this@SonatypePortalPublisherExtension.sonatypePublisherSettings.setAndFinalize(SonatypePublisherSettingsBuilder().apply(settings).build())
    }

    fun getSettings(): SonatypePublisherSettings? {
        return sonatypePublisherSettings.orNull
    }


    /**
     * Extension for the set and finalize method for the property.
     */
    private fun <T> Property<T>.setAndFinalize(value: T) {
        this.set(value)
        this.finalizeValue()
    }
}