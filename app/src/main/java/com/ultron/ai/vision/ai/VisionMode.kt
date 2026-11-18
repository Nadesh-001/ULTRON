package com.ultron.ai.vision.ai

/**
 * Visual intelligence modes supported by ULTRON Vision AI.
 * Transcribed from official ULTRON Vision Camera design specs.
 */
enum class VisionMode(val displayName: String, val tag: String) {
    OBJECT("Object", "OBJECT"),
    OCR_TEXT("OCR Text", "OCR TEXT"),
    TRANSLATE("Translate", "TRANSLATE"),
    MATH_SOLVER("Math", "MATH SOLVER"),
    QR_BARCODE("QR Code", "QR / BARCODE"),
    DOCUMENT("Document", "DOCUMENT")
}
