package com.ultron.ai.core.pdf

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

/**
 * Automates PDF workflows via https://www.ilovepdf.com/
 *
 * Capabilities:
 * - Merge PDF (merge, combine)
 * - Split PDF (split, extract)
 * - Compress PDF (compress, reduce)
 * - PDF to Office (pdf_to_word, pdf_to_excel, pdf_to_powerpoint)
 * - Office to PDF (word_to_pdf, excel_to_pdf, powerpoint_to_pdf)
 * - PDF to/from JPG (pdf_to_jpg, jpg_to_pdf)
 * - Sign PDF (sign, signature)
 * - Watermark PDF (watermark)
 * - Rotate PDF (rotate)
 * - Protect & Unlock PDF (protect, encrypt, unlock)
 * - Organize & Repair PDF (organize, repair)
 * - OCR PDF (ocr)
 * - Page Numbers (page_number)
 */
class ILovePdfAutomator(private val context: Context) {

    companion object {
        const val BASE_URL = "https://www.ilovepdf.com"

        val ACTION_URL_MAP = mapOf(
            "merge" to "$BASE_URL/merge_pdf",
            "combine" to "$BASE_URL/merge_pdf",
            "split" to "$BASE_URL/split_pdf",
            "extract" to "$BASE_URL/split_pdf",
            "compress" to "$BASE_URL/compress_pdf",
            "reduce" to "$BASE_URL/compress_pdf",
            "pdf_to_word" to "$BASE_URL/pdf_to_word",
            "word_to_pdf" to "$BASE_URL/word_to_pdf",
            "pdf_to_powerpoint" to "$BASE_URL/pdf_to_powerpoint",
            "powerpoint_to_pdf" to "$BASE_URL/powerpoint_to_pdf",
            "pdf_to_excel" to "$BASE_URL/pdf_to_excel",
            "excel_to_pdf" to "$BASE_URL/excel_to_pdf",
            "pdf_to_jpg" to "$BASE_URL/pdf_to_jpg",
            "jpg_to_pdf" to "$BASE_URL/jpg_to_pdf",
            "image_to_pdf" to "$BASE_URL/jpg_to_pdf",
            "sign" to "$BASE_URL/sign-pdf",
            "signature" to "$BASE_URL/sign-pdf",
            "watermark" to "$BASE_URL/watermark-pdf",
            "rotate" to "$BASE_URL/rotate_pdf",
            "protect" to "$BASE_URL/protect-pdf",
            "encrypt" to "$BASE_URL/protect-pdf",
            "unlock" to "$BASE_URL/unlock_pdf",
            "organize" to "$BASE_URL/organize-pdf",
            "repair" to "$BASE_URL/repair-pdf",
            "ocr" to "$BASE_URL/ocr-pdf",
            "page_number" to "$BASE_URL/page_number-pdf"
        )
    }

    /**
     * Launch automated iLovePDF capability workflow for [action] and optional [fileHint].
     */
    fun automate(action: String, fileHint: String? = null): String {
        val key = action.trim().lowercase().replace(' ', '_').replace('-', '_')
        val targetUrl = ACTION_URL_MAP[key] ?: if (key.isEmpty() || key == "home" || key == "open") {
            BASE_URL
        } else {
            "$BASE_URL/$key"
        }
        val humanAction = if (key.isEmpty() || key == "home") "tools" else key.replace('_', ' ')

        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            if (fileHint.isNullOrBlank()) {
                "Ultron: Launched iLovePDF automated $humanAction workflow ($targetUrl)."
            } else {
                "Ultron: Launched iLovePDF automated $humanAction workflow for '$fileHint' ($targetUrl)."
            }
        } catch (e: Exception) {
            Log.e("ILovePdfAutomator", "Failed to launch iLovePDF: ${e.message}")
            "Failed to launch iLovePDF automation: ${e.message}"
        }
    }
}
