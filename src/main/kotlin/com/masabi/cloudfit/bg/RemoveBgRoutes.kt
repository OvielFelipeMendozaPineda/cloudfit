package com.masabi.cloudfit.bg

import com.masabi.cloudfit.storage.ImageStore
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

@Serializable
data class RemoveBgRequest(val photoUrl: String)

@Serializable
data class RemoveBgResponse(val photoUrl: String)

fun Application.removeBgRoutes(remover: BackGroundRemover?, imageStore: ImageStore) {
    routing {
        post("/remove-bg") {
            val request = call.receive<RemoveBgRequest>()
            if (remover == null) {
                call.respond(HttpStatusCode.NotImplemented, mapOf("error" to "REMOVE_BG_API_KEY not set"))
                return@post
            }
            val controller = BackGroundRemoverController(remover)
            val input = imageStore.get(request.photoUrl)
            controller.handle(input).fold(
                onSuccess = { png -> call.respond(RemoveBgResponse(imageStore.put(png, "image/png"))) },
                onFailure = { call.respond(HttpStatusCode.BadGateway, mapOf("error" to (it.message ?: "remove-bg failed"))) },
            )
        }
    }
}
