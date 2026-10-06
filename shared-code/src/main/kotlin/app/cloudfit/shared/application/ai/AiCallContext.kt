package app.cloudfit.shared.application.ai

import java.util.UUID
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class AiCallContext(
    val userId: UUID,
    val operation: String,
) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<AiCallContext>
}
