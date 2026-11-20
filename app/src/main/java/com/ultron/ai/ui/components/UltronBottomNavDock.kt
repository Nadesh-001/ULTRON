package com.ultron.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultron.ai.ui.theme.OnSurfaceMuted
import com.ultron.ai.ui.theme.SurfaceContainerLow
import com.ultron.ai.ui.theme.UltronRed

enum class UltronNavTab {
    CORE, VOICE, CHAT
}

/**
 * Shared Persistent Bottom Navigation Dock matching the ULTRON HTML HUD specification.
 * Provides instant 1-tap switching between Core (Dashboard), Voice (Assistant), and Chat (Interface).
 */
@Composable
fun UltronBottomNavDock(
    currentTab: UltronNavTab,
    onNavigateToHome: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onNavigateToChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceContainerLow)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        UltronNavItem(
            label = "CORE",
            icon = Icons.Filled.Radar,
            selected = currentTab == UltronNavTab.CORE,
            onClick = onNavigateToHome
        )
        UltronNavItem(
            label = "VOICE",
            icon = Icons.Filled.GraphicEq,
            selected = currentTab == UltronNavTab.VOICE,
            onClick = onNavigateToVoice
        )
        UltronNavItem(
            label = "CHAT",
            icon = Icons.Filled.Terminal,
            selected = currentTab == UltronNavTab.CHAT,
            onClick = onNavigateToChat
        )
    }
}

@Composable
private fun UltronNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tint = if (selected) UltronRed else OnSurfaceMuted
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            color = tint,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 11.sp,
            letterSpacing = 1.sp
        )
    }
}
