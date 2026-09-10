package com.masabi.cloudfit

import com.masabi.cloudfit.avatar.avatarRoutes
import com.masabi.cloudfit.bg.removeBgRoutes
import com.masabi.cloudfit.clothes.clothesRoutes
import com.masabi.cloudfit.config.CloudFitConfig
import com.masabi.cloudfit.defaults.defaultsRoutes
import com.masabi.cloudfit.outfit.outfitRoutes
import com.masabi.cloudfit.tagger.taggerRoutes
import com.masabi.cloudfit.uploads.uploadsRoutes
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    val config = CloudFitConfig.from(environment.config)
    val components = AppComponents.from(config)

    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; prettyPrint = true })
    }
    install(CallLogging)
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    routing {
        get("/health") { call.respond(mapOf("status" to "ok")) }
    }
    clothesRoutes(components.clothes)
    avatarRoutes(components.avatars)
    defaultsRoutes()
    removeBgRoutes(components.backgroundRemover, components.imageStore)
    uploadsRoutes(components.imageStore)
    taggerRoutes(components.tagger)
    outfitRoutes(components.clothes, components.outfits, components.avatars, components.stylist)
}
