package com.tddworks.sonatype.portal.api.http

import com.tddworks.sonatype.publish.portal.api.http.HttpRequestBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HttpRequestBuilderTest {

    @Test
    fun `should return correct authorization`() {
        val httpRequestBuilder = HttpRequestBuilder()

        httpRequestBuilder.addAuthorization("some-username", "some-password")

        val headers = httpRequestBuilder.getHeaders()

        assertEquals(headers.size, 1)
        assertEquals(headers["Authorization"], "UserToken c29tZS11c2VybmFtZTpzb21lLXBhc3N3b3Jk")
    }

    @Test
    fun `should return correct parameters`() {
        val httpRequestBuilder = HttpRequestBuilder()

        httpRequestBuilder.addParameter("some-parameter", "some-value")

        val parameters = httpRequestBuilder.getParameters()

        assertEquals(parameters.size, 1)
        assertEquals(parameters["some-parameter"], "some-value")
    }

    @Test
    fun `should return correct headers`() {
        val httpRequestBuilder = HttpRequestBuilder()

        httpRequestBuilder.addHeader("some-header", "some-value")

        val headers = httpRequestBuilder.getHeaders()

        assertEquals(headers.size, 1)
        assertEquals(headers["some-header"], "some-value")
    }
}