package com.masabi.cloudfit.tagger

import com.masabi.cloudfit.ai.ClotheTagger
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

fun Application.taggerRoutes(tagger: ClotheTagger) {
    routing {
        post("/tagger") {
            var fileBytes: ByteArray? = null
            var contentType = "image/jpeg"
            call.receiveMultipart().forEachPart { part ->
                if (part is PartData.FileItem && fileBytes == null) {
                    contentType = part.contentType?.toString() ?: "image/jpeg"
                    fileBytes = part.provider().readRemaining().readByteArray()
                }
                part.dispose()
            }

            val bytes = fileBytes
            if (bytes == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "no file part found"))
                return@post
            }

            runCatching { tagger.tag(bytes, contentType) }.fold(
                onSuccess = { call.respond(HttpStatusCode.OK, it) },
                onFailure = { call.respond(HttpStatusCode.BadGateway, mapOf("error" to (it.message ?: "tagging failed"))) },
            )
        }
    }
}
