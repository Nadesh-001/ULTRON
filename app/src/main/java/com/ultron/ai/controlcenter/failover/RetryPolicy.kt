package com.ultron.ai.controlcenter.failover

import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Exponential backoff and retry policy respecting HTTP Retry-After headers.
 */
object RetryPolicy {

    private const val INITIAL_DELAY_MS = 500L
    private const val MAX_DELAY_MS = 5_000L
    private const val BACKOFF_FACTOR = 2.0

    suspend fun executeWithRetry(
        maxAttempts: Int = 2,
        retryAfterSeconds: Int? = null,
        attempt: Int,
        block: suspend () -> Unit
    ) {
        if (attempt >= maxAttempts) return

        val delayMs = if (retryAfterSeconds != null && retryAfterSeconds > 0) {
            (retryAfterSeconds * 1000L).coerceAtMost(MAX_DELAY_MS)
        } else {
            val exponential = INITIAL_DELAY_MS * BACKOFF_FACTOR.pow(attempt.toDouble()).toLong()
            val jitter = Random.nextLong(50, 200)
            min(exponential + jitter, MAX_DELAY_MS)
        }

        delay(delayMs)
        block()
    }
}
