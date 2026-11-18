package com.ultron.ai.controlcenter.presentation

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ultron.ai.ui.theme.ArcAmber
import com.ultron.ai.ui.theme.CrimsonRed
import com.ultron.ai.ui.theme.OnBackground
import com.ultron.ai.ui.theme.OnPrimary
import com.ultron.ai.ui.theme.OnSurfaceMuted
import com.ultron.ai.ui.theme.OutlineDim
import com.ultron.ai.ui.theme.OutlineVariant
import com.ultron.ai.ui.theme.SuccessGreen
import com.ultron.ai.ui.theme.SurfaceContainerHighest
import com.ultron.ai.ui.theme.SurfaceContainerLow
import com.ultron.ai.ui.theme.SurfaceDark
import com.ultron.ai.ui.theme.UltronRed

@Composable
fun ControlCenterUsageScreen(
    viewModel: ControlCenterViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTimeframe by viewModel.selectedTimeframe.collectAsStateWithLifecycle()
    val usage by viewModel.usageState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Timeframe Filter Strip ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDark)
                .border(1.dp, OutlineDim, RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            UsageTimeframe.entries.forEach { tf ->
                val isSelected = selectedTimeframe == tf
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) UltronRed else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { viewModel.selectTimeframe(tf) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tf.label,
                        color = if (isSelected) OnPrimary else OnSurfaceMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // --- 2. Billing Transparency Notice ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDark)
                .border(1.dp, OutlineDim, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = ArcAmber,
                    modifier = Modifier.size(20.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "BILLING & CREDITS",
                        color = OnBackground,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Billing balance unavailable through API. Token totals below reflect locally tracked requests only.",
                        color = OnSurfaceMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // --- 3. Daily Token Usage Chart ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDark)
                .border(1.dp, OutlineDim, RoundedCornerShape(8.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DAILY TOKEN USAGE (LOCAL)",
                        color = OnBackground,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${String.format("%,d", usage.totalTokens)} TOKENS",
                        color = UltronRed,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                val chartData = when (selectedTimeframe) {
                    UsageTimeframe.TODAY -> listOf(14f, 22f, 18f, 35f, 29f, 44f, 38f)
                    UsageTimeframe.SEVEN_DAYS -> listOf(62f, 78f, 54f, 91f, 84f, 103f, 96f)
                    UsageTimeframe.THIRTY_DAYS -> listOf(48f, 66f, 72f, 85f, 60f, 94f, 103f)
                    UsageTimeframe.CUSTOM -> listOf(30f, 45f, 60f, 52f, 74f, 88f, 92f)
                }

                TokenBarChart(
                    values = chartData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                )
            }
        }

        // --- 4. Model-Wise Token Distribution ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDark)
                .border(1.dp, OutlineDim, RoundedCornerShape(8.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "MODEL-WISE CONSUMPTION",
                    color = OnBackground,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )

                ModelConsumptionBar(
                    modelId = "llama-3.3-70b-versatile",
                    percentageText = "72% (74,268)",
                    progress = 0.72f,
                    barColor = UltronRed
                )

                ModelConsumptionBar(
                    modelId = "llama-3.1-8b-instant",
                    percentageText = "18% (18,567)",
                    progress = 0.18f,
                    barColor = CrimsonRed
                )

                ModelConsumptionBar(
                    modelId = "llama-3.2-90b-vision-preview",
                    percentageText = "10% (10,315)",
                    progress = 0.10f,
                    barColor = OnSurfaceMuted
                )
            }
        }

        // --- 5. API Error Frequency & Failovers ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDark)
                .border(1.dp, OutlineDim, RoundedCornerShape(8.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "API ERROR FREQUENCY",
                        color = OnBackground,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${usage.successRatePercentage}% SUCCESS",
                        color = SuccessGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ErrorStatCard(
                        label = "HTTP 429",
                        value = usage.rateLimitErrors.toString(),
                        accentColor = ArcAmber,
                        modifier = Modifier.weight(1f)
                    )
                    ErrorStatCard(
                        label = "5XX / NET",
                        value = usage.serverErrors.toString(),
                        accentColor = UltronRed,
                        modifier = Modifier.weight(1f)
                    )
                    ErrorStatCard(
                        label = "FAILOVERS",
                        value = usage.failoverEvents.toString(),
                        accentColor = OnSurfaceMuted,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TokenBarChart(
    values: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val maxVal = values.maxOrNull() ?: 1f
        val count = values.size
        val barWidth = 24.dp.toPx()
        val spacing = (size.width - (count * barWidth)) / (count + 1)

        values.forEachIndexed { i, v ->
            val h = (v / maxVal) * (size.height - 20.dp.toPx())
            val x = spacing + i * (barWidth + spacing)
            val y = size.height - h

            val isLast = i == count - 1
            val color = if (isLast) UltronRed else OutlineDim

            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }
    }
}

@Composable
private fun ModelConsumptionBar(
    modelId: String,
    percentageText: String,
    progress: Float,
    barColor: androidx.compose.ui.graphics.Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = modelId,
                color = OnBackground,
                fontSize = 12.sp
            )
            Text(
                text = percentageText,
                color = OnSurfaceMuted,
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
                    .fillMaxWidth(progress)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun ErrorStatCard(
    label: String,
    value: String,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceContainerLow)
            .border(1.dp, OutlineVariant, RoundedCornerShape(6.dp))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                color = accentColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
            Text(
                text = value,
                color = OnBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
