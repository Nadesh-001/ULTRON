package com.ultron.ai.ui.dashboard

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
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultron.ai.ui.theme.BackgroundDark
import com.ultron.ai.ui.theme.CrimsonRed
import com.ultron.ai.ui.theme.OnBackground
import com.ultron.ai.ui.theme.OnPrimary
import com.ultron.ai.ui.theme.OnSurfaceMuted
import com.ultron.ai.ui.theme.OutlineDim
import com.ultron.ai.ui.theme.OutlineVariant
import com.ultron.ai.ui.theme.SuccessGreen
import com.ultron.ai.ui.theme.SurfaceContainerLow
import com.ultron.ai.ui.theme.SurfaceDark
import com.ultron.ai.ui.theme.SurfaceVariant
import com.ultron.ai.ui.theme.UltronRed
import kotlin.math.cos
import kotlin.math.sin

enum class UltronAiState(val label: String, val readout: String) {
    READY("READY", "CORE STANDBY // AWAITING DIRECTIVE"),
    LISTENING("LISTENING", "ACOUSTIC CAPTURE // LISTENING..."),
    THINKING("THINKING", "NEURAL SYNTHESIS // PROCESSING..."),
    SPEAKING("SPEAKING", "VOCAL OUTPUT // TRANSMITTING...")
}

/**
 * Cinematic dark-metallic HUD and neural core Home Dashboard for ULTRON AI.
 * Transcribed directly from the official ULTRON AI design specifications.
 */
@Composable
fun UltronHomeDashboard(
    onNavigateToChat: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onOpenSettings: () -> Unit,
    onNavigateToVision: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentState by remember { mutableStateOf(UltronAiState.READY) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- 1. Top HUD Header Bar ---
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
                        contentDescription = "Core Settings",
                        tint = OnSurfaceMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // --- 2. Main Dashboard Core Content ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Identity Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "ULTRON",
                    color = OnBackground,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "ADVANCED INTELLIGENCE SYSTEM",
                    color = OnSurfaceMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Central 3D Ultron AI Orb / Reactor with Orbital HUD Rings
            UltronNeuralReactor(
                state = currentState,
                modifier = Modifier
                    .size(280.dp)
                    .clickable {
                        // Cycle through states on tap for interactive preview
                        currentState = when (currentState) {
                            UltronAiState.READY -> UltronAiState.LISTENING
                            UltronAiState.LISTENING -> UltronAiState.THINKING
                            UltronAiState.THINKING -> UltronAiState.SPEAKING
                            UltronAiState.SPEAKING -> UltronAiState.READY
                        }
                    }
            )

            // State Selector Pill Bar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                        .border(1.dp, OutlineDim, RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    UltronAiState.entries.forEach { state ->
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
                Text(
                    text = currentState.readout,
                    color = OnSurfaceMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 0.6.sp
                )
            }

            // 2x2 Telemetry Cards Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TelemetryCard(
                        indicatorColor = UltronRed,
                        title = "SYSTEM ONLINE",
                        subtitle = "UPTIME 99.9%",
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryCard(
                        indicatorColor = UltronRed,
                        title = "VOICE ENGINE",
                        subtitle = "48KHZ NEURAL",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TelemetryCard(
                        indicatorColor = SuccessGreen,
                        title = "INTERNET ACTIVE",
                        subtitle = "12MS LATENCY",
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryCard(
                        indicatorColor = UltronRed,
                        title = "AI CORE READY",
                        subtitle = "TEMP 34.2°C",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // --- 3. Persistent Bottom Navigation Dock ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceContainerLow)
                .border(width = 1.dp, color = OutlineVariant)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Core (Active)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Radar,
                    contentDescription = "Core",
                    tint = UltronRed,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "CORE",
                    color = UltronRed,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
            }

            // Voice
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onNavigateToVoice() }
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.GraphicEq,
                    contentDescription = "Voice",
                    tint = OnSurfaceMuted,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "VOICE",
                    color = OnSurfaceMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
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
                    .padding(horizontal = 14.dp, vertical = 4.dp)
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

            // Vision
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onNavigateToVision() }
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Radar,
                    contentDescription = "Vision",
                    tint = UltronRed,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "VISION",
                    color = UltronRed,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
            }
        }
    }
}

