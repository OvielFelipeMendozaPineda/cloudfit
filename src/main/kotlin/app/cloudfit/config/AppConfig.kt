package app.cloudfit.config

import app.cloudfit.shared.infrastructure.http.RateLimitSettings
import io.ktor.server.config.ApplicationConfig

data class AppConfig(
    val devMode: Boolean,
    val appUrl: String,
    val signupBonus: Int,
    val rateLimits: RateLimitSettings,
) {
    companion object {
        fun from(config: ApplicationConfig): AppConfig {
            val section = config.config("cloudfit")
            return AppConfig(
                devMode = section.property("env").getString().trim().lowercase() == "dev",
                appUrl = section.property("appUrl").getString().trim().removeSuffix("/"),
                signupBonus = section.propertyOrNull("billing.signupBonus")?.getString()?.toIntOrNull() ?: 5,
                rateLimits = RateLimitSettings(
                    authPerMinute = section.property("rateLimit.authPerMinute").getString().toInt(),
                    looksPerMinute = section.property("rateLimit.looksPerMinute").getString().toInt(),
                    trustProxy = section.property("trustProxy").getString().toBooleanStrictOrNull() ?: true,
                ),
            )
        }
    }
}
