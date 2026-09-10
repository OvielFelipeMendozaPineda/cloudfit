package com.masabi.cloudfit.avatar

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing

fun Application.avatarRoutes(avatars: AvatarRepository) {
    routing {
        get("/avatar") {
            val avatar = avatars.get()
            if (avatar == null) call.respond(HttpStatusCode.NotFound) else call.respond(avatar)
        }
        post("/avatar") {
            call.respond(HttpStatusCode.Created, avatars.set(call.receive<Avatar>()))
        }
        put("/avatar") {
            call.respond(avatars.set(call.receive<Avatar>()))
        }
        delete("/avatar") {
            avatars.clear()
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
