package com.masabi.cloudfit.outfit

import com.masabi.cloudfit.ai.OutfitStylist
import com.masabi.cloudfit.avatar.AvatarRepository
import com.masabi.cloudfit.clothes.ClothesRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

@Serializable
data class ComposeOutfitRequest(val event: String)

@Serializable
data class RenderOutfitImageRequest(val avatarUrl: String? = null)

fun Application.outfitRoutes(
    clothes: ClothesRepository,
    outfits: OutfitsRepository,
    avatars: AvatarRepository,
    stylist: OutfitStylist,
) {
    routing {
        post("/outfits") {
            val request = call.receive<ComposeOutfitRequest>()
            val outfit = stylist.pick(request.event, clothes.all())
            call.respond(HttpStatusCode.Created, outfits.save(outfit))
        }

        get("/outfits") { call.respond(outfits.all()) }

        get("/outfits/{id}") {
            val outfit = call.parameters["id"]?.let { outfits.get(it) }
            if (outfit == null) call.respond(HttpStatusCode.NotFound) else call.respond(outfit)
        }

        post("/outfits/{id}/image") {
            val outfit = call.parameters["id"]?.let { outfits.get(it) }
            if (outfit == null) {
                call.respond(HttpStatusCode.NotFound)
                return@post
            }
            val request = call.receive<RenderOutfitImageRequest>()
            val avatarUrl = request.avatarUrl ?: avatars.get()?.photoUrl
            val rendered = stylist.renderImage(outfit, clothes.all(), avatarUrl)
            call.respond(outfits.save(rendered))
        }
    }
}
