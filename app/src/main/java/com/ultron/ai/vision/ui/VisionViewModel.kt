package com.ultron.ai.vision.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ultron.ai.vision.ai.VisionAIService
import com.ultron.ai.vision.ai.VisionContextManager
import com.ultron.ai.vision.ai.VisionMode
import com.ultron.ai.vision.barcode.BarcodeScanner
import com.ultron.ai.vision.camera.CameraController
import com.ultron.ai.vision.image.ImageProcessor
import com.ultron.ai.vision.ocr.TextRecognitionService
import com.ultron.ai.vision.translation.TranslationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel orchestrating CameraX capture, ML Kit on-device intelligence,
 * and Groq Vision model streaming in ULTRON Vision AI.
 */
class VisionViewModel(
    application: Application,
    val cameraController: CameraController,
    val textRecognitionService: TextRecognitionService,
    val barcodeScanner: BarcodeScanner,
    val translationService: TranslationService,
    val contextManager: VisionContextManager,
    val visionAIService: VisionAIService
) : AndroidViewModel(application) {

    val state: StateFlow<VisionContextManager.VisualContextState> = contextManager.state

    private var activeAnalysisJob: Job? = null

    fun selectMode(mode: VisionMode) {
        contextManager.setMode(mode)
    }

    /**
     * Captures a frame from CameraX, runs on-device ML Kit detection, and initiates Vision AI analysis.
     */
    fun captureAndAnalyze(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                contextManager.setAnalyzing(true)
                contextManager.setSummary("Capturing high-resolution frame...")
                val bitmap = cameraController.captureImage()
                processBitmap(bitmap)
                onSuccess()
                startAiAnalysis()
            } catch (e: Exception) {
                contextManager.setAnalyzing(false)
                contextManager.setSummary("Capture error: ${e.message}")
            }
        }
    }

    /**
     * Processes an image chosen via Android SAF Photo Picker.
     */
    fun onGalleryImageSelected(uri: Uri, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                contextManager.setAnalyzing(true)
                contextManager.setSummary("Importing image from gallery...")
                val bitmap = ImageProcessor.loadAndNormalizeBitmap(getApplication(), uri)
                if (bitmap != null) {
                    processBitmap(bitmap)
                    onSuccess()
                    startAiAnalysis()
                } else {
                    contextManager.setAnalyzing(false)
                    contextManager.setSummary("Failed to load image from gallery")
                }
            } catch (e: Exception) {
                contextManager.setAnalyzing(false)
                contextManager.setSummary("Import error: ${e.message}")
            }
        }
    }

    private suspend fun processBitmap(bitmap: Bitmap) {
        contextManager.setVisualContext(bitmap)
        contextManager.setSummary("Running on-device ML Kit analysis...")

        // 1. ML Kit OCR
        try {
            val ocr = textRecognitionService.recognizeText(bitmap)
            contextManager.updateOcr(ocr)

            // 2. Language ID if text is present
            if (ocr.fullText.isNotBlank()) {
                val lang = translationService.identifyLanguage(ocr.fullText)
                contextManager.updateLanguage(translationService.getLanguageDisplayName(lang))
            }
        } catch (_: Exception) {}

        // 3. ML Kit Barcode
        try {
            val barcodes = barcodeScanner.scanBarcodes(bitmap)
            contextManager.updateBarcodes(barcodes)
        } catch (_: Exception) {}

        val ocrCount = state.value.ocrResult?.blockCount ?: 0
        val summary = when {
            state.value.detectedBarcodes.isNotEmpty() -> "Barcode/QR Code detected"
            ocrCount > 0 -> "Text detected ($ocrCount blocks)"
            else -> "Visual object ready for analysis"
        }
        contextManager.setSummary(summary)
    }

    /**
     * Sends initial prompt to Vision AI and streams response into the VQA conversation thread.
     */
    fun startAiAnalysis() {
        val currentState = state.value
        val base64 = currentState.base64Jpeg ?: return
        val prompt = visionAIService.buildInitialPrompt(
            mode = currentState.mode,
            ocrText = currentState.ocrResult?.fullText,
            barcodeText = currentState.detectedBarcodes.firstOrNull()?.displayValue,
            detectedLanguage = currentState.identifiedLanguage
        )

        val streamId = UUID.randomUUID().toString()
        contextManager.setAnalyzing(true)
        activeAnalysisJob?.cancel()

        activeAnalysisJob = viewModelScope.launch(Dispatchers.IO) {
            val fullResponse = StringBuilder()
            try {
                visionAIService.analyzeImageStream(base64, prompt).collect { token ->
                    fullResponse.append(token)
                    contextManager.updateAssistantStream(streamId, fullResponse.toString(), isFinished = false)
                }
                contextManager.updateAssistantStream(streamId, fullResponse.toString(), isFinished = true)
            } catch (e: Exception) {
                val err = "AI Analysis Error: ${e.localizedMessage ?: "Unable to complete visual analysis"}"
                contextManager.updateAssistantStream(streamId, err, isFinished = true)
            } finally {
                contextManager.setAnalyzing(false)
            }
        }
    }

    /**
     * Handles follow-up conversational question about the locked image context.
     */
    fun askFollowUp(question: String) {
        val q = question.trim()
        if (q.isBlank() || state.value.isAnalyzing) return

        contextManager.addOperatorMessage(q)
        val currentState = state.value
        val base64 = currentState.base64Jpeg ?: return

        val prompt = visionAIService.buildVqaPrompt(
            userQuestion = q,
            history = currentState.conversation,
            ocrSummary = currentState.ocrResult?.fullText?.take(600)
        )

        val streamId = UUID.randomUUID().toString()
        contextManager.setAnalyzing(true)

        viewModelScope.launch(Dispatchers.IO) {
            val responseBuilder = StringBuilder()
            try {
                visionAIService.analyzeImageStream(base64, prompt).collect { token ->
                    responseBuilder.append(token)
                    contextManager.updateAssistantStream(streamId, responseBuilder.toString(), isFinished = false)
                }
                contextManager.updateAssistantStream(streamId, responseBuilder.toString(), isFinished = true)
            } catch (e: Exception) {
                val err = "Error: ${e.localizedMessage ?: "Failed to generate answer"}"
                contextManager.updateAssistantStream(streamId, err, isFinished = true)
            } finally {
                contextManager.setAnalyzing(false)
            }
        }
    }

    /**
     * Copies extracted OCR text to the system clipboard.
     */
    fun copyOcrToClipboard(): Boolean {
        val text = state.value.ocrResult?.fullText
        if (text.isNullOrBlank()) return false
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("ULTRON OCR", text)
        clipboard?.setPrimaryClip(clip)
        return true
    }

    override fun onCleared() {
        super.onCleared()
        cameraController.stopCamera()
        textRecognitionService.close()
        barcodeScanner.close()
        translationService.close()
    }
}
