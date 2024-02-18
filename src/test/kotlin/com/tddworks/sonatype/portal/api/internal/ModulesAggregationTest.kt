package com.tddworks.sonatype.portal.api.internal

import com.tddworks.sonatype.publish.portal.api.internal.ModulesAggregation
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Answers
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

class ModulesAggregationTest {

    private lateinit var target: ModulesAggregation
    private lateinit var mockConfiguration: Configuration
    private lateinit var mockProject: Project

    @BeforeEach
    fun setUp() {
        mockConfiguration = mock(Configuration::class.java)
        mockProject = mock(Project::class.java, Answers.RETURNS_DEEP_STUBS)
        target = ModulesAggregation(mockConfiguration, mockProject)
    }

    @Test
    fun `should add dependency project to configuration`() {
        val path = ":some-path"

        target.project(path)

        verify(mockProject.dependencies).add(
            mockConfiguration.name,
            mockProject.dependencies.project(mapOf("path" to path))
        )
    }

}