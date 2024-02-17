package com.tddworks.sonatype.publish.portal.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

class SonatypePortalPublisherPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = with(project) {
        val extension = extensions.create<SonatypePortalPublisherExtension>(EXTENSION_NAME)

    }
}