@Composable
private fun TelemetryCard(
    indicatorColor: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark)
            .border(1.dp, OutlineDim, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(indicatorColor)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                color = OnBackground,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            )
            Text(
                text = subtitle,
                color = OnSurfaceMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 0.6.sp
            )
        }
    }
}

/**
 * Animated Ultron AI Neural Reactor with kinetic orbital HUD rings and rotating energy telemetry.
 */
@Composable
fun UltronNeuralReactor(
    state: UltronAiState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "reactor")

    // Rotation speeds based on state
    val ringCwSpeed = when (state) {
        UltronAiState.THINKING -> 6000
        UltronAiState.SPEAKING -> 12000
        else -> 24000
    }
    val ringCcwSpeed = when (state) {
        UltronAiState.THINKING -> 4000
        UltronAiState.SPEAKING -> 8000
        else -> 16000
    }

    val rotationCw by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = ringCwSpeed, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationCw"
    )

    val rotationCcw by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = ringCcwSpeed, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationCcw"
    )

    val pulseDuration = when (state) {
        UltronAiState.LISTENING -> 1200
        UltronAiState.SPEAKING -> 750
        else -> 2400
    }

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = pulseDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Rotating Canvas HUD Rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f

            // 1. Clockwise Outer Ring
            rotate(rotationCw, pivot = center) {
                drawCircle(
                    color = OutlineDim,
                    radius = radius * 0.94f,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 16f))
                    )
                )
                drawCircle(
                    color = CrimsonRed.copy(alpha = 0.5f),
                    radius = radius * 0.88f,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(42f, 18f, 12f, 18f))
                    )
                )
                // Tick lines
                drawLine(
                    color = UltronRed,
                    start = Offset(center.x, center.y - radius * 0.98f),
                    end = Offset(center.x, center.y - radius * 0.88f),
                    strokeWidth = 2.5.dp.toPx()
                )
                drawLine(
                    color = UltronRed,
                    start = Offset(center.x, center.y + radius * 0.88f),
                    end = Offset(center.x, center.y + radius * 0.98f),
                    strokeWidth = 2.5.dp.toPx()
                )
                drawLine(
                    color = OnSurfaceMuted,
                    start = Offset(center.x - radius * 0.98f, center.y),
                    end = Offset(center.x - radius * 0.88f, center.y),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawLine(
                    color = OnSurfaceMuted,
                    start = Offset(center.x + radius * 0.88f, center.y),
                    end = Offset(center.x + radius * 0.98f, center.y),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            // 2. Counter-Clockwise Inner Ring
            rotate(rotationCcw, pivot = center) {
                drawCircle(
                    color = SurfaceVariant,
                    radius = radius * 0.75f,
                    style = Stroke(width = 6.dp.toPx())
                )
                drawCircle(
                    color = UltronRed.copy(alpha = 0.8f),
                    radius = radius * 0.75f,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(28f, 54f))
                    )
                )
                // Orbital beacon points
                drawCircle(
                    color = UltronRed,
                    radius = 4.dp.toPx(),
                    center = Offset(center.x, center.y - radius * 0.75f)
                )
                drawCircle(
                    color = CrimsonRed,
                    radius = 4.dp.toPx(),
                    center = Offset(center.x, center.y + radius * 0.75f)
                )
            }

            // 3. Fast Telemetry Arc
            rotate(rotationCw * 1.8f, pivot = center) {
                drawArc(
                    color = UltronRed,
                    startAngle = 0f,
                    sweepAngle = 90f,
                    useCenter = false,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
                    size = androidx.compose.ui.geometry.Size(radius * 1.2f, radius * 1.2f),
                    topLeft = Offset(center.x - radius * 0.6f, center.y - radius * 0.6f)
                )
            }
        }

        // Central Orb with Neon Core & Crimson Energy Gradients
        Box(
            modifier = Modifier
                .size((128 * pulseScale).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            UltronRed.copy(alpha = 0.7f),
                            CrimsonRed.copy(alpha = 0.4f),
                            SurfaceDark
                        )
                    )
                )
                .border(1.5.dp, UltronRed, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(UltronRed, CrimsonRed, BackgroundDark)
                        )
                    )
                    .border(1.dp, OutlineDim, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerLow)
                        .border(1.dp, UltronRed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(UltronRed)
                    )
                }
            }
        }
    }
}
