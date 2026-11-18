package com.ultron.ai.vision.ai

import com.ultron.ai.domain.usecase.AnalyzeContentUseCase
import kotlinx.coroutines.flow.Flow

/**
 * AI reasoning engine for ULTRON Vision.
 * Synthesizes domain-specific prompts for Object Recognition, Math Solving, OCR Analysis,
 * Live Translation, Document Intelligence, Barcode Validation, and Interactive Visual Q&A.
 */
class VisionAIService(
    private val analyzeContent: AnalyzeContentUseCase
) {

    /**
     * Builds the foundational vision prompt based on the selected [VisionMode] and ML Kit context.
     */
    fun buildInitialPrompt(
        mode: VisionMode,
        ocrText: String?,
        barcodeText: String?,
        detectedLanguage: String?
    ): String {
        val ocrSection = if (!ocrText.isNullOrBlank()) {
            "\n[Detected On-Device OCR Text]: \"$ocrText\""
        } else ""

        val barcodeSection = if (!barcodeText.isNullOrBlank()) {
            "\n[Scanned Barcode / QR Data]: \"$barcodeText\""
        } else ""

        val langSection = if (!detectedLanguage.isNullOrBlank() && detectedLanguage != "und") {
            "\n[Detected Source Language]: $detectedLanguage"
        } else ""

        return when (mode) {
            VisionMode.OBJECT -> """
                You are ULTRON Vision AI. Analyze this image with high precision.
                1. Identify the primary object(s) or device.
                2. Explain its purpose and core functions.
                3. List important characteristics, specifications, and visible components.
                4. Explain how it operates and any notable observation or damage.
                Keep explanations clear, structured, and technically accurate.
                $ocrSection$barcodeSection
            """.trimIndent()

            VisionMode.OCR_TEXT -> """
                You are ULTRON Optical Document Reader.
                Extract, transcribe, and structure all readable text from this image accurately.
                Preserve formatting and reading order where possible.
                Provide a clean transcription followed by a brief summary of key details.
                $ocrSection
            """.trimIndent()

            VisionMode.TRANSLATE -> """
                You are ULTRON Neural Translator.
                1. Detect the original language of the text in this image.
                2. Translate the text into clean, fluent English (and also provide the original text).
                3. Explain any idiomatic nuances, technical terminology, or context.
                $ocrSection$langSection
            """.trimIndent()

            VisionMode.MATH_SOLVER -> """
                You are ULTRON STEM & Homework Solver.
                Examine this captured problem, equation, or diagram carefully.
                1. State the identified question and subject (Mathematics, Physics, Chemistry, or Engineering).
                2. Provide a step-by-step logical derivation or solution.
                3. Explain the underlying formulas, laws, or theorems used.
                4. State the final answer clearly in a dedicated conclusion.
                $ocrSection
            """.trimIndent()

            VisionMode.QR_BARCODE -> """
                You are ULTRON Barcode Intelligence.
                Analyze the scanned barcode or QR code within this image.
                1. Validate the encoded payload and explain what it links to or represents.
                2. Flag any potential phishing or security concerns if it is a web URL.
                3. Provide actionable suggestions (e.g. open link, add Wi-Fi, save contact).
                $barcodeSection$ocrSection
            """.trimIndent()

            VisionMode.DOCUMENT -> """
                You are ULTRON Document Intelligence.
                Examine this document, book, receipt, form, or handwritten note.
                1. Identify the document type.
                2. Provide a structured summary of key points and clauses.
                3. List actionable items, dates, monetary totals, or commitments if present.
                4. Offer a concise synthesis for quick reference.
                $ocrSection
            """.trimIndent()
        }
    }

    /**
     * Builds follow-up contextual prompt for Visual Q&A threads, preserving prior conversation history.
     */
    fun buildVqaPrompt(
        userQuestion: String,
        history: List<VisionContextManager.VqaMessage>,
        ocrSummary: String?
    ): String {
        val historyContext = if (history.isNotEmpty()) {
            val historyLines = history.takeLast(6).joinToString("\n") { "${it.role}: ${it.text}" }
            "\n[Visual Q&A Conversation History]:\n$historyLines\n"
        } else ""

        val ocrContext = if (!ocrSummary.isNullOrBlank()) {
            "\n[Detected Visual OCR Snippet]: $ocrSummary\n"
        } else ""

        return """
            You are ULTRON Vision Core. You have locked visual context on the captured image.
            Answer the operator's question directly referencing the visual details, spatial layout, ports, components, or text in the image.
            $ocrContext$historyContext
            Operator's Question: "$userQuestion"
        """.trimIndent()
    }

    /**
     * Executes streaming vision reasoning via [AnalyzeContentUseCase].
     */
    suspend fun analyzeImageStream(imageBase64: String, prompt: String): Flow<String> {
        return analyzeContent.analyzeImage(imageBase64, prompt)
    }
}
