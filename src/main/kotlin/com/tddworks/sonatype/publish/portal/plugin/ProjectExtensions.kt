package com.tddworks.sonatype.publish.portal.plugin

import org.gradle.api.Project


internal const val EXTENSION_NAME = "sonatypePortalPublisher"

internal val Project.layoutBuildDir get() = layout.buildDirectory.get().asFile
internal val Project.layoutBuildDirectory get() = layout.buildDirectory

