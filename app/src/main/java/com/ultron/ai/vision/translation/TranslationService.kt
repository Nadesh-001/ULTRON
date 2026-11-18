package com.ultron.ai.vision.translation

import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * On-device language identification and translation orchestrator for ULTRON Vision.
 */
class TranslationService {

    private val identifier: LanguageIdentifier = LanguageIdentification.getClient()

    /**
     * Identifies the language code of the extracted OCR text (e.g. "en", "es", "ta", "hi", "ru", "und").
     */
    suspend fun identifyLanguage(text: String): String = suspendCancellableCoroutine { continuation ->
        if (text.isBlank()) {
            continuation.resume("und")
            return@suspendCancellableCoroutine
        }
        identifier.identifyLanguage(text)
            .addOnSuccessListener { languageCode ->
                continuation.resume(languageCode)
            }
            .addOnFailureListener {
                continuation.resume("und")
            }
    }

    /**
     * Resolves user-friendly language name from ISO code.
     */
    fun getLanguageDisplayName(code: String): String = when (code.lowercase()) {
        "en" -> "English"
        "es" -> "Spanish"
        "fr" -> "French"
        "de" -> "German"
        "hi" -> "Hindi"
        "ta" -> "Tamil"
        "ru" -> "Russian"
        "zh" -> "Chinese"
        "ja" -> "Japanese"
        "ar" -> "Arabic"
        "it" -> "Italian"
        "pt" -> "Portuguese"
        else -> "Undetermined ($code)"
    }

    fun close() {
        identifier.close()
    }
}
