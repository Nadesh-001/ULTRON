package com.ultron.ai.ui.modes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.ultron.ai.core.modes.Mode
import com.ultron.ai.core.modes.ModeEvent
import com.ultron.ai.core.modes.ModeSteps
import com.ultron.ai.core.modes.Schedule
import com.ultron.ai.core.modes.ScheduleMath
import com.ultron.ai.core.modes.Trigger
import com.ultron.ai.core.modes.describeEvent
import com.ultron.ai.ui.theme.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ModesScreen(
    viewModel: ModesViewModel = org.koin.androidx.compose.koinViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val modes by viewModel.modes.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()

    ModesScreen(
        modes = modes,
        busy = busy,
        status = status,
        onRun = viewModel::run,
        onStop = viewModel::stop,
        onDelete = viewModel::delete,
        onNavigateBack = onNavigateBack,
        schedules = schedules,
        onUnschedule = viewModel::unschedule,
        events = events,
        onRemoveEvent = viewModel::removeEvent,
        onTag = viewModel::tag
    )
}

@Composable
fun ModesScreen(
    modes: List<Mode>?,
    busy: String?,
    status: String?,
    onRun: (Mode) -> Unit,
    onStop: (Mode) -> Unit,
    onDelete: (Mode) -> Unit,
    onNavigateBack: () -> Unit,
    schedules: Map<String, List<Schedule>> = emptyMap(),
    onUnschedule: (Schedule) -> Unit = {},
    events: Map<String, List<ModeEvent>> = emptyMap(),
    onRemoveEvent: (ModeEvent) -> Unit = {},
    onTag: (Mode) -> Unit = {}
) {
    var deleting by remember { mutableStateOf<Mode?>(null) }
    HudBackground {
        Column(Modifier.fillMaxSize()) {
            HudTopBar(
                title = "MODES",
                status = when {
                    modes == null -> HudStatus("Loading")
                    modes.isEmpty() -> HudStatus("None yet")
                    else -> HudStatus("Saved: ${modes.size} · active: ${modes.count { it.active }}")
                },
                navigation = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OnBackground)
                    }
                }
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                status?.let { item { StatusStrip(it) } }
                if (modes != null && modes.isEmpty()) item { HowTo() }
                items(modes.orEmpty(), key = { it.id }) { mode ->
                    ModePanel(
                        mode, index = modes.orEmpty().indexOf(mode) + 1, busy = busy == mode.id,
                        onRun = { onRun(mode) }, onStop = { onStop(mode) }, onDelete = { deleting = mode },
                        schedules = schedules[mode.id].orEmpty(), onUnschedule = onUnschedule,
                        events = events[mode.id].orEmpty(), onRemoveEvent = onRemoveEvent,
                        onTag = { onTag(mode) }
                    )
                }
                if (!modes.isNullOrEmpty()) {
                    item { HudNote("Create new modes via voice: 'create mode <name>: <actions>'.") }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
    deleting?.let { mode ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete ${mode.name} mode?") },
            text = {
                Text(
                    if (mode.active) "It is currently active: settings it modified will remain as they are."
                    else "Its routine steps will be removed."
                )
            },
            confirmButton = { TextButton(onClick = { onDelete(mode); deleting = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ModePanel(
    mode: Mode,
    index: Int,
    busy: Boolean,
    onRun: () -> Unit,
    onStop: () -> Unit,
    onDelete: () -> Unit,
    schedules: List<Schedule> = emptyList(),
    onUnschedule: (Schedule) -> Unit = {},
    events: List<ModeEvent> = emptyList(),
    onRemoveEvent: (ModeEvent) -> Unit = {},
    onTag: () -> Unit = {}
) {
    HudPanel("${mode.name} mode", index = index, accent = if (mode.active) ArcCyan else ArcCyanDim) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(if (mode.active) ArcCyan else OnSurfaceMuted, live = mode.active)
            Spacer(Modifier.width(6.dp))
            Text(
                if (mode.active) "ACTIVE" else "INACTIVE",
                style = HudLabelStyle,
                color = if (mode.active) ArcCyan else OnSurfaceMuted
            )
        }
        Text(
            "\"${mode.description}\"",
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            color = OnSurfaceMuted
        )
        mode.steps.forEachIndexed { i, step ->
            Row {
                Text("%02d".format(i + 1), style = HudLabelStyle, color = ArcCyan.copy(alpha = 0.6f))
                Spacer(Modifier.width(10.dp))
                Text(
                    ModeSteps.describe(step, russian = false),
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnBackground
                )
            }
        }
        if (schedules.isNotEmpty() || events.isNotEmpty()) Triggers(schedules, onUnschedule, events, onRemoveEvent)
        HudReadout("Runs", mode.runCount.toString())
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (mode.active) {
                HudButton(onClick = onStop, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Deactivate") }
            } else {
                HudButton(onClick = onRun, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Activate") }
            }
            HudOutlinedButton(onClick = onTag, enabled = !busy) { Text("Tag") }
            HudOutlinedButton(onClick = onDelete, enabled = !busy) { Text("Delete") }
        }
    }
}

@Composable
private fun Triggers(
    schedules: List<Schedule>,
    onUnschedule: (Schedule) -> Unit,
    events: List<ModeEvent>,
    onRemoveEvent: (ModeEvent) -> Unit
) {
    Column {
        schedules.sortedBy { it.exit }.forEach { s ->
            TriggerRow(
                Icons.Filled.Schedule,
                verb(s.exit) + ScheduleMath.describe(s.days, s.time, russian = false),
                onRemove = { onUnschedule(s) }
            )
        }
        events.forEach { e ->
            val icon = when (e.trigger) {
                Trigger.CHARGER -> Icons.Filled.BatteryChargingFull
                Trigger.WIFI -> Icons.Filled.Wifi
                Trigger.BLUETOOTH -> Icons.Filled.Bluetooth
            }
            TriggerRow(icon, verb(e.exit) + describeEvent(e, russian = false), onRemove = { onRemoveEvent(e) })
        }
    }
}

private fun verb(exit: Boolean) = if (exit) "Turns off " else "Turns on "

@Composable
private fun TriggerRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = ArcCyan, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = ArcCyan,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Close, contentDescription = "Remove: $text", tint = OnSurfaceMuted)
        }
    }
}

@Composable
private fun StatusStrip(text: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .hudFrame()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text("ULTRON", style = HudLabelStyle, color = ArcCyan)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = OnBackground)
    }
}

@Composable
private fun HowTo() {
    HudPanel("How to create a mode") {
        HudNote(
            "A mode combines multiple phone actions under one name. Create it via voice, " +
                "and Ultron will compile the steps and read them back."
        )
        Example("\"Create relaxing mode: silent ringer, Do Not Disturb, and brightness to minimum.\"")
        Example("\"Create workout mode: play upbeat music on Spotify.\"")
        HudNote("Then: '<name> mode' to activate, 'turn off <name> mode' to restore previous settings.")
        HudNote("Schedule: 'turn on rest mode every day at 11 pm'.")
        HudNote("Events: 'turn on driving mode when connecting to car bluetooth'.")
        HudNote("NFC Tag: 'link rest mode to tag' — tapping the NFC tag toggles the mode.")
    }
}

@Composable
private fun Example(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = ArcCyan)
}
