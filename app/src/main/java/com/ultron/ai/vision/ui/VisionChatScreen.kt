package com.ultron.ai.vision.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ultron.ai.ui.theme.BackgroundDark
import com.ultron.ai.ui.theme.CrimsonRed
import com.ultron.ai.ui.theme.OnBackground
import com.ultron.ai.ui.theme.OnPrimary
import com.ultron.ai.ui.theme.OnSurfaceMuted
import com.ultron.ai.ui.theme.OutlineDim
import com.ultron.ai.ui.theme.OutlineVariant
import com.ultron.ai.ui.theme.SurfaceContainerHigh
import com.ultron.ai.ui.theme.SurfaceContainerLow
import com.ultron.ai.ui.theme.SurfaceDark
import com.ultron.ai.ui.theme.UltronRed
import org.koin.androidx.compose.koinViewModel

/**
 * Visual Question Answering (VQA) screen for ULTRON Vision.
 * Maintains locked image context throughout interactive multi-turn conversation.
 */
@Composable
fun VisionChatScreen(
    onNavigateBack: () -> Unit,
    viewModel: VisionViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var copyFeedback by remember { mutableStateOf(false) }

    LaunchedEffect(state.conversation.size, state.conversation.lastOrNull()?.text?.length) {
        if (state.conversation.isNotEmpty()) {
            listState.animateScrollToItem(state.conversation.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- 1. Top Contextual Header ---
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
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                        .border(1.dp, OutlineDim, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = OnBackground,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "VISUAL Q&A",
                    color = OnBackground,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.2.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceDark)
                    .border(1.dp, OutlineDim, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
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
                        text = "CONTEXT LOCKED",
                        color = OnBackground,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        letterSpacing = 0.6.sp
                    )
                }
            }
        }

        // --- 2. Main Content Area ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Captured Image Context Card
            state.bitmap?.let { bmp ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                        .border(1.dp, OutlineDim, RoundedCornerShape(8.dp))
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(SurfaceContainerLow)
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Captured Visual Context",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Reticle component highlight box
                            Box(
                                modifier = Modifier
                                    .padding(start = 24.dp, top = 20.dp)
                                    .size(width = 110.dp, height = 75.dp)
                                    .border(2.dp, UltronRed, RoundedCornerShape(2.dp))
                            )

                            Box(
                                modifier = Modifier
                                    .padding(start = 24.dp, top = 6.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(SurfaceDark)
                                    .border(1.dp, UltronRed, RoundedCornerShape(2.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = state.componentCallout,
                                    color = UltronRed,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp
                                )
                            }
                        }

                        // Extracted Summary Strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceDark)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${state.mode.tag} // CONTEXT",
                                    color = OnBackground,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp
                                )
                                Text(
                                    text = state.ocrResult?.fullText?.take(60)?.replace("\n", " ") ?: state.detectedSummary,
                                    color = OnSurfaceMuted,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SurfaceContainerHigh)
                                    .border(1.dp, OutlineDim, RoundedCornerShape(4.dp))
                                    .clickable {
                                        if (viewModel.copyOcrToClipboard()) {
                                            copyFeedback = true
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (copyFeedback) Icons.Filled.Check else Icons.Filled.ContentCopy,
                                        contentDescription = "Copy OCR",
                                        tint = if (copyFeedback) UltronRed else OnBackground,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = if (copyFeedback) "COPIED" else "COPY OCR",
                                        color = if (copyFeedback) UltronRed else OnBackground,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Visual Q&A Conversation Thread
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.conversation, key = { it.id }) { msg ->
                    if (msg.role == "OPERATOR") {
                        // User message
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "OPERATOR // ${msg.timestamp}",
                                color = OnSurfaceMuted,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                letterSpacing = 0.6.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 2.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                                    .background(SurfaceContainerHigh)
                                    .border(1.dp, OutlineDim, RoundedCornerShape(topStart = 8.dp, topEnd = 2.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    color = OnBackground,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    } else {
                        // Ultron Vision Core message
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(CrimsonRed)
                                        .border(1.dp, UltronRed, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(UltronRed)
                                    )
                                }
                                Text(
                                    text = "ULTRON // VISION CORE",
                                    color = UltronRed,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.6.sp
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                                    .background(SurfaceDark)
                                    .border(1.dp, UltronRed.copy(alpha = 0.6f), RoundedCornerShape(topStart = 2.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = msg.text,
                                        color = OnBackground,
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp
                                    )
                                    if (msg.isStreaming) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(12.dp),
                                                color = UltronRed,
                                                strokeWidth = 1.5.dp
                                            )
                                            Text(
                                                text = "Synthesizing visual details...",
                                                color = UltronRed,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 3. Follow-up Quick Prompts Strip ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val quickChips = listOf(
                "Explain all components",
                "What does this port do?",
                "Is anything damaged?",
                "How do I connect this?",
                "Step-by-step solution"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickChips.forEach { chip ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceDark)
                            .border(1.dp, OutlineDim, RoundedCornerShape(4.dp))
                            .clickable {
                                inputText = chip
                                viewModel.askFollowUp(chip)
                                inputText = ""
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = chip,
                            color = OnSurfaceMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            letterSpacing = 0.6.sp
                        )
                    }
                }
            }

            // Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceDark)
                    .border(1.dp, OutlineDim, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = "Ask a follow-up about this image...",
                            color = OnSurfaceMuted,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = UltronRed,
                        focusedTextColor = OnBackground,
                        unfocusedTextColor = OnBackground
                    ),
                    maxLines = 3
                )

                IconButton(
                    onClick = {
                        val q = inputText.trim()
                        if (q.isNotEmpty()) {
                            viewModel.askFollowUp(q)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(UltronRed)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowUpward,
                        contentDescription = "Send",
                        tint = OnPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
