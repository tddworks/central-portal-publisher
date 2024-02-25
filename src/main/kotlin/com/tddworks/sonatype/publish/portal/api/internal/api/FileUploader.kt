package com.tddworks.sonatype.publish.portal.api.internal.api

import okio.Buffer
import java.io.File

interface FileUploader {
    fun uploadFile(
        file: File,
        builder: HttpRequestBuilder.() -> Unit,
    ): String

    companion object
}

class HttpRequestBuilder {
    private val headers = mutableMapOf<String, String>()
    private val parameters = mutableMapOf<String, String>()

    fun addAuthorization(username: String, password: String) {
        val token = "$username:$password".let {
            Buffer().writeUtf8(it).readByteString().base64()
        }
        addHeader("Authorization", "UserToken $token")
    }

    fun addParameter(name: String, value: String) {
        parameters[name] = value
    }

    fun addHeader(name: String, value: String) {
        headers[name] = value
    }

    fun getHeaders(): Map<String, String> {
        return headers
    }

    fun getParameters(): Map<String, String> {
        return parameters
    }
}