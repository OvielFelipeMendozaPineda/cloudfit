package com.masabi.cloudfit

import com.masabi.cloudfit.config.CloudFitConfig
import com.masabi.cloudfit.wardrobe.wardrobeRoutes
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import kotlinx.serialization.json.Json

// Server host/port come from application.yaml (ktor.deployment); EngineMain reads it.
fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    val config = CloudFitConfig.from(environment.config)
    val components = AppComponents.from(config)

    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; prettyPrint = true })
    }
    install(CallLogging)
    install(CORS) {
        anyHost() // hackathon: let the front (Silvia) call from anywhere
        allowHeader(HttpHeaders.ContentType)
    }

    wardrobeRoutes(components.closet, components.events, components.stylist)
}
