package com.masabi.cloudfit.wardrobe

import com.masabi.cloudfit.outfit.OutfitStylist
import com.masabi.cloudfit.shared.Clothe
import com.masabi.cloudfit.shared.Event
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.wardrobeRoutes(
    closet: ClosetRepository,
    events: EventRepository,
    stylist: OutfitStylist,
) {
    routing {
        get("/health") { call.respond(mapOf("status" to "ok")) }

        get("/clothes") { call.respond(closet.all()) }
        post("/clothes") {
            call.respond(HttpStatusCode.Created, closet.save(call.receive<Clothe>()))
        }

        get("/events") { call.respond(events.all()) }
        post("/events") {
            call.respond(HttpStatusCode.Created, events.save(call.receive<Event>()))
        }

        post("/events/{eventId}/outfit") {
            val event = call.parameters["eventId"]?.let { events.get(it) }
            if (event == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "unknown event"))
                return@post
            }
            val avatarUrl = call.request.queryParameters["avatarUrl"]
            call.respond(stylist.compose(event = event, wardrobe = closet.all(), avatarImageUrl = avatarUrl))
        }
    }
}
