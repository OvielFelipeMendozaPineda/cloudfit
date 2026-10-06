package app.cloudfit.shared.infrastructure.database

import io.ktor.server.config.ApplicationConfig

data class DatabaseConfig(
    val host: String,
    val port: Int,
    val name: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int,
) {
    val jdbcUrl: String get() = "jdbc:postgresql://$host:$port/$name"

    companion object {
        fun from(config: ApplicationConfig): DatabaseConfig {
            val section = config.config("cloudfit.database")
            return DatabaseConfig(
                host = section.property("host").getString(),
                port = section.property("port").getString().toInt(),
                name = section.property("name").getString(),
                user = section.property("user").getString(),
                password = section.property("password").getString(),
                maxPoolSize = section.propertyOrNull("maxPoolSize")?.getString()?.toIntOrNull() ?: 10,
            )
        }
    }
}
