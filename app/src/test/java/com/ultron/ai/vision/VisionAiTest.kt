package com.ultron.ai.vision

import com.ultron.ai.domain.usecase.AnalyzeContentUseCase
import com.ultron.ai.vision.ai.VisionAIService
import com.ultron.ai.vision.ai.VisionContextManager
import com.ultron.ai.vision.ai.VisionMode
import com.ultron.ai.vision.translation.TranslationService
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VisionAiTest {

    private val mockAnalyzeContent = mockk<AnalyzeContentUseCase>(relaxed = true)
    private val visionAiService = VisionAIService(mockAnalyzeContent)
    private val contextManager = VisionContextManager()
    private val translationService = TranslationService()

    @Test
    fun `vision modes contain all required Google Lens-inspired modes`() {
        val modes = VisionMode.entries
        assertTrue(modes.any { it == VisionMode.OBJECT })
        assertTrue(modes.any { it == VisionMode.OCR_TEXT })
        assertTrue(modes.any { it == VisionMode.TRANSLATE })
        assertTrue(modes.any { it == VisionMode.MATH_SOLVER })
        assertTrue(modes.any { it == VisionMode.QR_BARCODE })
        assertTrue(modes.any { it == VisionMode.DOCUMENT })
    }

    @Test
    fun `buildInitialPrompt includes OCR text when present`() {
        val prompt = visionAiService.buildInitialPrompt(
            mode = VisionMode.OBJECT,
            ocrText = "INTEL CORE I9 14900K",
            barcodeText = null,
            detectedLanguage = null
        )

        assertTrue(prompt.contains("INTEL CORE I9 14900K"))
        assertTrue(prompt.contains("Identify the primary object"))
    }

    @Test
    fun `buildInitialPrompt for math solver includes STEM directives`() {
        val prompt = visionAiService.buildInitialPrompt(
            mode = VisionMode.MATH_SOLVER,
            ocrText = "integrate x^2 dx",
            barcodeText = null,
            detectedLanguage = null
        )

        assertTrue(prompt.contains("ULTRON STEM & Homework Solver"))
        assertTrue(prompt.contains("step-by-step logical derivation"))
    }

    @Test
    fun `buildVqaPrompt preserves prior thread history`() {
        val history = listOf(
            VisionContextManager.VqaMessage(role = "OPERATOR", text = "What is this?"),
            VisionContextManager.VqaMessage(role = "ULTRON", text = "This is a computer motherboard.")
        )

        val prompt = visionAiService.buildVqaPrompt(
            userQuestion = "Is the PCIe latch intact?",
            history = history,
            ocrSummary = "LGA1700 DDR5"
        )

        assertTrue(prompt.contains("What is this?"))
        assertTrue(prompt.contains("This is a computer motherboard."))
        assertTrue(prompt.contains("Is the PCIe latch intact?"))
        assertTrue(prompt.contains("LGA1700 DDR5"))
    }

    @Test
    fun `contextManager updates messages and handles streaming updates`() {
        contextManager.addOperatorMessage("Explain all ports")
        val state1 = contextManager.state.value
        assertEquals(1, state1.conversation.size)
        assertEquals("OPERATOR", state1.conversation[0].role)
        assertEquals("Explain all ports", state1.conversation[0].text)

        val streamId = "stream-123"
        contextManager.updateAssistantStream(streamId, "Analyzing ports...", isFinished = false)
        val state2 = contextManager.state.value
        assertEquals(2, state2.conversation.size)
        assertTrue(state2.conversation[1].isStreaming)

        contextManager.updateAssistantStream(streamId, "All ports are USB-C and HDMI.", isFinished = true)
        val state3 = contextManager.state.value
        assertEquals(2, state3.conversation.size)
        assertFalse(state3.conversation[1].isStreaming)
        assertEquals("All ports are USB-C and HDMI.", state3.conversation[1].text)
    }

    @Test
    fun `translationService resolves supported languages`() {
        assertEquals("English", translationService.getLanguageDisplayName("en"))
        assertEquals("Spanish", translationService.getLanguageDisplayName("es"))
        assertEquals("French", translationService.getLanguageDisplayName("fr"))
        assertEquals("German", translationService.getLanguageDisplayName("de"))
        assertEquals("Hindi", translationService.getLanguageDisplayName("hi"))
        assertEquals("Tamil", translationService.getLanguageDisplayName("ta"))
        assertTrue(translationService.getLanguageDisplayName("xyz").startsWith("Undetermined"))
    }
}
