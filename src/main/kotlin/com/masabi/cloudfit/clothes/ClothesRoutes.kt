package com.masabi.cloudfit.clothes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing

fun Application.clothesRoutes(clothes: ClothesRepository) {
    routing {
        get("/clothes") { call.respond(clothes.all()) }
        post("/clothes") {
            call.respond(HttpStatusCode.Created, clothes.save(call.receive<Clothe>()))
        }
        put("/clothes/{id}") {
            val id = call.parameters["id"]!!
            if (clothes.get(id) == null) {
                call.respond(HttpStatusCode.NotFound)
                return@put
            }
            call.respond(clothes.save(call.receive<Clothe>().copy(id = id)))
        }
        delete("/clothes/{id}") {
            val id = call.parameters["id"]!!
            if (clothes.get(id) == null) {
                call.respond(HttpStatusCode.NotFound)
                return@delete
            }
            clothes.delete(id)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
