package com.ultron.ai.controlcenter.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ultron.ai.ui.theme.OnBackground
import com.ultron.ai.ui.theme.OnPrimary
import com.ultron.ai.ui.theme.OnSurfaceMuted
import com.ultron.ai.ui.theme.OutlineDim
import com.ultron.ai.ui.theme.SuccessGreen
import com.ultron.ai.ui.theme.SurfaceContainer
import com.ultron.ai.ui.theme.SurfaceContainerHighest
import com.ultron.ai.ui.theme.SurfaceDark
import com.ultron.ai.ui.theme.UltronRed

@Composable
fun ControlCenterOverviewScreen(
    viewModel: ControlCenterViewModel,
    modifier: Modifier = Modifier
) {
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val usage by viewModel.usageState.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- SECTION A: AI CONNECTION STATUS ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "A // AI CONNECTION STATUS",
                color = OnSurfaceMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Groq API Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                        .border(1.dp, OutlineDim, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GROQ API",
                                color = OnBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (telemetry.isGroqConnected) SuccessGreen else UltronRed)
                            )
                        }
                        Text(
                            text = telemetry.statusMessage,
                            color = if (telemetry.isGroqConnected) SuccessGreen else UltronRed,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "AVG: ${telemetry.averageLatencyMs}MS • ${telemetry.lastSuccessTimeAgo}",
                            color = OnSurfaceMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }

                // Backup Provider Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                        .border(1.dp, OutlineDim, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "BACKUP",
                                color = OnBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(OnSurfaceMuted)
                            )
                        }
                        Text(
                            text = "GROQ FALLBACK",
                            color = OnSurfaceMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "STANDBY READY",
                            color = OnSurfaceMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // --- SECTION B: USAGE OVERVIEW (4 Summary Cards) ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "B // LOCALLY TRACKED USAGE (24H)",
                color = OnSurfaceMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UsageSummaryCard(
                    title = "TOTAL REQUESTS",
                    value = usage.totalRequests.toString(),
                    subtitle = "+12% VS YESTERDAY",
                    subtitleColor = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
                UsageSummaryCard(
                    title = "INPUT TOKENS",
                    value = String.format("%,d", usage.inputTokens),
                    subtitle = "AVG 433 / REQ",
                    subtitleColor = OnSurfaceMuted,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UsageSummaryCard(
                    title = "OUTPUT TOKENS",
                    value = String.format("%,d", usage.outputTokens),
                    subtitle = "AVG 263 / REQ",
                    subtitleColor = OnSurfaceMuted,
                    modifier = Modifier.weight(1f)
                )
                UsageSummaryCard(
                    title = "TOTAL TOKENS",
                    value = String.format("%,d", usage.totalTokens),
                    valueColor = UltronRed,
                    subtitle = "LOCAL COUNTER",
                    subtitleColor = OnSurfaceMuted,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // --- SECTION C: API RATE LIMITS (Provider Headers) ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "C // API RATE LIMITS (PROVIDER HEADERS)",
                color = OnSurfaceMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceDark)
                    .border(1.dp, OutlineDim, RoundedCornerShape(8.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // RPM Bar
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "REQUESTS / MIN (RPM)",
                                color = OnBackground,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "${telemetry.remainingRpm} / ${telemetry.totalRpm} REM",
                                color = OnBackground,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(SurfaceContainerHighest)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.92f)
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(SuccessGreen)
                            )
                        }
                    }

                    // TPM Bar
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "TOKENS / MIN (TPM)",
                                color = OnBackground,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "${telemetry.remainingTpm} / ${telemetry.totalTpm} REM",
                                color = OnBackground,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(SurfaceContainerHighest)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(UltronRed)
                            )
                        }
                    }

                    Text(
                        text = "RESET IN: ${telemetry.rateLimitResetSeconds}S // X-RATELIMIT-RESET-TOKENS",
                        color = OnSurfaceMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // --- SECTION D: ACTIVE ROUTING MODELS ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "D // ACTIVE ROUTING MODELS",
                color = OnSurfaceMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp
            )

            ActiveModelRow(
                role = "PRIMARY • GROQ",
                modelName = config.primaryChatModel,
                capabilities = "TEXT • TOOL CALLING • STREAMING",
                status = "HEALTHY",
                isPrimary = true
            )
            ActiveModelRow(
                role = "BACKUP • GROQ",
                modelName = config.backupChatModel,
                capabilities = "TEXT • STREAMING",
                status = "STANDBY",
                isPrimary = false
            )
            ActiveModelRow(
                role = "VISION • GROQ",
                modelName = config.visionModel,
                capabilities = "VISION • TEXT",
                status = "READY",
                isPrimary = false
            )
            ActiveModelRow(
                role = "VOICE • GROQ",
                modelName = config.voiceModel,
                capabilities = "AUDIO TRANSCRIPTION",
                status = "READY",
                isPrimary = false
            )
        }

        // --- SECTION E: QUICK ACTIONS ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "E // QUICK ACTIONS",
                color = OnSurfaceMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(UltronRed)
                        .clickable { viewModel.selectTab(ControlCenterTab.MODELS) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SWITCH MODEL",
                        color = OnPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceDark)
                        .border(1.dp, OutlineDim, RoundedCornerShape(6.dp))
                        .clickable { viewModel.selectTab(ControlCenterTab.MODELS) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CONFIGURE BACKUP",
                        color = OnBackground,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceDark)
                        .border(1.dp, OutlineDim, RoundedCornerShape(6.dp))
                        .clickable { viewModel.selectTab(ControlCenterTab.USAGE) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "VIEW ANALYTICS",
                        color = OnBackground,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceDark)
                        .border(1.dp, OutlineDim, RoundedCornerShape(6.dp))
                        .clickable { viewModel.testConnection() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "TEST CONNECTION",
                        color = OnBackground,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun UsageSummaryCard(
    title: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = OnBackground,
    subtitle: String,
    subtitleColor: androidx.compose.ui.graphics.Color = OnSurfaceMuted,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark)
            .border(1.dp, OutlineDim, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                color = OnSurfaceMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
            Text(
                text = value,
                color = valueColor,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subtitle,
                color = subtitleColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun ActiveModelRow(
    role: String,
    modelName: String,
    capabilities: String,
    status: String,
    isPrimary: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark)
            .border(1.dp, if (isPrimary) UltronRed.copy(alpha = 0.6f) else OutlineDim, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = role,
                    color = if (isPrimary) UltronRed else OnSurfaceMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = modelName,
                    color = OnBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = capabilities,
                    color = OnSurfaceMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }
            Text(
                text = status,
                color = SuccessGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
