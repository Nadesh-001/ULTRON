package com.ultron.ai.controlcenter.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ultron.ai.ui.theme.BackgroundDark
import com.ultron.ai.ui.theme.OnBackground
import com.ultron.ai.ui.theme.OnPrimary
import com.ultron.ai.ui.theme.OnSurfaceMuted
import com.ultron.ai.ui.theme.OutlineDim
import com.ultron.ai.ui.theme.OutlineVariant
import com.ultron.ai.ui.theme.SuccessGreen
import com.ultron.ai.ui.theme.SurfaceContainerLow
import com.ultron.ai.ui.theme.SurfaceDark
import com.ultron.ai.ui.theme.UltronRed
import org.koin.androidx.compose.koinViewModel

/**
 * Master ULTRON AI Control Center host screen.
 * Transcribed directly from the official UI specifications with 4 unified tabs:
 * Overview, AI Usage & Analytics, Model Manager, and Health & Providers.
 */
@Composable
fun ControlCenterHostScreen(
    onNavigateBack: () -> Unit,
    viewModel: ControlCenterViewModel = koinViewModel()
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- 1. Top Control Center Header ---
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
                Column {
                    Text(
                        text = when (currentTab) {
                            ControlCenterTab.OVERVIEW -> "CONTROL CENTER"
                            ControlCenterTab.USAGE -> "AI USAGE & ANALYTICS"
                            ControlCenterTab.MODELS -> "MODEL MANAGER"
                            ControlCenterTab.HEALTH -> "HEALTH & PROVIDERS"
                        },
                        color = OnBackground,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = if (telemetry.isGroqConnected) "● GROQ CONNECTED" else "● DISCONNECTED",
                        color = if (telemetry.isGroqConnected) SuccessGreen else UltronRed,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        letterSpacing = 0.6.sp
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = {
                        viewModel.refreshUsage()
                        viewModel.testConnection()
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                        .border(1.dp, OutlineDim, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = OnSurfaceMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (currentTab == ControlCenterTab.HEALTH) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(UltronRed)
                            .clickable { viewModel.testConnection() }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (telemetry.isChecking) "TESTING..." else "TEST",
                            color = OnPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                } else {
                    IconButton(
                        onClick = { viewModel.selectTab(ControlCenterTab.HEALTH) },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark)
                            .border(1.dp, OutlineDim, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = OnSurfaceMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // --- 2. Main Scrollable Screen Content ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            when (currentTab) {
                ControlCenterTab.OVERVIEW -> ControlCenterOverviewScreen(viewModel = viewModel)
                ControlCenterTab.USAGE -> ControlCenterUsageScreen(viewModel = viewModel)
                ControlCenterTab.MODELS -> ControlCenterModelsScreen(viewModel = viewModel)
                ControlCenterTab.HEALTH -> ControlCenterHealthScreen(viewModel = viewModel)
            }
        }

        // --- 3. Sticky 4-Tab Bottom Navigation Dock ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceContainerLow)
                .border(width = 1.dp, color = OutlineVariant)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTabItem(
                label = "Overview",
                icon = Icons.Default.Dashboard,
                isSelected = currentTab == ControlCenterTab.OVERVIEW,
                onClick = { viewModel.selectTab(ControlCenterTab.OVERVIEW) }
            )
            NavTabItem(
                label = "Usage",
                icon = Icons.Default.BarChart,
                isSelected = currentTab == ControlCenterTab.USAGE,
                onClick = { viewModel.selectTab(ControlCenterTab.USAGE) }
            )
            NavTabItem(
                label = "Models",
                icon = Icons.Default.ModelTraining,
                isSelected = currentTab == ControlCenterTab.MODELS,
                onClick = { viewModel.selectTab(ControlCenterTab.MODELS) }
            )
            NavTabItem(
                label = "Health",
                icon = Icons.Default.MonitorHeart,
                isSelected = currentTab == ControlCenterTab.HEALTH,
                onClick = { viewModel.selectTab(ControlCenterTab.HEALTH) }
            )
        }
    }
}

@Composable
private fun NavTabItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) UltronRed else OnSurfaceMuted,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            color = if (isSelected) UltronRed else OnSurfaceMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
