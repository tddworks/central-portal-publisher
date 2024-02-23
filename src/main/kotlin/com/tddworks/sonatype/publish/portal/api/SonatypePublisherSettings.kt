package com.tddworks.sonatype.publish.portal.api

/**
 * Settings for the Sonatype Portal Publisher
 * @param autoPublish Publish the module to the Sonatype Portal and Automatically publish to Central Repository
 * @param aggregation Aggregate the module with other modules
 */
data class SonatypePublisherSettings(
    val autoPublish: Boolean? = false,
    val aggregation: Boolean? = false,
)

/**
 * Builder for the Settings
 * @see SonatypePublisherSettings
 */
class SonatypePublisherSettingsBuilder {
    var autoPublish: Boolean? = false
    var aggregation: Boolean? = false

    fun build(): SonatypePublisherSettings {
        return SonatypePublisherSettings(autoPublish, aggregation)
    }
}
