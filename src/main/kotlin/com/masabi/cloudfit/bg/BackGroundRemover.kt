package com.masabi.cloudfit.bg

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

fun interface BackGroundRemover {
    suspend fun remove(image: ByteArray): ByteArray
}

class DefaultBackGroundRemover(
    private val apiKey: String,
    private val client: HttpClient,
    private val endpoint: String = "https://api.remove.bg/v1.0/removebg",
) : BackGroundRemover {

    override suspend fun remove(image: ByteArray): ByteArray {
        return client.post(endpoint) {
            header("X-Api-Key", apiKey)
            setBody(MultiPartFormDataContent(formData {
                append("image_file", image, Headers.build {
                    append(HttpHeaders.ContentType, "image/jpeg")
                })
                append("size", "auto")
            }))
        }.body()
    }
}

class BackGroundRemoverController(private val remover: BackGroundRemover) {

    suspend fun handle(inputImage: ByteArray): Result<ByteArray> = runCatching {
        remover.remove(inputImage)
    }
}
