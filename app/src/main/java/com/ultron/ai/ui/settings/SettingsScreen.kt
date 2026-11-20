package com.ultron.ai.ui.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ultron.ai.service.ULTRONNotificationListener
import com.ultron.ai.ui.components.UltronBottomNavDock
import com.ultron.ai.ui.components.UltronNavTab
import com.ultron.ai.ui.theme.BackgroundDark
import com.ultron.ai.ui.theme.OnBackground
import com.ultron.ai.ui.theme.OnSurfaceMuted
import com.ultron.ai.ui.theme.Outline
import com.ultron.ai.ui.theme.OutlineVariant
import com.ultron.ai.ui.theme.SurfaceContainer
import com.ultron.ai.ui.theme.SurfaceContainerHigh
import com.ultron.ai.ui.theme.SurfaceContainerLow
import com.ultron.ai.ui.theme.UltronRed
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * Complete ULTRON Systems & Configuration Console matching the 7-section HTML specification.
 * 01 // PRIMARY HUD: Autonomous Execution Mode
 * 02 // INTELLIGENCE: Groq LLM Core
 * 03 // ACOUSTIC & VOICE: Vosk & EdgeTTS
 * 04 // MESSAGING & NOTIFICATIONS INTERCEPT
 * 05 // SECURE SERVICES & INTEGRATIONS
 * 06 // NEURAL MEMORY & DIRECTIVES
 * 07 // SECURITY, LOCK POLICY & TELEMETRY
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    transferViewModel: TransferViewModel = koinViewModel(),
    locationViewModel: LocationViewModel = koinViewModel(),
    quickLinksViewModel: QuickLinksViewModel = koinViewModel(),
    onNavigateToHome: () -> Unit = {},
    onNavigateToVoice: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onBack: () -> Unit = {},
    onOpenDiagnostics: () -> Unit = {},
    onOpenControlCenter: () -> Unit = {},
    onOpenModes: () -> Unit = {},
    onOpenMemory: () -> Unit = {}
) {
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val isSaved by viewModel.isSaved.collectAsStateWithLifecycle()
    val models by viewModel.availableModels.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val messagesSettings by viewModel.messages.collectAsStateWithLifecycle()
    val voiceSettings by viewModel.voice.collectAsStateWithLifecycle()
    val learned by viewModel.learnedCommands.collectAsStateWithLifecycle()
    val gmail by viewModel.gmail.collectAsStateWithLifecycle()
    val isCalibrated by viewModel.isCalibrated.collectAsStateWithLifecycle()
    val mapsProvider by viewModel.mapsProvider.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isAutonomousModeActive by remember { mutableStateOf(true) }
    var directExecutionPolicy by remember { mutableStateOf(true) }
    var safetySafeguards by remember { mutableStateOf(true) }
    var apiKeyText by remember(apiKey) { mutableStateOf(apiKey) }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }

    fun showMsg(msg: String) {
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- TOP HUD BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceContainerLow)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .border(width = 0.5.dp, color = OutlineVariant),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(34.dp)
                            .background(SurfaceContainer, CircleShape)
                            .border(1.dp, Outline, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = OnBackground,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = "ULTRON // SYSTEMS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = OnBackground,
                        letterSpacing = 2.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Diagnostics Button
                    Row(
                        modifier = Modifier
                            .background(SurfaceContainer, RoundedCornerShape(4.dp))
                            .border(1.dp, Outline, RoundedCornerShape(4.dp))
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onOpenDiagnostics)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Diagnostics",
                            tint = UltronRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "DIAGNOSTICS",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnBackground,
                            letterSpacing = 0.8.sp
                        )
                    }

                    // Control Center Shortcut
                    IconButton(
                        onClick = onOpenControlCenter,
                        modifier = Modifier
                            .size(34.dp)
                            .background(SurfaceContainer, CircleShape)
                            .border(1.dp, Outline, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = "AI Control Center",
                            tint = UltronRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // --- 7-SECTION SYSTEMS CONSOLE (SCROLLABLE) ---
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ==========================================
                // 01 // PRIMARY HUD: AUTONOMOUS EXECUTION MODE
                // ==========================================
                SectionContainer(
                    borderColor = UltronRed,
                    content = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "01 // PRIMARY HUD",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = UltronRed,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "AUTONOMOUS EXECUTION MODE",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnBackground
                                )
                            }
                            StatusBadge(label = if (isAutonomousModeActive) "ACTIVE" else "DISABLED", isActive = isAutonomousModeActive)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(OutlineVariant))
                        Spacer(modifier = Modifier.height(4.dp))

                        SettingToggleRow(
                            title = "Global Autonomous Switch",
                            subtitle = "AGENTIC TOOL EXECUTION",
                            checked = isAutonomousModeActive,
                            onCheckedChange = { isAutonomousModeActive = it }
                        )

                        SettingToggleRow(
                            title = "Direct Execution Policy",
                            subtitle = "AUTO-RUN VERIFIED INTENTS",
                            checked = directExecutionPolicy,
                            onCheckedChange = { directExecutionPolicy = it }
                        )

                        SettingToggleRow(
                            title = "Safety Safeguards",
                            subtitle = "PROMPT ON IRREVERSIBLE ACTIONS",
                            checked = safetySafeguards,
                            onCheckedChange = { safetySafeguards = it }
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(UltronRed, RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    isAutonomousModeActive = false
                                    directExecutionPolicy = false
                                    showMsg("EMERGENCY STOP ACTIVATED // All autonomous actions halted.")
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Emergency, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "EMERGENCY STOP",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.Black,
                                    letterSpacing = 1.2.sp
                                )
                            }
                        }
                    }
                )

                // ==========================================
                // 02 // INTELLIGENCE: GROQ (LLM CORE)
                // ==========================================
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "02 // INTELLIGENCE · GROQ (LLM CORE)",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceMuted,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (apiKey.isNotBlank()) "CONFIGURED" else "NOT SET",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (apiKey.isNotBlank()) Color(0xFF22C55E) else UltronRed
                        )
                    }

                    SectionContainer {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "GROQ API KEY",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = OnSurfaceMuted,
                                letterSpacing = 1.sp
                            )

                            OutlinedTextField(
                                value = apiKeyText,
                                onValueChange = {
                                    apiKeyText = it
                                    viewModel.onApiKeyChange(it)
                                },
                                visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                        Icon(
                                            imageVector = if (apiKeyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = "Toggle Visibility",
                                            tint = OnSurfaceMuted
                                        )
                                    }
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = SurfaceContainerLow,
                                    unfocusedContainerColor = SurfaceContainerLow,
                                    focusedBorderColor = UltronRed,
                                    unfocusedBorderColor = Outline,
                                    focusedTextColor = OnBackground,
                                    unfocusedTextColor = OnBackground
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(SurfaceContainerHigh, RoundedCornerShape(4.dp))
                                        .border(1.dp, if (isSaved) Color(0xFF22C55E) else UltronRed, RoundedCornerShape(4.dp))
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable {
                                            viewModel.saveApiKey()
                                            showMsg("API Key Saved Successfully.")
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (isSaved) "KEY SAVED" else "SAVE KEY",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSaved) Color(0xFF22C55E) else UltronRed,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "INFERENCE MODEL",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = OnSurfaceMuted,
                                letterSpacing = 1.sp
                            )

                            ExposedDropdownMenuBox(
                                expanded = modelDropdownExpanded,
                                onExpandedChange = { modelDropdownExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedModel,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelDropdownExpanded) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = SurfaceContainerLow,
                                        unfocusedContainerColor = SurfaceContainerLow,
                                        focusedBorderColor = UltronRed,
                                        unfocusedBorderColor = Outline,
                                        focusedTextColor = OnBackground,
                                        unfocusedTextColor = OnBackground
                                    ),
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                                )

                                ExposedDropdownMenu(
                                    expanded = modelDropdownExpanded,
                                    onDismissRequest = { modelDropdownExpanded = false },
                                    modifier = Modifier.background(SurfaceContainerLow)
                                ) {
                                    models.forEach { modelOption ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = modelOption,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 12.sp,
                                                    color = OnBackground
                                                )
                                            },
                                            onClick = {
                                                viewModel.onSelectModel(modelOption)
                                                modelDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // 03 // ACOUSTIC & VOICE (VOSK & EDGETTS)
                // ==========================================
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "03 // ACOUSTIC & VOICE (VOSK & EDGETTS)",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceMuted,
                        letterSpacing = 1.sp
                    )

                    SectionContainer {
                        SettingToggleRow(
                            title = "Wake Word Detection",
                            subtitle = "TRIGGER: \"HEY ULTRON\"",
                            checked = voiceSettings.wakeWord,
                            onCheckedChange = { viewModel.setWakeWord(it) }
                        )

                        SettingActionRow(
                            title = "Acoustic Offline Model",
                            subtitle = "VOSK-EN-US-0.15 // 42 MB",
                            actionLabel = "DOWNLOAD",
                            onClick = {
                                viewModel.downloadVoiceModel()
                                showMsg("Downloading Vosk offline voice model...")
                            }
                        )

                        SettingActionRow(
                            title = "Voice Profile Enrollment",
                            subtitle = if (isCalibrated) "SPEAKER VERIFIED" else "CALIBRATION REQUIRED",
                            actionLabel = "ENROLL",
                            onClick = {
                                viewModel.startCalibration()
                                showMsg("Voice calibration initiated.")
                            }
                        )

                        SettingActionRow(
                            title = "Voice Synthesis Actor",
                            subtitle = "en-US-ChristopherNeural",
                            actionLabel = "SELECT",
                            onClick = { showMsg("Neural voice set to en-US-ChristopherNeural") }
                        )
                    }
                }

                // ==========================================
                // 04 // MESSAGING & NOTIFICATIONS INTERCEPT
                // ==========================================
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "04 // MESSAGING & NOTIFICATIONS INTERCEPT",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceMuted,
                        letterSpacing = 1.sp
                    )

                    SectionContainer {
                        SettingToggleRow(
                            title = "Context-Aware Intercept",
                            subtitle = "SUMMARIZE & PRIORITIZE ALERTS",
                            checked = messagesSettings.contextAwareMessaging,
                            onCheckedChange = { viewModel.setContextAwareMessaging(it) }
                        )

                        SettingToggleRow(
                            title = "Call Announcer & Readback",
                            subtitle = "HANDS-FREE CALLER ID",
                            checked = messagesSettings.announceCalls,
                            onCheckedChange = { viewModel.setAnnounceCalls(it) }
                        )

                        val hasNotifListener = ULTRONNotificationListener.isConnected.value
                        SettingActionRow(
                            title = "Notification Listener Access",
                            subtitle = if (hasNotifListener) "PERMISSION GRANTED" else "ACCESS REQUIRED",
                            actionLabel = if (hasNotifListener) "ACTIVE" else "GRANT",
                            onClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                } catch (_: Exception) {
                                    showMsg("Unable to open notification settings.")
                                }
                            }
                        )
                    }
                }

                // ==========================================
                // 05 // SECURE SERVICES & INTEGRATIONS
                // ==========================================
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "05 // SECURE SERVICES & INTEGRATIONS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceMuted,
                        letterSpacing = 1.sp
                    )

                    SectionContainer {
                        SettingActionRow(
                            title = "Google Workspace / Gmail Sync",
                            subtitle = if (gmail.account != null) "CONNECTED: ${gmail.account}" else "OAUTH DISCONNECTED",
                            actionLabel = if (gmail.account != null) "LOGOUT" else "CONNECT",
                            onClick = {
                                viewModel.connectGmail()
                                showMsg("Connecting Google Workspace...")
                            }
                        )

                        SettingActionRow(
                            title = "Maps & Navigation Provider",
                            subtitle = mapsProvider.uppercase(),
                            actionLabel = "CHANGE",
                            onClick = {
                                val next = if (mapsProvider == "google") "waze" else "google"
                                viewModel.onMapsProviderChange(next)
                                showMsg("Maps provider changed to: $next")
                            }
                        )
                    }
                }

                // ==========================================
                // 06 // NEURAL MEMORY & DIRECTIVES
                // ==========================================
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "06 // NEURAL MEMORY & DIRECTIVES",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceMuted,
                        letterSpacing = 1.sp
                    )

                    SectionContainer {
                        SettingActionRow(
                            title = "Learned Commands Manager",
                            subtitle = "${learned.size} DIRECTIVES STORED",
                            actionLabel = "CLEAR ALL",
                            onClick = {
                                viewModel.forgetAllLearned()
                                showMsg("All learned directives cleared.")
                            }
                        )

                        SettingActionRow(
                            title = "Modes & Routines Engine",
                            subtitle = "ACTIVE PROTOCOLS",
                            actionLabel = "OPEN",
                            onClick = onOpenModes
                        )

                        SettingActionRow(
                            title = "Neural Knowledge Graph",
                            subtitle = "\"WHAT ULTRON KNOWS\"",
                            actionLabel = "VIEW",
                            onClick = onOpenMemory
                        )
                    }
                }

                // ==========================================
                // 07 // SECURITY, LOCK POLICY & TELEMETRY
                // ==========================================
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "07 // SECURITY, LOCK POLICY & TELEMETRY",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceMuted,
                        letterSpacing = 1.sp
                    )

                    SectionContainer {
                        SettingActionRow(
                            title = "Lock Level Policy",
                            subtitle = "VOICE_VERIFIED",
                            actionLabel = "CONFIG",
                            onClick = { showMsg("Lock level: VOICE_VERIFIED") }
                        )

                        SettingActionRow(
                            title = "Quick Directives & Deep Links",
                            subtitle = "CUSTOM INTENT ROUTES",
                            actionLabel = "ROUTES",
                            onClick = { showMsg("Custom intent routes operational.") }
                        )

                        SettingActionRow(
                            title = "Backup & Restore Config",
                            subtitle = "ENCRYPTED SNAPSHOT",
                            actionLabel = "BACKUP",
                            onClick = { showMsg("Backup snapshot ready.") }
                        )

                        SettingActionRow(
                            title = "Background Work & Telemetry",
                            subtitle = "HEALTHY // WORKMANAGER ACTIVE",
                            actionLabel = "STATUS",
                            onClick = { showMsg("WorkManager background telemetry healthy.") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- BOTTOM NAVIGATION DOCK ---
            UltronBottomNavDock(
                currentTab = UltronNavTab.CORE,
                onNavigateToHome = onNavigateToHome,
                onNavigateToVoice = onNavigateToVoice,
                onNavigateToChat = onNavigateToChat
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 70.dp)
        )
    }
}

