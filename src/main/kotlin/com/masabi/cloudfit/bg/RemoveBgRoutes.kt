package com.masabi.cloudfit.bg

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
data class RemoveBgResponse(val photoUrl: String)

fun Application.removeBgRoutes(remover: BackGroundRemover?, imageStore: ImageStore) {
    routing {
        post("/remove-bg") {
            if (remover == null) {
                call.respond(HttpStatusCode.NotImplemented, mapOf("error" to "REMOVE_BG_API_KEY not set"))
                return@post
            }

            var fileBytes: ByteArray? = null
            call.receiveMultipart().forEachPart { part ->
                if (part is PartData.FileItem && fileBytes == null) {
                    fileBytes = part.provider().readRemaining().readByteArray()
                }
                part.dispose()
            }

            val input = fileBytes
            if (input == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "no file part found"))
                return@post
            }

            BackGroundRemoverController(remover).handle(input).fold(
                onSuccess = { png -> call.respond(RemoveBgResponse(imageStore.put(png, "image/png"))) },
                onFailure = { call.respond(HttpStatusCode.BadGateway, mapOf("error" to (it.message ?: "remove-bg failed"))) },
            )
        }
    }
}
