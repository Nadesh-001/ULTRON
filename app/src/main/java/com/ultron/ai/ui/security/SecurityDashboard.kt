package com.ultron.ai.ui.security

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ultron.ai.core.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ULTRON Obsidian & Red Palette
private val UltronObsidian = Color(0xFF050505)
private val UltronSurface = Color(0xFF101216)
private val UltronSurfaceHigh = Color(0xFF181B22)
private val UltronOutline = Color(0xFF2A2F3A)
private val UltronRed = Color(0xFFFF3030)
private val UltronCrimson = Color(0xFFA50000)
private val UltronMuted = Color(0xFF929292)
private val UltronSuccess = Color(0xFF22C55E)
private val UltronWarning = Color(0xFFF59E0B)

class SecurityDashboardViewModel(
    private val permissionManager: PermissionManager,
    private val securityManager: SecurityManager,
    private val emergencyStopController: EmergencyStopController
) : ViewModel() {

    val permissionStatuses: StateFlow<List<PermissionStatus>> = permissionManager.permissionStateFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isEmergencyStopped: StateFlow<Boolean> = emergencyStopController.isEmergencyStopped
    val recentActions: StateFlow<List<String>> = securityManager.recentAutomationActions
    val isMicActive: StateFlow<Boolean> = securityManager.isMicrophoneActive
    val isCameraActive: StateFlow<Boolean> = securityManager.isCameraActive

    private val _securityWarnings = MutableStateFlow<List<String>>(emptyList())
    val securityWarnings: StateFlow<List<String>> = _securityWarnings

    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled

    private val _isNotificationListenerEnabled = MutableStateFlow(false)
    val isNotificationListenerEnabled: StateFlow<Boolean> = _isNotificationListenerEnabled

    fun refresh(ctx: Context) {
        permissionManager.refreshAll(ctx)
        _securityWarnings.value = securityManager.getSecurityWarnings(ctx)
        _isAccessibilityEnabled.value = securityManager.isAccessibilityServiceRunning(ctx)
        _isNotificationListenerEnabled.value = securityManager.isNotificationListenerRunning(ctx)
    }

    fun getSettingsIntent(permission: String, ctx: Context): Intent =
        permissionManager.getSettingsIntentForPermission(permission, ctx)

    fun stopAllAutomation() = emergencyStopController.stopAllAutomation()
    fun disableVoiceProcessing() = emergencyStopController.disableVoiceProcessing()
    fun stopBackgroundAiTasks() = emergencyStopController.stopBackgroundAiTasks()
    fun disconnectAiSessions() = emergencyStopController.disconnectAiSessions()
    fun resetAutomationState() = emergencyStopController.resetAutomationState()
    fun fullEmergencyStop() = emergencyStopController.fullEmergencyStop()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityDashboard(
    viewModel: SecurityDashboardViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.refresh(context)
    }

    val permissions by viewModel.permissionStatuses.collectAsStateWithLifecycle()
    val warnings by viewModel.securityWarnings.collectAsStateWithLifecycle()
    val isStopped by viewModel.isEmergencyStopped.collectAsStateWithLifecycle()
    val recentActions by viewModel.recentActions.collectAsStateWithLifecycle()
    val isMicActive by viewModel.isMicActive.collectAsStateWithLifecycle()
    val isCameraActive by viewModel.isCameraActive.collectAsStateWithLifecycle()
    val isAccessibility by viewModel.isAccessibilityEnabled.collectAsStateWithLifecycle()
    val isNotification by viewModel.isNotificationListenerEnabled.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = UltronObsidian,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isStopped) UltronRed else UltronSuccess)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "ULTRON // SECURITY & PERMISSIONS",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh(context) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = UltronRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = UltronSurface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // Emergency banner if active
            if (isStopped) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = UltronCrimson.copy(alpha = 0.25f)),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, UltronRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = UltronRed)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("EMERGENCY KILL SWITCH ENGAGED", color = UltronRed, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                                Text("All automation, voice listeners and AI sessions halted.", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Section 1: Security Warnings
            if (warnings.isNotEmpty()) {
                item {
                    Text("ACTIVE WARNINGS", color = UltronWarning, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
                }
                items(warnings) { warning ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = UltronSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, UltronWarning.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = UltronWarning, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(warning, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Section 2: Sensor Activity Monitor
            item {
                Text("REAL-TIME SENSOR MONITORS", color = UltronMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SensorCard(
                        title = "MICROPHONE",
                        isActive = isMicActive,
                        activeColor = UltronRed,
                        icon = Icons.Default.Mic,
                        modifier = Modifier.weight(1f)
                    )
                    SensorCard(
                        title = "CAMERA",
                        isActive = isCameraActive,
                        activeColor = UltronRed,
                        icon = Icons.Default.Videocam,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Section 3: Active System Services
            item {
                Text("SYSTEM PRIVILEGES & SERVICES", color = UltronMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(UltronSurface)
                        .border(1.dp, UltronOutline, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ServiceRow("Accessibility Engine", isAccessibility) {
                        context.startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                    HorizontalDivider(color = UltronOutline)
                    ServiceRow("Notification Listener", isNotification) {
                        context.startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                }
            }

            // Section 4: Emergency Controls
            item {
                Text("EMERGENCY CONTROLS", color = UltronRed, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.fullEmergencyStop()
                            scope.launch { snackbarHostState.showSnackbar("EMERGENCY STOP EXECUTED: All AI stopped") }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = UltronRed),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, tint = Color.Black)
                        Spacer(Modifier.width(8.dp))
                        Text("FULL EMERGENCY STOP", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                viewModel.disableVoiceProcessing()
                                scope.launch { snackbarHostState.showSnackbar("Voice processing disabled") }
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, UltronCrimson),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("MUTE VOICE", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.stopBackgroundAiTasks()
                                scope.launch { snackbarHostState.showSnackbar("Background AI cancelled") }
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, UltronCrimson),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("KILL AI TASKS", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                viewModel.disconnectAiSessions()
                                scope.launch { snackbarHostState.showSnackbar("AI sessions disconnected") }
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, UltronOutline),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("DISCONNECT", color = UltronMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.resetAutomationState()
                                scope.launch { snackbarHostState.showSnackbar("Automation state reset to normal") }
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, UltronSuccess),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("RESET STATE", color = UltronSuccess, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            // Section 5: Granular Permission Monitor
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("PERMISSION REGISTRY (${permissions.size})", color = UltronMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
                    Text("Tap to fix in Settings", color = UltronRed, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }

            items(permissions) { perm ->
                PermissionItem(
                    status = perm,
                    onClick = {
                        try {
                            val intent = viewModel.getSettingsIntent(perm.androidName, context)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            scope.launch { snackbarHostState.showSnackbar("Cannot open settings: ${e.message}") }
                        }
                    }
                )
            }

            // Section 6: Recent Automation Actions
            if (recentActions.isNotEmpty()) {
                item {
                    Text("RECENT AUTOMATION AUDIT", color = UltronMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
                }
                items(recentActions.take(15)) { action ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(UltronSurfaceHigh, RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            action,
                            color = Color(0xFFD0D7DE),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SensorCard(
    title: String,
    isActive: Boolean,
    activeColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "alpha"
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = UltronSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isActive) activeColor else UltronOutline),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isActive) activeColor else UltronMuted,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, color = UltronMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) activeColor.copy(alpha = alpha) else Color.DarkGray
                            )
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isActive) "ACTIVE" else "IDLE",
                        color = if (isActive) activeColor else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun ServiceRow(title: String, isEnabled: Boolean, onConfigure: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onConfigure() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(if (isEnabled) "Active & Authorized" else "Disabled / Unbound", color = if (isEnabled) UltronSuccess else UltronMuted, fontSize = 11.sp)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (isEnabled) UltronSuccess.copy(alpha = 0.15f) else UltronCrimson.copy(alpha = 0.15f))
                .border(1.dp, if (isEnabled) UltronSuccess else UltronCrimson, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                if (isEnabled) "RUNNING" else "OFFLINE",
                color = if (isEnabled) UltronSuccess else UltronRed,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun PermissionItem(status: PermissionStatus, onClick: () -> Unit) {
    val (pillBg, pillBorder, pillColor, pillText) = when (status.state) {
        PermissionState.GRANTED -> Quad(UltronSuccess.copy(alpha = 0.15f), UltronSuccess, UltronSuccess, "GRANTED")
        PermissionState.DENIED -> Quad(UltronCrimson.copy(alpha = 0.2f), UltronRed, UltronRed, "DENIED")
        PermissionState.PERMANENTLY_DENIED -> Quad(UltronWarning.copy(alpha = 0.2f), UltronWarning, UltronWarning, "BLOCKED")
        PermissionState.NOT_APPLICABLE -> Quad(Color.DarkGray.copy(alpha = 0.2f), Color.Gray, Color.Gray, "N/A")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(UltronSurface)
            .border(1.dp, UltronOutline, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(status.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                status.androidName.substringAfterLast("."),
                color = UltronMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(pillBg)
                .border(1.dp, pillBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(pillText, color = pillColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
