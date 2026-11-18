package com.ultron.ai.service.voice

import com.ultron.ai.data.remote.GroqApiException
import java.io.IOException

/**
 * Turns a failed answer into something worth hearing.
 *
 * "Something went wrong" was all ULTRON ever said, whatever the cause — so when
 * Groq retired her model there was no way to tell from the phone. Each cause
 * the user can act on gets its own sentence.
 */
object VoiceErrors {

    private const val UNAUTHORISED = 401
    private const val RATE_LIMITED = 429

    fun spoken(e: Throwable, russian: Boolean): String = when {
        e is GroqApiException && e.modelMissing ->
            "Groq changed its models; I've switched over. Please say that again."
        e is GroqApiException && e.code == UNAUTHORISED ->
            "The Groq key was rejected. Please check it in settings."
        e is GroqApiException && e.code == RATE_LIMITED ->
            "Groq asks me to wait: the request limit is used up."
        e is IOException ->
            "I can't reach Groq. Check the connection."
        else ->
            "Groq didn't answer: ${e.message ?: e::class.simpleName}"
    }

    /** Said when the model answered with nothing at all. */
    fun empty(russian: Boolean): String =
        "The model came back empty. Please try again."
}