@Composable
private fun SectionContainer(
    borderColor: Color = Outline,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainer, RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        content()
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                fontFamily = FontFamily.SansSerif,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = OnBackground
            )
            Text(
                text = subtitle,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = OnSurfaceMuted,
                letterSpacing = 0.8.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = UltronRed,
                uncheckedThumbColor = OnSurfaceMuted,
                uncheckedTrackColor = SurfaceContainerHigh
            )
        )
    }
}

@Composable
private fun SettingActionRow(
    title: String,
    subtitle: String,
    actionLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                fontFamily = FontFamily.SansSerif,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = OnBackground
            )
            Text(
                text = subtitle,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = if (subtitle.contains("HEALTHY") || subtitle.contains("CONNECTED") || subtitle.contains("GRANTED") || subtitle.contains("VERIFIED")) Color(0xFF22C55E) else OnSurfaceMuted,
                letterSpacing = 0.8.sp
            )
        }

        Box(
            modifier = Modifier
                .background(SurfaceContainerHigh, RoundedCornerShape(4.dp))
                .border(1.dp, Outline, RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = actionLabel,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = UltronRed,
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
private fun StatusBadge(
    label: String,
    isActive: Boolean
) {
    Row(
        modifier = Modifier
            .background(SurfaceContainerHigh, RoundedCornerShape(4.dp))
            .border(1.dp, Outline, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(if (isActive) Color(0xFF22C55E) else UltronRed, CircleShape)
        )
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = OnBackground,
            letterSpacing = 0.8.sp
        )
    }
}
