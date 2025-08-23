package com.tddworks.sonatype.publish.portal.plugin

import org.gradle.api.Project

inline fun <reified T : Any> Project.configureIfExists(fn: T.() -> Unit) {
    extensions.findByType(T::class.java)?.fn()
}
