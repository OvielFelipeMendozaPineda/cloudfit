package app.cloudfit.styling.domain

import java.time.Duration

data class StylingPolicy(
    val stuckAfter: Duration = Duration.ofMinutes(10),
    val maxEventLength: Int = 200,
    val listLimit: Int = 50,
)
