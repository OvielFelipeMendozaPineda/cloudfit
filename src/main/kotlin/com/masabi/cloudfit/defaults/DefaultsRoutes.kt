package com.masabi.cloudfit.defaults

import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.defaultsRoutes() {
    routing {
        get("/default/clothes") { call.respond(Defaults.clothes) }
        get("/default/avatars") { call.respond(Defaults.avatars) }
    }
}
