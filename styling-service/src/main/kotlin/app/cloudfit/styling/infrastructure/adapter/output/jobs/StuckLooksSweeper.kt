package app.cloudfit.styling.infrastructure.adapter.output.jobs

import app.cloudfit.styling.application.port.input.RecoverStuckLooksUseCase
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class StuckLooksSweeper(
    private val scope: CoroutineScope,
    private val recover: RecoverStuckLooksUseCase,
    private val interval: Duration = 5.minutes,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun start(): Job = scope.launch {
        while (isActive) {
            runCatching { recover.execute() }.onFailure { log.error("Stuck looks sweep failed: {}", it.message) }
            delay(interval)
        }
    }
}
