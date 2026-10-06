package app.cloudfit.shared.infrastructure.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory
import org.jetbrains.exposed.sql.DatabaseConfig as ExposedDatabaseConfig

object DatabaseFactory {
    private val log = LoggerFactory.getLogger(DatabaseFactory::class.java)

    fun connect(config: DatabaseConfig): AutoCloseable {
        require(config.host.isNotBlank()) { "DB_HOST is not set — the app requires PostgreSQL to run." }
        val dataSource = HikariDataSource(
            HikariConfig().apply {
                driverClassName = "org.postgresql.Driver"
                jdbcUrl = config.jdbcUrl
                username = config.user
                password = config.password
                maximumPoolSize = config.maxPoolSize
                minimumIdle = 1
                isAutoCommit = false
                transactionIsolation = "TRANSACTION_READ_COMMITTED"
                validate()
            },
        )
        migrate(dataSource)
        Database.connect(dataSource, databaseConfig = ExposedDatabaseConfig { defaultMaxAttempts = 1 })
        log.info("PostgreSQL connected at {}:{}/{}", config.host, config.port, config.name)
        return dataSource
    }

    fun migrate(dataSource: DataSource) {
        val result = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .migrate()
        log.info("Flyway applied {} migration(s)", result.migrationsExecuted)
    }
}
