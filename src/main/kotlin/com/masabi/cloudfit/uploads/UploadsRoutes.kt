package com.masabi.cloudfit.uploads

import com.masabi.cloudfit.storage.ImageStore
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.Application
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import kotlinx.serialization.Serializable

@Serializable
data class UploadResponse(val url: String)

fun Application.uploadsRoutes(imageStore: ImageStore) {
    routing {
        post("/uploads") {
            var uploaded: UploadResponse? = null
            call.receiveMultipart().forEachPart { part ->
                if (part is PartData.FileItem && uploaded == null) {
                    val bytes = part.provider().readRemaining().readByteArray()
                    val contentType = part.contentType?.toString() ?: "image/jpeg"
                    uploaded = UploadResponse(imageStore.put(bytes, contentType))
                }
                part.dispose()
            }
            val result = uploaded
            if (result == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "no file part found"))
            } else {
                call.respond(HttpStatusCode.Created, result)
            }
        }
    }
}
