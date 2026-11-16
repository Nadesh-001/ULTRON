package com.ultron.ai.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ultron.ai.core.VoiceProfile
import com.ultron.ai.service.FridayNotificationListener
import com.ultron.ai.service.VoiceCalibrator
import com.ultron.ai.service.VoskModelManager
import com.ultron.ai.ui.theme.ArcCyan
import com.ultron.ai.ui.theme.ErrorColor
import com.ultron.ai.ui.theme.OnBackground
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    transferViewModel: TransferViewModel,
    locationViewModel: LocationViewModel,
    quickLinksViewModel: QuickLinksViewModel,
    onBack: () -> Unit,
    onOpenDiagnostics: () -> Unit = {},
    onOpenModes: () -> Unit = {},
    onOpenMemory: () -> Unit = {},
    onEnrol: () -> Unit = {}
) {
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val isSaved by viewModel.isSaved.collectAsStateWithLifecycle()
    val models by viewModel.availableModels.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val learned by viewModel.learnedCommands.collectAsStateWithLifecycle()
    val gmail by viewModel.gmail.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun say(text: String) { scope.launch { snackbarHostState.showSnackbar(text) } }

    val context = LocalContext.current
    var notifListenerPending by remember { mutableStateOf(false) }

    val contextLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.setContextAwareMessaging(true)
        else say("Notification permission required")
    }

    LaunchedEffect(Unit) {
        if (notifListenerPending && hasNotificationListener(context)) {
            notifListenerPending = false
            viewModel.setContextAwareMessaging(true)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HudHeader(
                title = "SYSTEMS",
                status = HudStatus("Settings"),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OnBackground)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenDiagnostics) {
                        Icon(Icons.Filled.MonitorHeart, contentDescription = "Diagnostics", tint = ArcCyan)
                    }
                }
            )

            BrainPanel(
                apiKey = apiKey,
                isSaved = isSaved,
                selectedModel = selectedModel,
                models = models,
                onApiKeyChange = viewModel::onApiKeyChange,
                onSaveApiKey = viewModel::saveApiKey,
                onSelectModel = viewModel::onSelectModel
            )

            VoicePanels(vm = viewModel, onEnrol = onEnrol, say = ::say)

            MessagingPanel(
                messages = messages,
                onToggleMessaging = { on ->
                    when {
                        !on -> viewModel.setContextAwareMessaging(false)
                        !hasNotificationListener(context) -> {
                            notifListenerPending = true
                            say("Grant Notification access in Android Settings")
                            openNotificationListenerSettings(context)
                        }
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED ->
                            contextLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else -> viewModel.setContextAwareMessaging(true)
                    }
                },
                onToggleCalls = viewModel::setAnnounceCalls
            )

            GmailPanel(
                gmail = gmail,
                onConnect = {
                    val act = context as? androidx.activity.ComponentActivity
                    if (act != null) viewModel.connectGmail(act)
                },
                onDisconnect = viewModel::disconnectGmail
            )

            LearnedCommandsPanel(
                learned = learned,
                onForget = viewModel::forgetLearned,
                onForgetAll = viewModel::forgetAllLearned
            )

            MapsPanel(
                provider = viewModel.mapsProvider.collectAsStateWithLifecycle().value,
                onProviderChange = viewModel::onMapsProviderChange
            )

            HudPanel("Modes", index = 9) {
                HudNote(
                    "Voice-created multi-action routines, e.g. 'create relaxing mode: " +
                        "silent and brightness to minimum'."
                )
                Spacer(Modifier.height(8.dp))
                HudOutlinedButton(onClick = onOpenModes, modifier = Modifier.fillMaxWidth()) {
                    Text("Open Modes")
                }
            }

            HudPanel("Memory", index = 10) {
                HudNote("Structured facts Ultron knows about you extracted from conversations.")
                Spacer(Modifier.height(8.dp))
                HudOutlinedButton(onClick = onOpenMemory, modifier = Modifier.fillMaxWidth()) {
                    Text("What Ultron knows about me")
                }
            }

            TransferSection(vm = transferViewModel, say = ::say)

            LocationPanel(vm = locationViewModel, say = ::say)

            QuickLinksPanel(vm = quickLinksViewModel, say = ::say)

            LockPanel(vm = viewModel)

            BackgroundWorkStatus(vm = viewModel)
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
            snackbar = { data -> Snackbar(snackbarData = data, containerColor = OnBackground, contentColor = ArcCyan) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrainPanel(
    apiKey: String,
    isSaved: Boolean,
    selectedModel: String,
    models: List<String>,
    onApiKeyChange: (String) -> Unit,
    onSaveApiKey: () -> Unit,
    onSelectModel: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    HudPanel("Intelligence · Groq", index = 1) {
        HudReadout(
            "Status",
            if (apiKey.isBlank()) "NO KEY" else "CONFIGURED",
            valueColor = if (apiKey.isBlank()) ErrorColor else ArcCyan
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            label = { Text("Groq API Key") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedModel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Model") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                models.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model) },
                        onClick = {
                            onSelectModel(model)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        HudOutlinedButton(onClick = onSaveApiKey, modifier = Modifier.fillMaxWidth()) {
            Text(if (isSaved) "Saved" else "Save")
        }
        Spacer(Modifier.height(4.dp))
        HudNote("Key is stored encrypted (Android Keystore) and sent only to Groq.")
    }
}

@Composable
private fun MessagingPanel(
    messages: SettingsViewModel.MessageSettings,
    onToggleMessaging: (Boolean) -> Unit,
    onToggleCalls: (Boolean) -> Unit
) {
    HudPanel("Calls & Messages", index = 5) {
        HudNote(
            "Ultron aggregates WhatsApp, Telegram, and SMS messages. " +
                "Ask 'any unread messages?' to hear who wrote, and reply hands-free."
        )
        Spacer(Modifier.height(8.dp))
        HudToggle(
            checked = messages.contextAwareMessaging,
            title = "Notification listener",
            subtitle = "Collects incoming messages in memory to summarize and reply",
            onCheckedChange = onToggleMessaging
        )
        Spacer(Modifier.height(8.dp))
        HudToggle(
            checked = messages.announceCalls,
            title = "Announce calls",
            subtitle = "Announces caller name for phone, WhatsApp, or Telegram",
            onCheckedChange = onToggleCalls
        )
    }
}

@Composable
private fun GmailPanel(
    gmail: SettingsViewModel.GmailState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    HudPanel("Email · Gmail", index = 6) {
        HudReadout(
            "Account",
            gmail.account ?: "NOT CONNECTED",
            valueColor = if (gmail.account == null) ErrorColor else ArcCyan
        )
        Spacer(Modifier.height(8.dp))
        HudNote(
            "Ultron checks unread emails, reads letters, and drafts replies — sent only after your confirmation."
        )
        Spacer(Modifier.height(8.dp))
        if (gmail.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HudOutlinedButton(onClick = onConnect, enabled = !gmail.busy, modifier = Modifier.weight(1f)) {
                Text(if (gmail.account == null) "Connect Gmail" else "Reconnect")
            }
            if (gmail.account != null) {
                HudOutlinedButton(onClick = onDisconnect, enabled = !gmail.busy) { Text("Disconnect") }
            }
        }
    }
}

@Composable
private fun LearnedCommandsPanel(
    learned: List<Pair<String, String>>,
    onForget: (String) -> Unit,
    onForgetAll: () -> Unit
) {
    HudPanel("Learned Commands", index = 7) {
        HudNote(
            "Phrases the AI previously resolved and Ultron now executes directly on-device without calling the LLM."
        )
        Spacer(Modifier.height(8.dp))
        if (learned.isEmpty()) HudReadout("Learned", "0")
        else {
            learned.forEach { (phrase, action) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(phrase, style = MaterialTheme.typography.bodyMedium, color = OnBackground)
                        Text(action, style = MaterialTheme.typography.bodySmall, color = ArcCyan)
                    }
                    IconButton(onClick = { onForget(phrase) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Forget '$phrase'", tint = OnBackground)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            HudOutlinedButton(onClick = onForgetAll) { Text("Forget all") }
        }
    }
}

@Composable
private fun MapsPanel(
    provider: String,
    onProviderChange: (String) -> Unit
) {
    HudPanel("Maps Provider", index = 8) {
        HudNote("Which provider to use for 'find nearest ...' searches.")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("auto" to "Default", "google" to "Google Maps", "osm" to "OpenStreetMap").forEach { (value, label) ->
                Box(Modifier.weight(1f)) {
                    HudOutlinedButton(
                        onClick = { onProviderChange(value) },
                        selected = provider == value,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(label, maxLines = 1) }
                }
            }
        }
    }
}

private fun hasNotificationListener(context: Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners").orEmpty()
    return flat.contains(context.packageName)
}

private fun openNotificationListenerSettings(context: Context) {
    try {
        context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
