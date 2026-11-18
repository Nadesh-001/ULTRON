package com.ultron.ai.vision.ai

import android.graphics.Bitmap
import com.ultron.ai.vision.barcode.BarcodeScanner
import com.ultron.ai.vision.image.ImageProcessor
import com.ultron.ai.vision.ocr.TextRecognitionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Manages active visual context for ULTRON Vision AI.
 * Locks the current image context so that follow-up questions in the Visual Q&A thread
 * continuously reference the same visual frame.
 */
class VisionContextManager {

    data class VqaMessage(
        val id: String = UUID.randomUUID().toString(),
        val role: String, // "OPERATOR" or "ULTRON"
        val text: String,
        val timestamp: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
        val isStreaming: Boolean = false
    )

    data class VisualContextState(
        val bitmap: Bitmap? = null,
        val base64Jpeg: String? = null,
        val ocrResult: TextRecognitionService.OcrResult? = null,
        val detectedBarcodes: List<BarcodeScanner.ScannedCode> = emptyList(),
        val identifiedLanguage: String? = null,
        val mode: VisionMode = VisionMode.OBJECT,
        val isContextLocked: Boolean = false,
        val conversation: List<VqaMessage> = emptyList(),
        val isAnalyzing: Boolean = false,
        val detectedSummary: String = "Analyzing frame...",
        val componentCallout: String = "COMPONENTS DETECTED"
    )

    private val _state = MutableStateFlow(VisualContextState())
    val state: StateFlow<VisualContextState> = _state.asStateFlow()

    /**
     * Locks in a new captured or selected image.
     */
    fun setVisualContext(
        bitmap: Bitmap,
        mode: VisionMode = _state.value.mode
    ) {
        val base64 = ImageProcessor.toBase64Jpeg(bitmap)
        _state.value = VisualContextState(
            bitmap = bitmap,
            base64Jpeg = base64,
            mode = mode,
            isContextLocked = true,
            conversation = emptyList()
        )
    }

    fun updateOcr(ocr: TextRecognitionService.OcrResult) {
        _state.update {
            it.copy(
                ocrResult = ocr,
                componentCallout = if (ocr.blockCount > 0) "OCR: ${ocr.blockCount} BLOCKS" else "COMPONENTS DETECTED"
            )
        }
    }

    fun updateBarcodes(barcodes: List<BarcodeScanner.ScannedCode>) {
        _state.update { it.copy(detectedBarcodes = barcodes) }
    }

    fun updateLanguage(lang: String) {
        _state.update { it.copy(identifiedLanguage = lang) }
    }

    fun setMode(mode: VisionMode) {
        _state.update { it.copy(mode = mode) }
    }

    fun setAnalyzing(analyzing: Boolean) {
        _state.update { it.copy(isAnalyzing = analyzing) }
    }

    fun setSummary(summary: String) {
        _state.update { it.copy(detectedSummary = summary) }
    }

    /**
     * Adds an Operator (user) message to the VQA thread.
     */
    fun addOperatorMessage(text: String) {
        val msg = VqaMessage(role = "OPERATOR", text = text)
        _state.update { it.copy(conversation = it.conversation + msg) }
    }

    /**
     * Starts or updates an Assistant (Ultron) streaming message.
     */
    fun updateAssistantStream(id: String, text: String, isFinished: Boolean) {
        _state.update { current ->
            val list = current.conversation.toMutableList()
            val existingIndex = list.indexOfFirst { it.id == id }
            val newMsg = VqaMessage(
                id = id,
                role = "ULTRON",
                text = text,
                isStreaming = !isFinished
            )
            if (existingIndex >= 0) {
                list[existingIndex] = newMsg
            } else {
                list.add(newMsg)
            }
            current.copy(conversation = list)
        }
    }

    /**
     * Clears context when exiting Vision or taking a new photo.
     */
    fun clearContext() {
        _state.value = VisualContextState()
    }
}
