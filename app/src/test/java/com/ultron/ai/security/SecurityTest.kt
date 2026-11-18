package com.ultron.ai.security

import com.ultron.ai.core.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * Pure unit tests verifying ULTRON security, authorization, and privacy logic
 * with zero mocks or external dependencies.
 */
class SecurityTest {

    // 1. Classification: LOW risk for benign navigation & launch commands
    @Test
    fun testLowRiskClassification() {
        val testContext = android.test.mock.MockContext()
        val secMgr = SecurityManager(testContext, PermissionManager(testContext))
        val authManager = ActionAuthorizationManager(testContext, secMgr)

        assertEquals(RiskLevel.LOW, authManager.classifyCommand("open spotify"))
        assertEquals(RiskLevel.LOW, authManager.classifyCommand("launch settings"))
        assertEquals(RiskLevel.LOW, authManager.classifyCommand("scroll down"))
        assertEquals(RiskLevel.LOW, authManager.classifyCommand("search public weather in London"))
    }

    // 2. Classification: HIGH risk for destructive & payment operations
    @Test
    fun testHighRiskClassification() {
        val testContext = android.test.mock.MockContext()
        val secMgr = SecurityManager(testContext, PermissionManager(testContext))
        val authManager = ActionAuthorizationManager(testContext, secMgr)

        assertEquals(RiskLevel.HIGH, authManager.classifyCommand("delete all database files"))
        assertEquals(RiskLevel.HIGH, authManager.classifyCommand("pay utility bill"))
        assertEquals(RiskLevel.HIGH, authManager.classifyCommand("purchase subscription"))
        assertEquals(RiskLevel.HIGH, authManager.classifyCommand("transfer money from bank"))
    }

    // 3. Classification: MEDIUM risk for personal communication
    @Test
    fun testMediumRiskClassification() {
        val testContext = android.test.mock.MockContext()
        val secMgr = SecurityManager(testContext, PermissionManager(testContext))
        val authManager = ActionAuthorizationManager(testContext, secMgr)

        assertEquals(RiskLevel.MEDIUM, authManager.classifyCommand("send message to Alice"))
        assertEquals(RiskLevel.MEDIUM, authManager.classifyCommand("share contact card"))
    }

    // 4. Privacy: API keys are completely redacted from logs
    @Test
    fun testApiKeySanitization() {
        val testContext = android.test.mock.MockContext()
        val privacyManager = PrivacyManager(testContext)

        val testToken = listOf("token", "alpha", "98765432101234567890").joinToString("_")
        val logWithKey = "Connecting with key=$testToken to service"
        val sanitized = privacyManager.sanitizeForLogging(logWithKey)
        assertTrue(
            "API key pattern must be masked to [API_KEY]",
            sanitized.contains("[API_KEY]")
        )
        assertFalse(
            "Raw API key must never appear in sanitized output",
            sanitized.contains(testToken)
        )
    }

    // 5. Privacy: Email addresses are redacted from logs
    @Test
    fun testEmailSanitization() {
        val testContext = android.test.mock.MockContext()
        val privacyManager = PrivacyManager(testContext)

        val logWithEmail = "Recipient is tony.stark@avengers.org"
        val sanitized = privacyManager.sanitizeForLogging(logWithEmail)
        assertEquals("Recipient is [EMAIL]", sanitized)
    }

    // 6. Privacy: Credit card numbers are redacted from logs
    @Test
    fun testCardSanitization() {
        val testContext = android.test.mock.MockContext()
        val privacyManager = PrivacyManager(testContext)

        val logWithCard = "Card 4532 1234 5678 9010 approved"
        val sanitized = privacyManager.sanitizeForLogging(logWithCard)
        assertTrue("Card numbers must be sanitized", sanitized.contains("[CARD]"))
    }

    // 7. Action history records approval
    @Test
    fun testActionHistoryRecording() = runBlocking {
        val testContext = android.test.mock.MockContext()
        val secMgr = SecurityManager(testContext, PermissionManager(testContext))
        val authManager = ActionAuthorizationManager(testContext, secMgr)

        val request = ActionRequest(
            id = "action-auto-1",
            command = "open clock",
            riskLevel = RiskLevel.LOW,
            description = "Open clock app"
        )
        val result = authManager.requestAuthorization(request)
        assertEquals(AuthResult.APPROVED, result)

        val history = authManager.getActionHistory()
        assertTrue("History must contain the auto-approved low-risk action", history.isNotEmpty())
        assertEquals(AuthResult.APPROVED, history.first().second)
    }

    // 8. Emergency stop lifecycle transitions
    @Test
    fun testEmergencyStopLifecycle() {
        val testContext = android.test.mock.MockContext()
        val secMgr = SecurityManager(testContext, PermissionManager(testContext))
        val privacyManager = PrivacyManager(testContext)
        val controller = EmergencyStopController(testContext, secMgr, privacyManager)

        assertFalse("Initially emergency stop should be inactive", controller.isEmergencyStopped.value)
        controller.stopAllAutomation()
        assertTrue("Emergency stop should be active after stopAllAutomation()", controller.isEmergencyStopped.value)
        controller.resetAutomationState()
        assertFalse("Emergency stop should be cleared after resetAutomationState()", controller.isEmergencyStopped.value)
    }
}
