package com.ultron.ai.ui.chat.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultron.ai.domain.model.Message
import com.ultron.ai.domain.model.MessageRole
import com.ultron.ai.ui.theme.OnBackground
import com.ultron.ai.ui.theme.OnSurfaceMuted
import com.ultron.ai.ui.theme.Outline
import com.ultron.ai.ui.theme.OutlineVariant
import com.ultron.ai.ui.theme.SurfaceContainer
import com.ultron.ai.ui.theme.SurfaceContainerHigh
import com.ultron.ai.ui.theme.UltronRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Cinematic dark-metallic message bubbles matching the ULTRON HTML Chat Interface specification.
 * - Operator messages: Right-aligned, dark metallic with hairline steel borders.
 * - Ultron messages: Left-aligned, obsidian containers with crimson accents, live streaming indicators,
 *   and neural core telemetry readouts.
 */
@Composable
fun MessageBubble(
    message: Message,
    modifier: Modifier = Modifier
) {
    if (message.role == MessageRole.USER) {
        UserMessageBubble(message, modifier)
    } else {
        UltronMessageBubble(message, modifier)
    }
}

@Composable
private fun UserMessageBubble(
    message: Message,
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = timeFormat.format(Date(message.timestamp))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Operator Header
        Text(
            text = "OPERATOR // $timeString",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = OnSurfaceMuted,
            letterSpacing = 0.8.sp
        )

        // Bubble Container
        SelectionContainer {
            Box(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .background(
                        color = SurfaceContainerHigh,
                        shape = RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 4.dp,
                            bottomStart = 12.dp,
                            bottomEnd = 12.dp
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Outline,
                        shape = RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 4.dp,
                            bottomStart = 12.dp,
                            bottomEnd = 12.dp
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.content,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = OnBackground
                )
            }
        }
    }
}

@Composable
private fun UltronMessageBubble(
    message: Message,
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = timeFormat.format(Date(message.timestamp))
    val isStreaming = message.isStreaming

    val infiniteTransition = rememberInfiniteTransition(label = "UltronStream")
    val dotAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dotAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dotAlpha3 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Ultron Core Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Reactor Avatar Node
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(Color(0xFF3B0808), CircleShape)
                    .border(1.dp, UltronRed, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(UltronRed, CircleShape)
                )
            }

            Text(
                text = if (isStreaming) "ULTRON // STREAMING" else "ULTRON // CORE",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = UltronRed,
                letterSpacing = 0.8.sp
            )

            Text(
                text = timeString,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = OnSurfaceMuted
            )
        }

        // Message Card
        SelectionContainer {
            Box(
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .background(
                        color = SurfaceContainer,
                        shape = RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 12.dp,
                            bottomStart = 12.dp,
                            bottomEnd = 12.dp
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = if (isStreaming) UltronRed else Color(0xFF3B0808),
                        shape = RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 12.dp,
                            bottomStart = 12.dp,
                            bottomEnd = 12.dp
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = message.content,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = OnBackground
                    )

                    // Streaming Synthesizing Bar
                    if (isStreaming) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Box(modifier = Modifier.size(5.dp).background(UltronRed.copy(alpha = dotAlpha1), CircleShape))
                            Box(modifier = Modifier.size(5.dp).background(UltronRed.copy(alpha = dotAlpha2), CircleShape))
                            Box(modifier = Modifier.size(5.dp).background(UltronRed.copy(alpha = dotAlpha3), CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SYNTHESIZING TOKENS",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = OnSurfaceMuted,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Telemetry Footer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .border(width = 0.5.dp, color = OutlineVariant)
                            .padding(top = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CPU: 18% LOAD",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = OnSurfaceMuted,
                                letterSpacing = 0.6.sp
                            )
                            Text(
                                text = "STATUS: OPTIMAL",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = UltronRed,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
