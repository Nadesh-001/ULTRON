package com.ultron.ai.ui.voice

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultron.ai.ui.theme.BackgroundDark
import com.ultron.ai.ui.theme.CrimsonRed
import com.ultron.ai.ui.theme.OnBackground
import com.ultron.ai.ui.theme.OnPrimary
import com.ultron.ai.ui.theme.OnSurfaceMuted
import com.ultron.ai.ui.theme.OutlineDim
import com.ultron.ai.ui.theme.OutlineVariant
import com.ultron.ai.ui.theme.SurfaceContainerLow
import com.ultron.ai.ui.theme.SurfaceDark
import com.ultron.ai.ui.theme.UltronRed
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class VoiceState(
    val label: String,
    val badge: String,
    val freq: String,
    val response: String,
    val icon: ImageVector,
    val hint: String
) {
    LISTENING(
        label = "LISTENING",
        badge = "LISTENING STATE",
        freq = "48.0 KHZ // LIVE",
        response = "\"At your service. All systems operational.\"",
        icon = Icons.Filled.Mic,
        hint = "TAP TO MUTE VOICE ENGINE"
    ),
    PROCESSING(
        label = "PROCESSING",
        badge = "PROCESSING STATE",
        freq = "NEURAL SYNC // 14MS",
        response = "Analyzing acoustic directive and routing telemetry...",
        icon = Icons.Filled.Memory,
        hint = "SYNTHESIZING RESPONSE..."
    ),
    SPEAKING(
        label = "SPEAKING",
        badge = "SPEAKING STATE",
        freq = "VOCAL OUT // ACTIVE",
        response = "\"At your service. All systems operational.\"",
        icon = Icons.Filled.VolumeUp,
        hint = "ULTRON TRANSMITTING"
    )
}

/**
 * Cinematic dark-metallic ULTRON AI Voice Assistant Screen in pure Kotlin Jetpack Compose.
 */
@Composable
fun UltronVoiceAssistantScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToChat: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentState by remember { mutableStateOf(VoiceState.LISTENING) }
    var userTranscript by remember { mutableStateOf("\"Hello Ultron\"") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- 1. Top HUD Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceContainerLow)
                .border(width = 1.dp, color = OutlineVariant)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(UltronRed)
                )
                Text(
                    text = "ULTRON // OS 4.2",
                    color = OnBackground,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.2.sp
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "SYNC 99.8%",
                    color = OnSurfaceMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 0.6.sp
                )
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                        .border(1.dp, OutlineDim, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "Audio settings",
                        tint = OnSurfaceMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // --- 2. Main Content Body ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // State Header & Mode Selector Pills
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceDark)
                        .border(1.dp, OutlineDim, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(UltronRed)
                    )
                    Text(
                        text = currentState.badge,
                        color = OnBackground,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        letterSpacing = 0.8.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                        .border(1.dp, OutlineDim, RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    VoiceState.entries.forEach { state ->
                        val isSelected = currentState == state
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) UltronRed else Color.Transparent)
                                .clickable { currentState = state }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state.label,
                                color = if (isSelected) OnPrimary else OnSurfaceMuted,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }
                }
            }

            // Real-Time Acoustic Matrix Waveform
            VoiceAcousticWaveform(
                state = currentState,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .border(1.dp, OutlineDim, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            )

            // Live Transcription & AI Response Cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // User Live Transcript
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                        .border(1.dp, OutlineDim, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "USER // LIVE TRANSCRIPT",
                            color = OnSurfaceMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = "00:02.4",
                            color = OnSurfaceMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = userTranscript,
                        color = OnBackground,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 16.sp
                    )
                }

                // Ultron Neural Response
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                        .border(1.dp, UltronRed, RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(UltronRed)
                            )
                            Text(
                                text = "ULTRON // NEURAL RESPONSE",
                                color = UltronRed,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            )
                        }
                        Text(
                            text = "CONFIDENCE 99.9%",
                            color = OnSurfaceMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = currentState.response,
                        color = OnBackground,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        lineHeight = 24.sp
                    )
                }
            }

            // Animated Microphone Action Trigger with Kinetic Energy Rings
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.size(96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Kinetic Ripple Rings
                    val infiniteTransition = rememberInfiniteTransition(label = "mic_ripple")
                    val rippleScale by infiniteTransition.animateFloat(
                        initialValue = 0.95f,
                        targetValue = 1.5f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1800, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "ripple"
                    )
                    val rippleAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.7f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1800, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "ripple_alpha"
                    )

                    Box(
                        modifier = Modifier
                            .size(76.dp * rippleScale)
                            .clip(CircleShape)
                            .border(1.5.dp, UltronRed.copy(alpha = rippleAlpha), CircleShape)
                    )

                    // Main Central Mic Button
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(UltronRed)
                            .border(2.dp, OnBackground, CircleShape)
                            .clickable {
                                // Toggle active state
                                currentState = if (currentState == VoiceState.LISTENING) {
                                    VoiceState.PROCESSING
                                } else {
                                    VoiceState.LISTENING
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = currentState.icon,
                            contentDescription = "Toggle microphone",
                            tint = OnPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Text(
                    text = currentState.hint,
                    color = OnSurfaceMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp
                )
            }
        }

        // --- 3. Shared Bottom Navigation Dock ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceContainerLow)
                .border(width = 1.dp, color = OutlineVariant)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Core
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onNavigateToHome() }
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Radar,
                    contentDescription = "Core",
                    tint = OnSurfaceMuted,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "CORE",
                    color = OnSurfaceMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 0.6.sp
                )
            }

            // Voice (Active)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.GraphicEq,
                    contentDescription = "Voice",
                    tint = UltronRed,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "VOICE",
                    color = UltronRed,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
            }

            // Chat
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onNavigateToChat() }
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Terminal,
                    contentDescription = "Chat",
                    tint = OnSurfaceMuted,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "CHAT",
                    color = OnSurfaceMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 0.6.sp
                )
            }
        }
    }
}

/**
 * Procedurally animated multi-band audio waveform visualizer for ULTRON AI.
 */
@Composable
private fun VoiceAcousticWaveform(
    state: VoiceState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "ACOUSTIC MATRIX",
                color = OnSurfaceMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 0.6.sp
            )
            Text(
                text = state.freq,
                color = UltronRed,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 0.6.sp
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            val bars = 36
            val barWidth = 4.dp.toPx()
            val totalWidth = size.width
            val gap = (totalWidth - bars * barWidth) / (bars - 1)
            val centerY = size.height / 2f

            for (i in 0 until bars) {
                val norm = i.toFloat() / (bars - 1).toFloat()
                val envelope = sin(norm * PI.toFloat())

                val amp = when (state) {
                    VoiceState.LISTENING -> {
                        (12f + abs(sin(phase + i * 0.45f)) * 28f) * envelope
                    }
                    VoiceState.PROCESSING -> {
                        (6f + abs(cos(phase * 1.4f + i * 0.7f)) * 16f) * (0.4f + 0.6f * envelope)
                    }
                    VoiceState.SPEAKING -> {
                        (16f + abs(sin(phase * 1.2f + i * 0.35f)) * 34f) * envelope
                    }
                }

                val x = i * (barWidth + gap)
                val y = centerY - amp
                val color = if (i % 3 == 0) UltronRed else CrimsonRed

                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, amp * 2),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
        }
    }
}
