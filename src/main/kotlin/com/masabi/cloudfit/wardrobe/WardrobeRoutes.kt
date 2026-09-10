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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory closet for the hackathon. Swap for RDS/DynamoDB later — this keeps the demo runnable
 * with zero infra.
 */
private val closet = ConcurrentHashMap<String, Clothe>()
private val events = ConcurrentHashMap<String, Event>()

fun Application.wardrobeRoutes() {
    // Called directly, in-process — no HTTP hop between the API and the AI stylist.
    val stylist = OutfitStylist()

    routing {
        get("/health") { call.respond(mapOf("status" to "ok")) }

        // ---- Closet ----
        get("/clothes") { call.respond(closet.values.toList()) }

        post("/clothes") {
            val incoming = call.receive<Clothe>()
            val stored = incoming.copy(id = incoming.id.ifBlank { UUID.randomUUID().toString() })
            closet[stored.id] = stored
            call.respond(HttpStatusCode.Created, stored)
        }

        // ---- Events ----
        get("/events") { call.respond(events.values.toList()) }

        post("/events") {
            val incoming = call.receive<Event>()
            val stored = incoming.copy(id = incoming.id.ifBlank { UUID.randomUUID().toString() })
            events[stored.id] = stored
            call.respond(HttpStatusCode.Created, stored)
        }

        // ---- Outfit generation (calls the stylist directly) ----
        post("/events/{eventId}/outfit") {
            val eventId = call.parameters["eventId"]
            val event = eventId?.let { events[it] }
            if (event == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "unknown event"))
                return@post
            }
            val outfit = stylist.compose(event = event, wardrobe = closet.values.toList())
            call.respond(outfit)
        }
    }
}
