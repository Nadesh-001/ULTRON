package com.ultron.ai.vision.barcode

import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device QR and Barcode detection powered by Google ML Kit.
 * Identifies URLs, Wi-Fi setups, contacts, product barcodes, and raw text payloads.
 */
class BarcodeScanner {

    data class ScannedCode(
        val rawValue: String,
        val displayValue: String,
        val format: Int,
        val valueType: Int,
        val isUrl: Boolean,
        val url: String? = null
    )

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_AZTEC,
            Barcode.FORMAT_DATA_MATRIX,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E
        )
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    /**
     * Scans [bitmap] for barcodes or QR codes.
     */
    suspend fun scanBarcodes(bitmap: Bitmap): List<ScannedCode> = suspendCancellableCoroutine { continuation ->
        val image = InputImage.fromBitmap(bitmap, 0)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val results = barcodes.map { barcode ->
                    val isUrl = barcode.valueType == Barcode.TYPE_URL
                    val urlString = if (isUrl) barcode.url?.url else null
                    ScannedCode(
                        rawValue = barcode.rawValue ?: "",
                        displayValue = barcode.displayValue ?: barcode.rawValue ?: "",
                        format = barcode.format,
                        valueType = barcode.valueType,
                        isUrl = isUrl,
                        url = urlString
                    )
                }
                continuation.resume(results)
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
    }

    fun close() {
        scanner.close()
    }
}
