package com.masabi.cloudfit.bg

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

@Serializable
data class RemoveBgRequest(val photoUrl: String)

fun Application.removeBgRoutes() {
    routing {
        post("/remove-bg") {
            call.receive<RemoveBgRequest>()
            // TODO paso 3: llamar a remove.bg (REMOVE_BG_API_KEY), subir el resultado y devolver su photoUrl.
            call.respond(HttpStatusCode.NotImplemented, mapOf("error" to "remove-bg pending step 3 (remove.bg API key)"))
        }
    }
}
