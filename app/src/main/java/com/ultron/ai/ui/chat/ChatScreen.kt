package com.ultron.ai.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ultron.ai.ui.chat.components.AddContextBottomSheet
import com.ultron.ai.ui.chat.components.ChatInputBar
import com.ultron.ai.ui.chat.components.MessageBubble
import com.ultron.ai.ui.chat.components.SessionDrawer
import com.ultron.ai.ui.components.UltronBottomNavDock
import com.ultron.ai.ui.components.UltronNavTab
import com.ultron.ai.ui.theme.BackgroundDark
import com.ultron.ai.ui.theme.OnBackground
import com.ultron.ai.ui.theme.OnSurfaceMuted
import com.ultron.ai.ui.theme.Outline
import com.ultron.ai.ui.theme.OutlineVariant
import com.ultron.ai.ui.theme.SurfaceContainer
import com.ultron.ai.ui.theme.SurfaceContainerLow
import com.ultron.ai.ui.theme.UltronRed
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * Cinematic ULTRON Chat Interface conforming exactly to the dark obsidian & crimson HTML HUD specification.
 * Pairs deep obsidian backgrounds (#050505) with dark metallic containers, live streaming telemetry,
 * quick directive chips, universal context picker (+), and the persistent bottom navigation dock.
 */
@Composable
fun ChatScreen(
    onNavigateToHome: () -> Unit = {},
    onNavigateToVoice: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToModes: () -> Unit = {},
    onNavigateToNotebook: () -> Unit = {},
    viewModel: ChatViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val currentSessionId by viewModel.currentSessionId.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddContextSheet by remember { mutableStateOf(false) }

    // Auto-scroll to bottom when messages update
    LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.content?.length) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Show error in snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { err ->
            snackbarHostState.showSnackbar(err)
            viewModel.onDismissError()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SurfaceContainerLow,
                drawerContentColor = OnBackground
            ) {
                SessionDrawer(
                    sessions = sessions,
                    currentSessionId = currentSessionId,
                    onSelect = { id ->
                        viewModel.onOpenSession(id)
                        scope.launch { drawerState.close() }
                    },
                    onDelete = { id -> viewModel.onDeleteSession(id) },
                    onNew = {
                        viewModel.onNewChat()
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // --- 1. Shared Top HUD Bar ---
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "History Drawer",
                                tint = OnSurfaceMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // System Active Dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(UltronRed, CircleShape)
                        )

                        Text(
                            text = "ULTRON // OS 4.2",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = OnBackground,
                            letterSpacing = 2.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "SYNC 99.8%",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = OnSurfaceMuted,
                            letterSpacing = 0.8.sp
                        )

                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier
                                .size(34.dp)
                                .background(SurfaceContainer, CircleShape)
                                .border(1.dp, Outline, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = "Settings",
                                tint = OnSurfaceMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // --- 2. Encrypted Neural Channel Header ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ENCRYPTED NEURAL CHANNEL",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = OnSurfaceMuted,
                        letterSpacing = 1.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .background(SurfaceContainer, RoundedCornerShape(4.dp))
                            .border(1.dp, Outline, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    if (uiState.isLoading) UltronRed else Color(0xFF22C55E),
                                    CircleShape
                                )
                        )
                        Text(
                            text = if (uiState.isLoading) "STREAMING" else "ONLINE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnBackground,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                // --- 3. Chat Conversation Stream ---
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    if (uiState.messages.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color(0xFF3B0808), CircleShape)
                                    .border(1.5.dp, UltronRed, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(UltronRed, CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "NEURAL LINK STANDBY",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnBackground,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Encrypted conversation channel active. Issue a directive below or trigger quick scans.",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 12.sp,
                                color = OnSurfaceMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = uiState.messages,
                                key = { it.id }
                            ) { message ->
                                MessageBubble(message = message)
                            }
                        }
                    }
                }

                // --- 4. Quick Directive Chips ---
                val chipScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(chipScrollState)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickChip(
                        label = "RUN SYSTEM SCAN",
                        onClick = {
                            viewModel.onInputChange("Run a full system and diagnostic scan")
                            viewModel.onSend()
                        }
                    )
                    QuickChip(
                        label = "VOICE DIAGNOSTICS",
                        onClick = {
                            viewModel.onInputChange("Check voice engine, acoustic calibration, and latency")
                            viewModel.onSend()
                        }
                    )
                    QuickChip(
                        label = "NETWORK TELEMETRY",
                        onClick = {
                            viewModel.onInputChange("Inspect active network latency and API routing")
                            viewModel.onSend()
                        }
                    )
                    QuickChip(
                        label = "CLEAR CONTEXT",
                        onClick = {
                            viewModel.onNewChat()
                        }
                    )
                }

                // --- 5. Glass Command Bar ---
                ChatInputBar(
                    text = inputText,
                    onTextChange = viewModel::onInputChange,
                    onSend = viewModel::onSend,
                    onMicClick = viewModel::onMicClick,
                    isLoading = uiState.isLoading,
                    isListening = uiState.isVoiceListening,
                    onToggleAddContext = { showAddContextSheet = true }
                )

                // --- 6. Persistent Bottom Navigation Dock ---
                UltronBottomNavDock(
                    currentTab = UltronNavTab.CHAT,
                    onNavigateToHome = onNavigateToHome,
                    onNavigateToVoice = onNavigateToVoice,
                    onNavigateToChat = {}
                )
            }

            // Universal Context Sheet (+)
            if (showAddContextSheet) {
                AddContextBottomSheet(
                    onDismiss = { showAddContextSheet = false },
                    onAttachFile = {
                        showAddContextSheet = false
                        viewModel.onAnalyzeFileRequested?.invoke(null)
                    },
                    onAttachFolder = {
                        showAddContextSheet = false
                        viewModel.onAnalyzeFileRequested?.invoke(null)
                    },
                    onMention = { mention ->
                        showAddContextSheet = false
                        viewModel.onInputChange("$inputText @$mention ")
                    },
                    onAction = { action ->
                        showAddContextSheet = false
                        viewModel.onInputChange(action)
                        viewModel.onSend()
                    },
                    onWebExtract = { url ->
                        showAddContextSheet = false
                        viewModel.onInputChange("Extract and summarize: $url")
                        viewModel.onSend()
                    },
                    onGoogleDrive = {
                        showAddContextSheet = false
                        viewModel.onInputChange("Search Google Drive for: ")
                    }
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
            )
        }
    }
}

@Composable
private fun QuickChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(SurfaceContainer, RoundedCornerShape(4.dp))
            .border(1.dp, Outline, RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = OnSurfaceMuted,
            letterSpacing = 0.8.sp
        )
    }
}
