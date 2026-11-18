package com.ultron.ai.core

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import android.util.Log

// ---------------------------------------------------------------------------
// Risk / result types
// ---------------------------------------------------------------------------

enum class RiskLevel { LOW, MEDIUM, HIGH }

enum class AuthResult { APPROVED, DENIED, PENDING_USER_CONFIRMATION }

data class ActionRequest(
    val id: String = UUID.randomUUID().toString(),
    val command: String,
    val riskLevel: RiskLevel,
    val description: String,
    val requiresBiometric: Boolean = false
)

// ---------------------------------------------------------------------------
// ActionAuthorizationManager
// ---------------------------------------------------------------------------

class ActionAuthorizationManager(
    private val ctx: Context,
    private val securityManager: SecurityManager
) {

    companion object {
        private const val TAG = "ActionAuthMgr"
        private const val MAX_HISTORY = 50

        // Keyword sets for classification
        private val LOW_KEYWORDS = setOf(
            "open", "launch", "navigate", "scroll", "search", "play", "read", "show"
        )
        private val MEDIUM_KEYWORDS = setOf(
            "send", "message", "sms", "email", "contact", "share", "settings", "modify", "post"
        )
        private val HIGH_KEYWORDS = setOf(
            "delete", "remove", "pay", "purchase", "buy", "password",
            "bank", "transfer", "credit", "security"
        )
    }

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    /** Pending actions waiting for a user decision. */
    private val _pendingApprovals = MutableStateFlow<List<ActionRequest>>(emptyList())
    val pendingApprovals: StateFlow<List<ActionRequest>> = _pendingApprovals.asStateFlow()

    /** One CompletableDeferred per in-flight action request. */
    private val deferredMap = ConcurrentHashMap<String, CompletableDeferred<AuthResult>>()

    /** Circular history (capped at MAX_HISTORY). */
    private val actionHistory = ArrayDeque<Pair<ActionRequest, AuthResult>>(MAX_HISTORY + 1)

    // -----------------------------------------------------------------------
    // Classification
    // -----------------------------------------------------------------------

    /**
     * Classify a command string into a [RiskLevel].
     *
     * HIGH wins over MEDIUM wins over LOW; if no keyword matches, defaults to MEDIUM
     * as a safe fallback.
     */
    fun classifyCommand(command: String): RiskLevel {
        val lower = command.lowercase()
        return when {
            HIGH_KEYWORDS.any { lower.contains(it) } -> RiskLevel.HIGH
            MEDIUM_KEYWORDS.any { lower.contains(it) } -> RiskLevel.MEDIUM
            LOW_KEYWORDS.any { lower.contains(it) } -> RiskLevel.LOW
            else -> RiskLevel.MEDIUM // unknown intent → treat as MEDIUM
        }
    }

    // -----------------------------------------------------------------------
    // Authorization flow
    // -----------------------------------------------------------------------

    /**
     * Request authorization for [request].
     *
     * - LOW  → auto-approve immediately (logs the event).
     * - MEDIUM → emits to [pendingApprovals]; suspends until [approveAction] /
     *            [denyAction] is called.
     * - HIGH → emits to [pendingApprovals]; suspends until explicit confirmation;
     *           logs the event regardless of outcome.
     *
     * Returns [AuthResult.APPROVED] or [AuthResult.DENIED].
     */
    suspend fun requestAuthorization(request: ActionRequest): AuthResult {
        return when (request.riskLevel) {
            RiskLevel.LOW -> {
                Log.i(TAG, "AUTO-APPROVED [LOW] id=${request.id} cmd='${request.command}'")
                recordHistory(request, AuthResult.APPROVED)
                AuthResult.APPROVED
            }

            RiskLevel.MEDIUM -> {
                Log.i(TAG, "PENDING_USER [MEDIUM] id=${request.id} cmd='${request.command}'")
                val result = suspendForUserDecision(request)
                recordHistory(request, result)
                result
            }

            RiskLevel.HIGH -> {
                Log.w(TAG, "PENDING_USER [HIGH] id=${request.id} cmd='${request.command}'")
                val result = suspendForUserDecision(request)
                // Log HIGH-risk outcome regardless
                Log.w(
                    TAG,
                    "HIGH-RISK outcome=$result id=${request.id} cmd='${request.command}'"
                )
                recordHistory(request, result)
                result
            }
        }
    }

    // -----------------------------------------------------------------------
    // Approve / Deny
    // -----------------------------------------------------------------------

    /** Resolve the suspended coroutine for [id] with [AuthResult.APPROVED]. */
    fun approveAction(id: String) {
        resolve(id, AuthResult.APPROVED)
    }

    /** Resolve the suspended coroutine for [id] with [AuthResult.DENIED]. */
    fun denyAction(id: String) {
        resolve(id, AuthResult.DENIED)
    }

    // -----------------------------------------------------------------------
    // History
    // -----------------------------------------------------------------------

    /** Returns up to the last [MAX_HISTORY] (request, result) pairs. */
    fun getActionHistory(): List<Pair<ActionRequest, AuthResult>> {
        synchronized(actionHistory) {
            return actionHistory.toList()
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Adds [request] to [pendingApprovals], creates a [CompletableDeferred],
     * suspends until it is completed, then cleans up.
     */
    private suspend fun suspendForUserDecision(request: ActionRequest): AuthResult {
        val deferred = CompletableDeferred<AuthResult>()
        deferredMap[request.id] = deferred

        // Surface the action to the UI
        _pendingApprovals.update { it + request }

        return try {
            deferred.await()
        } finally {
            // Always clean up regardless of cancellation
            deferredMap.remove(request.id)
            _pendingApprovals.update { list -> list.filter { it.id != request.id } }
        }
    }

    /** Complete the deferred for [id] with [result]; no-op if unknown. */
    private fun resolve(id: String, result: AuthResult) {
        val deferred = deferredMap[id]
        if (deferred == null) {
            Log.w(TAG, "resolve: no pending deferred for id=$id (already resolved or unknown)")
            return
        }
        deferred.complete(result)
    }

    /** Append to history, trimming to [MAX_HISTORY]. */
    private fun recordHistory(request: ActionRequest, result: AuthResult) {
        synchronized(actionHistory) {
            actionHistory.addLast(request to result)
            while (actionHistory.size > MAX_HISTORY) {
                actionHistory.removeFirst()
            }
        }
    }
}
