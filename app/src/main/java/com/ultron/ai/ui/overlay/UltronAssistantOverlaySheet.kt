package com.ultron.ai.ui.overlay

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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Translate
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
import androidx.compose.ui.unit.dp
import com.ultron.ai.ui.theme.BackgroundDark
import com.ultron.ai.ui.theme.Crimson
import com.ultron.ai.ui.theme.LabelMedium
import com.ultron.ai.ui.theme.LabelSmall
import com.ultron.ai.ui.theme.MetallicGrey
import com.ultron.ai.ui.theme.Outline
import com.ultron.ai.ui.theme.OutlineVariant
import com.ultron.ai.ui.theme.SurfaceContainer
import com.ultron.ai.ui.theme.SurfaceContainerHigh
import com.ultron.ai.ui.theme.SurfaceContainerLow
import com.ultron.ai.ui.theme.TitleMedium
import com.ultron.ai.ui.theme.UltronRed
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Cinematic Long-Press Assistant Overlay Sheet matching the HTML HUD specification.
 * Pairs obsidian void surfaces with brushed dark-metallic containers, kinetic waveform,
 * quick action chips, and a central ripple microphone trigger.
 */
@Composable
fun UltronAssistantOverlaySheet(
    modifier: Modifier = Modifier,
    onOpenFullApp: () -> Unit = {},
    onTypeClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onDirectiveSelected: (String) -> Unit = {}
) {
    var isListening by remember { mutableStateOf(true) }
    var transcriptText by remember { mutableStateOf("\"How can I assist you?\"") }
    var stateLabel by remember { mutableStateOf("LISTENING // ACTIVE") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(SurfaceContainer)
            .border(
                width = 2.dp,
                color = UltronRed,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Drag Handle
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(Outline)
        )

        // Assistant Overlay Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mini Ultron Core Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.dp, UltronRed, CircleShape)
                        .background(BackgroundDark),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(24.dp)) {
                        drawCircle(color = UltronRed, radius = 5.dp.toPx())
                        drawCircle(
                            color = Crimson,
                            radius = 9.dp.toPx(),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f)
                        )
                    }
                }

                Column {
                    Text(
                        text = "ULTRON",
                        style = LabelMedium,
                        color = Color.White
                    )
                    Text(
                        text = stateLabel,
                        style = LabelSmall,
                        color = UltronRed
                    )
                }
            }

            IconButton(
                onClick = onOpenFullApp,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerHigh)
                    .border(1.dp, Outline, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInFull,
                    contentDescription = "Open full ULTRON app",
                    tint = MetallicGrey,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Live Acoustic Waveform
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceContainerLow)
                .border(1.dp, OutlineVariant, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            OverlayWaveform(isListening = isListening)
        }

        // Live Prompt / Transcription
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = transcriptText,
                style = TitleMedium,
                color = Color.White
            )
            Text(
                text = "Speak a command or select a quick screen action below.",
                style = LabelSmall,
                color = MetallicGrey
            )
        }

        // Contextual Quick Action Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OverlayActionChip(
                icon = Icons.Default.CenterFocusStrong,
                label = "SEARCH SCREEN"
            ) {
                stateLabel = "PROCESSING // DIRECTIVE"
                transcriptText = "\"Executing Search Screen...\""
                onDirectiveSelected("Search Screen")
            }
            OverlayActionChip(
                icon = Icons.Default.Translate,
                label = "TRANSLATE"
            ) {
                stateLabel = "PROCESSING // DIRECTIVE"
                transcriptText = "\"Executing Translate...\""
                onDirectiveSelected("Translate")
            }
            OverlayActionChip(
                icon = Icons.Default.MusicNote,
                label = "IDENTIFY AUDIO"
            ) {
                stateLabel = "PROCESSING // DIRECTIVE"
                transcriptText = "\"Executing Identify Audio...\""
                onDirectiveSelected("Identify Audio")
            }
        }

        // Bottom Control Dock: Keyboard | Central Mic | Chat
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type Button
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceContainerHigh)
                    .border(1.dp, Outline, RoundedCornerShape(8.dp))
                    .clickable { onTypeClick() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = "Switch to keyboard input",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "TYPE",
                    style = LabelSmall,
                    color = Color.White
                )
            }

            // Central Mic with Kinetic Ripple
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isListening) {
                    val rippleTransition = rememberInfiniteTransition(label = "micRipple")
                    val rippleScale by rippleTransition.animateFloat(
                        initialValue = 0.95f,
                        targetValue = 1.45f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1800, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "rippleScale"
                    )
                    val rippleAlpha by rippleTransition.animateFloat(
                        initialValue = 0.75f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1800, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "rippleAlpha"
                    )

                    Canvas(modifier = Modifier.size(64.dp)) {
                        drawCircle(
                            color = UltronRed.copy(alpha = rippleAlpha),
                            radius = (size.minDimension / 2f) * rippleScale,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(UltronRed)
                        .border(2.dp, Color.Black, CircleShape)
                        .clickable {
                            isListening = !isListening
                            if (isListening) {
                                stateLabel = "LISTENING // ACTIVE"
                                transcriptText = "\"How can I assist you?\""
                            } else {
                                stateLabel = "PAUSED // TAP MIC"
                                transcriptText = "\"Voice capture paused.\""
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Toggle voice listening",
                        tint = Color.Black,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Chat Button
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceContainerHigh)
                    .border(1.dp, Outline, RoundedCornerShape(8.dp))
                    .clickable { onChatClick() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "Open Chat",
                    tint = UltronRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CHAT",
                    style = LabelSmall,
                    color = Color.White
                )
            }
        }

        // Android Center Home Pill Indicator
        Box(
            modifier = Modifier
                .width(112.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(UltronRed)
        )
    }
}

@Composable
private fun OverlayActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(SurfaceContainerHigh)
            .border(1.dp, Outline, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = UltronRed,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = LabelSmall,
            color = Color.White
        )
    }
}

@Composable
private fun OverlayWaveform(isListening: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveformTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = Modifier.fillMaxWidth().height(44.dp)) {
        val bars = 32
        val barW = 4.dp.toPx()
        val gap = (size.width - bars * barW) / (bars - 1)
        val midY = size.height / 2f

        for (i in 0 until bars) {
            val norm = i.toFloat() / (bars - 1)
            val envelope = sin(norm * PI).toFloat()
            val amp = if (isListening) {
                (6.dp.toPx() + abs(sin(phase + i * 0.45f)) * 14.dp.toPx()) * envelope
            } else {
                3.dp.toPx() * envelope + 2.dp.toPx()
            }

            val x = i * (barW + gap)
            val color = if (i % 3 == 0) UltronRed else Crimson

            drawRoundRect(
                color = color,
                topLeft = Offset(x, midY - amp),
                size = Size(barW, amp * 2),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}
