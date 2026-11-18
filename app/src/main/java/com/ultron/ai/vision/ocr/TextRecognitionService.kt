package com.ultron.ai.vision.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * High-performance on-device Optical Character Recognition (OCR) powered by Google ML Kit.
 * Extracts formatted blocks, lines, and reading order from captured bitmaps.
 */
class TextRecognitionService {

    data class OcrResult(
        val fullText: String,
        val blockCount: Int,
        val linesCount: Int,
        val blocks: List<String>
    )

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Extracts text asynchronously from a given [Bitmap].
     */
    suspend fun recognizeText(bitmap: Bitmap): OcrResult = suspendCancellableCoroutine { continuation ->
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText: Text ->
                val blocksList = visionText.textBlocks.map { it.text }
                var lineCount = 0
                for (block in visionText.textBlocks) {
                    lineCount += block.lines.size
                }
                continuation.resume(
                    OcrResult(
                        fullText = visionText.text.trim(),
                        blockCount = visionText.textBlocks.size,
                        linesCount = lineCount,
                        blocks = blocksList
                    )
                )
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
    }

    fun close() {
        recognizer.close()
    }
}
