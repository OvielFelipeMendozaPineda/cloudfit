package app.cloudfit.shared.infrastructure.ai

import app.cloudfit.shared.application.ai.AiUsage
import app.cloudfit.shared.application.ai.AiUsageRecorder
import app.cloudfit.shared.application.port.ClockProvider
import app.cloudfit.shared.infrastructure.database.dbQuery
import app.cloudfit.shared.infrastructure.database.toUtc
import java.util.UUID
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object AiUsageTable : Table("ai_usage") {
    val id = uuid("id")
    val userId = uuid("user_id")
    val operation = varchar("operation", 32)
    val model = varchar("model", 100)
    val inputTokens = integer("input_tokens")
    val outputTokens = integer("output_tokens")
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}

class PostgresAiUsageRecorder(private val clock: ClockProvider) : AiUsageRecorder {
    override suspend fun record(usage: AiUsage) {
        dbQuery {
            AiUsageTable.insert {
                it[id] = UUID.randomUUID()
                it[userId] = usage.userId
                it[operation] = usage.operation
                it[model] = usage.model
                it[inputTokens] = usage.inputTokens
                it[outputTokens] = usage.outputTokens
                it[createdAt] = clock.now().toUtc()
            }
        }
    }
}
