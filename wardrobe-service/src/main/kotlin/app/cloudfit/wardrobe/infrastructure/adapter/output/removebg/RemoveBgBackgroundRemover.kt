package app.cloudfit.wardrobe.infrastructure.adapter.output.removebg

import app.cloudfit.wardrobe.application.port.output.BackgroundRemover
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess

class RemoveBgBackgroundRemover(
    private val apiKey: String,
    private val client: HttpClient,
    private val endpoint: String = "https://api.remove.bg/v1.0/removebg",
) : BackgroundRemover {

    override suspend fun remove(image: ByteArray, contentType: String): ByteArray {
        val response = client.post(endpoint) {
            header("X-Api-Key", apiKey)
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            "image_file",
                            image,
                            Headers.build {
                                append(HttpHeaders.ContentType, contentType)
                                append(HttpHeaders.ContentDisposition, "filename=\"garment\"")
                            },
                        )
                        append("size", "auto")
                    },
                ),
            )
        }
        if (!response.status.isSuccess()) {
            error("remove.bg ${response.status}: ${response.bodyAsText().take(300)}")
        }
        return response.body()
    }
}
