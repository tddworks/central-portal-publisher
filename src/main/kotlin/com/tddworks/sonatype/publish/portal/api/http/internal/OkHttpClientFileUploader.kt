package com.tddworks.sonatype.publish.portal.api.http.internal

import com.tddworks.sonatype.publish.portal.api.http.FileUploader
import com.tddworks.sonatype.publish.portal.api.http.HttpRequestBuilder
import okhttp3.Headers.Companion.toHeaders
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File


class OkHttpClientFileUploader : FileUploader {

    override fun uploadFile(file: File, builder: HttpRequestBuilder.() -> Unit): String {
        val builder = HttpRequestBuilder().apply(builder)
        val headers = builder.getHeaders()
        val parameters = builder.getParameters()

        val body = MultipartBody.Builder()
            .addFormDataPart(
                "bundle",
                "publication.zip",
                file.asRequestBody("application/zip".toMediaType())
            )
            .build()

        val urlBuilder = HttpUrl.Builder()
            .scheme("https")
            .host("central.sonatype.com")
            .addPathSegment("api")
            .addPathSegment("v1")
            .addPathSegment("publisher")
            .addPathSegment("upload")

        parameters.forEach { (name, value) ->
            urlBuilder.addQueryParameter(name, value)
        }

        val url = urlBuilder.build()

        return Request.Builder()
            .post(body)
            .headers(headers.toHeaders())
            .url(url)
            .build()
            .let {
                OkHttpClient.Builder()
                    .addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.HEADERS
                    })
                    .build()
                    .newCall(it).execute()
            }.use {
                if (!it.isSuccessful) {
                    error("Cannot publish to maven central (status='${it.code}'): ${it.body?.string()}")
                }
                it.body?.string() ?: ""
            }
    }
}


fun FileUploader.Companion.okHttpClient(): FileUploader {
    return OkHttpClientFileUploader()
}