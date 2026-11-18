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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ultron.ai.ui.theme.ArcAmber
import com.ultron.ai.ui.theme.OnBackground
import com.ultron.ai.ui.theme.OnPrimary
import com.ultron.ai.ui.theme.OnSurfaceMuted
import com.ultron.ai.ui.theme.OutlineDim
import com.ultron.ai.ui.theme.OutlineVariant
import com.ultron.ai.ui.theme.SuccessGreen
import com.ultron.ai.ui.theme.SurfaceDark
import com.ultron.ai.ui.theme.UltronRed

@Composable
fun ControlCenterHealthScreen(
    viewModel: ControlCenterViewModel,
    modifier: Modifier = Modifier
) {
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Model Health Monitor ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "MODEL HEALTH MONITOR",
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
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    HealthMonitorItem(
                        name = "llama-3.3-70b-versatile",
                        details = "${telemetry.averageLatencyMs}MS • 99.1% OK • 0 FAIL",
                        statusText = "HEALTHY",
                        statusColor = SuccessGreen
                    )
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(OutlineVariant))
                    HealthMonitorItem(
                        name = "llama-3.2-90b-vision",
                        details = "210MS • 429 NEAR LIMIT",
                        statusText = "WARN",
                        statusColor = ArcAmber
                    )
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(OutlineVariant))
                    HealthMonitorItem(
                        name = "OpenAI / Gemini / Claude",
                        details = "MODULAR SLOT • NO KEY",
                        statusText = "UNSET",
                        statusColor = OnSurfaceMuted
                    )
                }
            }
        }

        // --- 2. Provider Management ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "AI PROVIDERS",
                color = OnSurfaceMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Groq Primary
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                        .border(1.dp, UltronRed.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "PRIMARY",
                            color = UltronRed,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "GROQ",
                            color = OnBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "KEY CONFIGURED",
                            color = SuccessGreen,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }

                // OpenAI / Gemini Future Slot
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                        .border(1.dp, OutlineDim, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "FUTURE SLOT",
                            color = OnSurfaceMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "OPENAI / GEMINI",
                            color = OnBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "NOT CONFIGURED",
                            color = OnSurfaceMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // --- 3. Failover & Routing Policy Toggles ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "FAILOVER & ROUTING POLICY",
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
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    PolicySwitchRow(
                        title = "Automatic Model Switching",
                        checked = config.autoModelSwitching,
                        onCheckedChange = { viewModel.toggleAutoModelSwitching(it) }
                    )
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(OutlineVariant))
                    PolicySwitchRow(
                        title = "Smart Model Routing",
                        checked = config.smartRouting,
                        onCheckedChange = { viewModel.toggleSmartRouting(it) }
                    )
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(OutlineVariant))
                    PolicySwitchRow(
                        title = "Rate Limit Monitoring",
                        checked = config.rateLimitMonitoring,
                        onCheckedChange = { viewModel.toggleRateLimitMonitoring(it) }
                    )
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(OutlineVariant))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Max Failover Retries",
                            color = OnBackground,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${config.maxRetries} RETRIES",
                            color = UltronRed,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthMonitorItem(
    name: String,
    details: String,
    statusText: String,
    statusColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = name,
                    color = OnBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = details,
                    color = OnSurfaceMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }
        }
        Text(
            text = statusText,
            color = statusColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun PolicySwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = OnBackground,
            fontSize = 13.sp
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OnPrimary,
                checkedTrackColor = UltronRed,
                uncheckedThumbColor = OnSurfaceMuted,
                uncheckedTrackColor = SurfaceDark
            )
        )
    }
}
