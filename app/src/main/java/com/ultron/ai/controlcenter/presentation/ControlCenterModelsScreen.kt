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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.ultron.ai.controlcenter.core.ModelCapability
import com.ultron.ai.controlcenter.core.ModelInfo
import com.ultron.ai.ui.theme.OnBackground
import com.ultron.ai.ui.theme.OnPrimary
import com.ultron.ai.ui.theme.OnSurfaceMuted
import com.ultron.ai.ui.theme.OutlineDim
import com.ultron.ai.ui.theme.SuccessGreen
import com.ultron.ai.ui.theme.SurfaceContainerHigh
import com.ultron.ai.ui.theme.SurfaceDark
import com.ultron.ai.ui.theme.UltronRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlCenterModelsScreen(
    viewModel: ControlCenterViewModel,
    modifier: Modifier = Modifier
) {
    val pendingModel by viewModel.pendingPrimaryModel.collectAsStateWithLifecycle()
    val models = viewModel.getModels()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        models.forEach { model ->
            ModelCard(
                model = model,
                onSetPrimary = { viewModel.promptSwitchPrimaryModel(model.id) },
                onSetBackup = { viewModel.setBackupModel(model.id) },
                onTestModel = { viewModel.testConnection() }
            )
        }
    }

    // Primary Model Switch Confirmation Dialog
    if (pendingModel != null) {
        BasicAlertDialog(
            onDismissRequest = { viewModel.cancelSwitchPrimaryModel() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .border(1.5.dp, UltronRed, RoundedCornerShape(12.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "SWITCH PRIMARY MODEL?",
                        color = OnBackground,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Switch Primary Chat Model to '$pendingModel'? Note: models without tool-calling support will not execute device tool directives.",
                        color = OnSurfaceMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceContainerHigh)
                                .border(1.dp, OutlineDim, RoundedCornerShape(6.dp))
                                .clickable { viewModel.cancelSwitchPrimaryModel() }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "CANCEL",
                                color = OnBackground,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(UltronRed)
                                .clickable { viewModel.confirmSwitchPrimaryModel() }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "CONFIRM",
                                color = OnPrimary,
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
}

@Composable
private fun ModelCard(
    model: ModelInfo,
    onSetPrimary: () -> Unit,
    onSetBackup: () -> Unit,
    onTestModel: () -> Unit
) {
    val borderColor = if (model.isPrimary) UltronRed else OutlineDim
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header: Designation & Latency
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val designation = when {
                    model.isPrimary -> "PRIMARY CHAT • ${model.provider}"
                    model.isBackup -> "BACKUP & LIGHTWEIGHT • ${model.provider}"
                    model.id.contains("vision") -> "VISION MODEL • ${model.provider}"
                    model.id.contains("whisper") -> "VOICE TRANSCRIPTION • ${model.provider}"
                    else -> model.provider
                }
                Text(
                    text = designation,
                    color = if (model.isPrimary) UltronRed else OnSurfaceMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${model.averageLatencyMs}MS",
                    color = SuccessGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Title & ID
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = model.displayName,
                    color = OnBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = model.id,
                    color = OnSurfaceMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }

            // Capability Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                model.capabilities.forEach { cap ->
                    val isAccent = cap == ModelCapability.TOOL_CALLING || cap == ModelCapability.VISION
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceContainerHigh)
                            .border(1.dp, if (isAccent) UltronRed.copy(alpha = 0.5f) else OutlineDim, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = cap.label,
                            color = if (isAccent) UltronRed else OnBackground,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = if (isAccent) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!model.isPrimary && !model.id.contains("whisper")) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(UltronRed)
                            .clickable { onSetPrimary() }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SET PRIMARY",
                            color = OnPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }

                if (!model.isBackup && !model.id.contains("whisper")) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceContainerHigh)
                            .border(1.dp, OutlineDim, RoundedCornerShape(4.dp))
                            .clickable { onSetBackup() }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SET BACKUP",
                            color = OnBackground,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceContainerHigh)
                        .border(1.dp, OutlineDim, RoundedCornerShape(4.dp))
                        .clickable { onTestModel() }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "TEST MODEL",
                        color = OnBackground,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
