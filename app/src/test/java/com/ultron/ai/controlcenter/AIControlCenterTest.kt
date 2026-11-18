package com.ultron.ai.controlcenter

import android.content.Context
import android.content.SharedPreferences
import com.ultron.ai.controlcenter.core.AIModelRouter
import com.ultron.ai.controlcenter.core.ModelCapability
import com.ultron.ai.controlcenter.data.AIControlCenterPreferences
import com.ultron.ai.controlcenter.data.AIUsageDao
import com.ultron.ai.controlcenter.data.AIUsageEntity
import com.ultron.ai.controlcenter.failover.ErrorClassifier
import com.ultron.ai.controlcenter.usage.UsageTracker
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.SocketTimeoutException

class AIControlCenterTest {

    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var mockContext: Context
    private lateinit var preferences: AIControlCenterPreferences
    private lateinit var router: AIModelRouter
    private lateinit var mockUsageDao: AIUsageDao
    private lateinit var usageTracker: UsageTracker

    @Before
    fun setup() {
        mockPrefs = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)
        mockContext = mockk(relaxed = true)
        mockUsageDao = mockk(relaxed = true)

        every { mockContext.getSharedPreferences(any(), any()) } returns mockPrefs
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.putInt(any(), any()) } returns mockEditor

        every { mockPrefs.getBoolean("auto_model_switching", true) } returns true
        every { mockPrefs.getBoolean("smart_model_routing", true) } returns true
        every { mockPrefs.getBoolean("rate_limit_monitoring", true) } returns true
        every { mockPrefs.getInt("max_retries", 2) } returns 2
        every { mockPrefs.getString("primary_chat_model", any()) } returns "llama-3.3-70b-versatile"
        every { mockPrefs.getString("backup_chat_model", any()) } returns "llama-3.1-8b-instant"
        every { mockPrefs.getString("vision_model", any()) } returns "llama-3.2-90b-vision-preview"
        every { mockPrefs.getString("voice_model", any()) } returns "whisper-large-v3-turbo"

        preferences = AIControlCenterPreferences(mockContext)
        router = AIModelRouter(preferences)
        usageTracker = UsageTracker(mockUsageDao)
    }

    @Test
    fun `errorClassifier classifies 429 as recoverable rate limit`() {
        val analysis = ErrorClassifier.classify(null, 429)
        assertEquals(ErrorClassifier.ErrorCategory.RATE_LIMITED, analysis.category)
        assertTrue(analysis.isRecoverable)
    }

    @Test
    fun `errorClassifier classifies 503 as recoverable server failure`() {
        val analysis = ErrorClassifier.classify(null, 503)
        assertEquals(ErrorClassifier.ErrorCategory.SERVICE_UNAVAILABLE, analysis.category)
        assertTrue(analysis.isRecoverable)
    }

    @Test
    fun `errorClassifier classifies socket timeout as recoverable network error`() {
        val analysis = ErrorClassifier.classify(SocketTimeoutException("Read timed out"), null)
        assertEquals(ErrorClassifier.ErrorCategory.NETWORK_TIMEOUT, analysis.category)
        assertTrue(analysis.isRecoverable)
    }

    @Test
    fun `errorClassifier classifies 401 as non-recoverable authentication error`() {
        val analysis = ErrorClassifier.classify(null, 401)
        assertEquals(ErrorClassifier.ErrorCategory.AUTHENTICATION_FAIL, analysis.category)
        assertFalse(analysis.isRecoverable)
    }

    @Test
    fun `router selects primary model for normal chat`() {
        val model = router.selectModel(AIModelRouter.RequestType.NORMAL_CHAT)
        assertEquals("llama-3.3-70b-versatile", model)
    }

    @Test
    fun `router selects vision model for image vision request`() {
        val model = router.selectModel(AIModelRouter.RequestType.IMAGE_VISION)
        assertEquals("llama-3.2-90b-vision-preview", model)
    }

    @Test
    fun `router selects audio transcription model for audio request`() {
        val model = router.selectModel(AIModelRouter.RequestType.AUDIO_TRANSCRIPTION)
        assertEquals("whisper-large-v3-turbo", model)
    }

    @Test
    fun `router prevents switching image vision request to text-only model`() {
        // Text-only model should not be compatible for image vision
        assertFalse(router.isCompatibleForFailover("llama-3.1-8b-instant", AIModelRouter.RequestType.IMAGE_VISION))
        // Vision-capable model should be compatible
        assertTrue(router.isCompatibleForFailover("llama-3.2-90b-vision-preview", AIModelRouter.RequestType.IMAGE_VISION))
    }

    @Test
    fun `router prevents switching tool request to model without tool calling capability`() {
        // llama-3.1-8b-instant lacks tool-calling in registry
        assertFalse(router.isCompatibleForFailover("llama-3.1-8b-instant", AIModelRouter.RequestType.TOOL_EXECUTION))
        // llama-3.3-70b-versatile has tool-calling
        assertTrue(router.isCompatibleForFailover("llama-3.3-70b-versatile", AIModelRouter.RequestType.TOOL_EXECUTION))
    }

    @Test
    fun `getFailoverModel returns null when automatic switching is disabled`() {
        every { mockPrefs.getBoolean("auto_model_switching", true) } returns false
        val customPrefs = AIControlCenterPreferences(mockContext)
        val customRouter = AIModelRouter(customPrefs)

        val failover = customRouter.getFailoverModel("llama-3.3-70b-versatile", AIModelRouter.RequestType.NORMAL_CHAT)
        assertNull(failover)
    }

    @Test
    fun `usageTracker computes summary metrics accurately from local records`() = runTest {
        val sampleRecords = listOf(
            AIUsageEntity(
                requestId = "req-1",
                modelId = "llama-3.3-70b-versatile",
                inputTokens = 100,
                outputTokens = 50,
                totalTokens = 150,
                httpStatus = 200,
                isSuccess = true
            ),
            AIUsageEntity(
                requestId = "req-2",
                modelId = "llama-3.3-70b-versatile",
                inputTokens = 200,
                outputTokens = 100,
                totalTokens = 300,
                httpStatus = 429,
                isSuccess = false,
                isFailover = true
            )
        )

        coEvery { mockUsageDao.getUsageSince(any()) } returns sampleRecords

        val summary = usageTracker.getSummary(0L)
        assertEquals(2, summary.totalRequests)
        assertEquals(300, summary.totalInputTokens)
        assertEquals(150, summary.totalOutputTokens)
        assertEquals(450, summary.totalTokens)
        assertEquals(1, summary.failoverEvents)
        assertEquals(1, summary.rateLimitErrors)
        assertEquals(50.0f, summary.successRatePercentage, 0.01f)
    }
}
