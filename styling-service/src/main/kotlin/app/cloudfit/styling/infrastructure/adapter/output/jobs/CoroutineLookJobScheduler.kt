package app.cloudfit.styling.infrastructure.adapter.output.jobs

import app.cloudfit.styling.application.port.input.ProcessLookUseCase
import app.cloudfit.styling.application.port.output.LookJobScheduler
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class CoroutineLookJobScheduler(
    private val scope: CoroutineScope,
    private val processLook: ProcessLookUseCase,
) : LookJobScheduler {
    override fun schedule(lookId: UUID) {
        scope.launch { processLook.execute(lookId) }
    }
}
