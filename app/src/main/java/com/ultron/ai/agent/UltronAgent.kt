package com.ultron.ai.agent

import android.util.Log
import com.ultron.ai.command.CommandExecutor
import com.ultron.ai.core.capabilities.UltronCapabilities
import com.ultron.ai.core.capabilities.ToolRequirements
import com.ultron.ai.data.remote.ChatEvent
import com.ultron.ai.data.remote.GroqApiException
import com.ultron.ai.data.remote.GroqApiService
import com.ultron.ai.data.remote.dto.ApiMessage
import com.ultron.ai.data.remote.dto.ToolCall
import com.ultron.ai.data.remote.groqJson
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

typealias ULTRONAgent = UltronAgent

/**
 * Ultron answering hands-free: the model may use [AgentTools] while
 * it answers, and is told what each one actually did before it speaks.
 */
class UltronAgent(
    private val groq: GroqApiService,
    private val commands: CommandExecutor,
    private val now: () -> LocalDateTime = LocalDateTime::now,
    /** Where a phrase the model worked out is kept, to be carried out directly next time. */
    private val learned: LearnedCommands? = null,
    /** A fresh snapshot of what works right now; null means nothing is held back. */
    private val capabilities: suspend () -> UltronCapabilities? = { null }
) {

    internal companion object {
        private const val TAG = "UltronAgent"

        /**
         * What a tool call leaves in logcat: the tool and whether it worked.
         */
        fun toolLog(tool: String, done: Boolean): String = "Tool $tool: ${if (done) "done" else "not done"}"

        /**
         * Tools whose own reply is the answer, word for word.
         */
        private val SAYS_ITSELF = setOf("send_message", "call", "reply_message")

        /**
         * Model turns per answer.
         */
        const val MAX_ROUNDS = 4

        /** One retry when Groq rejects a malformed tool call; the next attempt is usually fine. */
        const val TOOL_FORMAT_RETRIES = 1

        const val WORTH_WAITING_MS = 2_500L
        const val WAIT_MARGIN_MS = 150L
    }

    private class Models(var current: String, private val backup: String?) {
        fun fallBack(): Boolean {
            if (backup == null || backup == current) return false
            current = backup
            return true
        }
    }

    data class Settings(val apiKey: String, val model: String, val maxTokens: Int, val backupModel: String? = null)

    /**
     * The reply to [messages], streamed as text. Tool calls in between are
     * carried out silently; only words reach the collector.
     */
    fun reply(settings: Settings, messages: List<ApiMessage>, russian: Boolean = false): Flow<String> = flow {
        val conversation = messages.toMutableList()
        val models = Models(settings.model, settings.backupModel)
        val said = messages.lastOrNull { it.role == "user" }?.content.orEmpty()
        val languageNote = languageNote(said)
        var kit = ToolKit.forText(said)
        val caps = capabilities()
        val done = mutableListOf<Pair<ToolCall, Boolean>>()
        repeat(MAX_ROUNDS) { round ->
            val tools = if (round == MAX_ROUNDS - 1) null else AgentTools.definitions(kit, caps)
            val calls = turn(settings, models, conversation, tools)
            if (calls == null) {
                learnFrom(said, done)
                return@flow
            }
            if (kit != ToolKit.Kit.FULL && calls.any { it.function.name == ToolKit.ESCALATE }) {
                Log.i(TAG, "Model asked for the phone's tools")
                kit = ToolKit.Kit.FULL
                return@repeat
            }
            conversation += ApiMessage(role = "assistant", content = "", toolCalls = calls)
            val own = mutableListOf<String>()
            calls.forEach { call ->
                val blocked = unavailable(call, caps) ?: ungrounded(call, said)
                val (result, ok) = blocked?.let { it to false } ?: carryOut(call, false)
                done += call to ok
                if (blocked == null && call.function.name in SAYS_ITSELF && !result.startsWith("Error:")) own += result
                conversation += ApiMessage(role = "tool", content = result + languageNote, toolCallId = call.id)
            }
            if (own.size == calls.size) {
                emit(own.joinToString(" "))
                learnFrom(said, done)
                return@flow
            }
        }
    }

    private fun learnFrom(said: String, done: List<Pair<ToolCall, Boolean>>) {
        val (call, ok) = done.singleOrNull() ?: return
        if (ok) learned?.learn(said, call.function.name, call.function.arguments)
    }

    private suspend fun FlowCollector<String>.turn(
        settings: Settings,
        models: Models,
        conversation: List<ApiMessage>,
        tools: List<com.ultron.ai.data.remote.dto.ToolDefinition>?
    ): List<ToolCall>? {
        var retries = TOOL_FORMAT_RETRIES
        var waited = false
        while (true) {
            var calls: List<ToolCall>? = null
            var spoke = false
            try {
                groq.streamChat(settings.apiKey, conversation.toList(), models.current, settings.maxTokens, tools)
                    .collect { event ->
                        when (event) {
                            is ChatEvent.Text -> {
                                spoke = true
                                emit(event.token)
                            }
                            is ChatEvent.ToolCalls -> calls = event.calls
                        }
                    }
                return calls
            } catch (e: GroqApiException) {
                val wait = e.retryAfterMs?.takeIf { e.rateLimited && !waited && it <= WORTH_WAITING_MS }
                when {
                    spoke -> throw e
                    wait != null -> {
                        waited = true
                        Log.w(TAG, "Rate limited, waiting ${wait}ms")
                        delay(wait + WAIT_MARGIN_MS)
                    }
                    e.rateLimited && models.fallBack() -> Log.w(TAG, "Rate limited, switching to ${models.current}")
                    e.toolUseFailed && retries-- > 0 -> Log.w(TAG, "Malformed tool call, retrying: ${e.message}")
                    else -> throw e
                }
            }
        }
    }

    /**
     * Guides the model on the language/tone to adopt.
     * Tells the model to reply in Tanglish if Tanglish phrases are used, or English otherwise.
     */
    private fun languageNote(said: String): String {
        val tanglishKeywords = setOf(
            "da", "dei", "pa", "vaa", "poda", "sari", "seri", "enna", "epdi", "pannu",
            "panna", "solla", "sollu", "irukku", "illa", "theriyum", "vanakkam", "aama",
            "kodu", "vaanga", "ponga", "machan", "thala", "bro", "bhai"
        )
        val words = said.lowercase().split(Regex("[^a-zA-Z0-9]+"))
        val isTanglish = words.any { it in tanglishKeywords }
        return if (isTanglish) {
            "\n(Tell the user in natural Tanglish using Latin script.)"
        } else {
            "\n(Tell the user in English.)"
        }
    }

    private fun unavailable(call: ToolCall, caps: UltronCapabilities?): String? {
        val gaps = caps?.let { ToolRequirements.missing(call.function.name, it) }.orEmpty()
        if (gaps.isEmpty()) return null
        return "Error: unavailable right now — ${gaps.joinToString { it.en }}. Nothing was done; " +
            "tell the user what to enable."
    }

    private fun ungrounded(call: ToolCall, said: String): String? {
        if (call.function.name !in SAYS_ITSELF || said.isBlank()) return null
        val text = runCatching {
            groqJson.parseToJsonElement(call.function.arguments).jsonObject["text"]?.jsonPrimitive?.contentOrNull
        }.getOrNull() ?: return null
        if (MessageGrounding.isOwnWords(text, said)) return null
        Log.w(TAG, "Message text not from this request; sent back to the model")
        return MessageGrounding.refusal(said)
    }

    private suspend fun carryOut(call: ToolCall, russian: Boolean): Pair<String, Boolean> {
        val args = try {
            groqJson.parseToJsonElement(call.function.arguments).jsonObject
        } catch (e: SerializationException) {
            return "Error: arguments are not valid JSON (${e.message})" to false
        } catch (e: IllegalArgumentException) {
            return "Error: arguments must be a JSON object (${e.message})" to false
        }
        val result = when (val interpreted = AgentTools.interpret(call.function.name, args, now())) {
            is AgentTools.Call.Invalid -> "Error: ${interpreted.reason}. Nothing was done." to false
            is AgentTools.Call.Answer -> interpreted.text to false
            AgentTools.Call.Escalate -> "The phone's tools are already available." to false
            is AgentTools.Call.Command -> when (val outcome = commands.execute(interpreted.command, false)) {
                is CommandExecutor.Outcome.Reply -> outcome.text to !CommandExecutor.isFailure(outcome.text)
                else -> "Nothing was done: this needs the app on screen." to false
            }
        }
        Log.i(TAG, toolLog(call.function.name, result.second))
        return result
    }
}
