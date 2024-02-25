package com.tddworks.sonatype.publish.portal.plugin

import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import java.lang.UnsupportedOperationException

inline fun <reified T> Project.configureIfExists(fn: T.() -> Unit) {
    extensions.findByType(T::class.java)?.fn()
}