package com.masabi.cloudfit.outfit

import com.masabi.cloudfit.ai.OutfitStylist
import com.masabi.cloudfit.avatar.AvatarRepository
import com.masabi.cloudfit.clothes.ClothesRepository
import com.masabi.cloudfit.storage.ImageStore
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.util.Base64
import kotlinx.serialization.Serializable

@Serializable
data class ComposeOutfitRequest(val event: String)

fun Application.outfitRoutes(
    clothes: ClothesRepository,
    outfits: OutfitsRepository,
    avatars: AvatarRepository,
    stylist: OutfitStylist,
    imageStore: ImageStore,
) {
    routing {
        // Generate + render, WITHOUT persisting: nothing hits S3 or RDS.
        // The image comes back inline as a data URL for the GUI to preview.
        post("/outfits") {
            val request = call.receive<ComposeOutfitRequest>()
            val wardrobe = clothes.all()
            val outfit = stylist.pick(request.event, wardrobe)
            val avatarUrl = avatars.get()?.photoUrl
            call.respond(stylist.renderImage(outfit, wardrobe, avatarUrl))
        }

        // Persist only when the user hits Save: upload the inline image to S3, then store in RDS.
        post("/outfits/save") {
            val outfit = call.receive<Outfit>()
            val persisted = outfit.uploadInlineImage(imageStore)
            call.respond(HttpStatusCode.Created, outfits.save(persisted))
        }

        get("/outfits") { call.respond(outfits.all()) }

        get("/outfits/{id}") {
            val outfit = call.parameters["id"]?.let { outfits.get(it) }
            if (outfit == null) call.respond(HttpStatusCode.NotFound) else call.respond(outfit)
        }
    }
}

/** If [Outfit.imageUrl] is an inline `data:` URL, upload it to the store and swap in the real URL. */
private fun Outfit.uploadInlineImage(imageStore: ImageStore): Outfit {
    val url = imageUrl ?: return this
    if (!url.startsWith("data:")) return this
    val mime = url.substringAfter("data:").substringBefore(";")
    val base64 = url.substringAfter("base64,")
    val stored = imageStore.put(Base64.getDecoder().decode(base64), mime)
    return copy(imageUrl = stored)
}
