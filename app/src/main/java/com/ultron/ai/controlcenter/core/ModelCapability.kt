package com.ultron.ai.controlcenter.core

enum class ModelCapability(val label: String) {
    TEXT("TEXT"),
    TOOL_CALLING("TOOL CALLING"),
    REASONING("REASONING"),
    VISION("VISION"),
    AUDIO("AUDIO"),
    STREAMING("STREAMING")
}

data class ModelInfo(
    val id: String,
    val displayName: String,
    val provider: String = "GROQ",
    val capabilities: Set<ModelCapability>,
    val averageLatencyMs: Long = 120,
    val isPrimary: Boolean = false,
    val isBackup: Boolean = false,
    val isAvailable: Boolean = true
)
