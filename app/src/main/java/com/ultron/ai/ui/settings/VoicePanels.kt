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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ultron.ai.core.VoiceProfile
import com.ultron.ai.service.VoiceCalibrator
import com.ultron.ai.service.VoskModelManager
import com.ultron.ai.ui.theme.*

@Composable
fun VoicePanels(
    vm: SettingsViewModel,
    onEnrol: () -> Unit,
    say: (String) -> Unit
) {
    val context = LocalContext.current
    val voice by vm.voice.collectAsStateWithLifecycle()
    val modelState by vm.modelState.collectAsStateWithLifecycle()
    val calibration by vm.calibrationResult.collectAsStateWithLifecycle()
    val energy by vm.calibrationEnergy.collectAsStateWithLifecycle()
    val profile by vm.voiceProfile.collectAsStateWithLifecycle()
    val calibrationMessage by vm.calibrationMessage.collectAsStateWithLifecycle()

    var overlayPending by remember { mutableStateOf(false) }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) vm.setWakeWord(true) else say("Microphone permission required")
    }

    LaunchedEffect(Unit) {
        if (overlayPending && Settings.canDrawOverlays(context)) {
            overlayPending = false
            vm.setWakeWord(true)
        }
    }

    HudPanel("Voice", index = 2) {
        HudToggle(
            checked = voice.wakeWord,
            title = "Wake Word 'Ultron'",
            subtitle = "Listens silently for the wake word and opens the assistant overlay",
            onCheckedChange = { on ->
                when {
                    !on -> vm.setWakeWord(false)
                    !hasMicPermission(context) -> micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    !Settings.canDrawOverlays(context) -> {
                        overlayPending = true
                        say(OVERLAY_HINT)
                        openOverlaySettings(context)
                    }
                    modelState !is VoskModelManager.DownloadState.Ready -> say("Download voice model first")
                    else -> vm.setWakeWord(true)
                }
            }
        )

        Spacer(Modifier.height(8.dp))
        HudNote(
            "Side button: choose Ultron as digital assistant — long press will summon it, " +
                "even when wake word is off."
        )

        Spacer(Modifier.height(8.dp))
        HudOutlinedButton(
            onClick = { openAssistantSettings(context) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Select digital assistant")
        }

        Spacer(Modifier.height(16.dp))
        ModelRow(modelState, onDownload = { vm.downloadModel() })
    }

    HudPanel("Calibration", index = 3) {
        HudNote("Tunes Ultron to your voice and ambient noise for reliable wake word detection.")
        Spacer(Modifier.height(8.dp))

        if (calibration != null) {
            HudReadout("Level", "%.0f".format(energy))
            HudOutlinedButton(onClick = { vm.cancelCalibration() }, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
        } else {
            val isCalibrated = voice.wakeThreshold > 0
            HudOutlinedButton(
                onClick = {
                    if (!hasMicPermission(context)) {
                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        vm.startCalibration()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isCalibrated || calibrationMessage != null) "Recalibrate" else "Calibrate")
            }

            if (isCalibrated) {
                val r = voice
                HudReadout("Ambient noise", "%.0f".format(r.ambientNoise))
                HudReadout("Your voice", "%.0f".format(r.speechEnergy))
                HudReadout("Wake threshold", "%.0f".format(r.wakeThreshold))
                HudReadout("Speech threshold", "%.0f".format(r.speechThreshold))
                Spacer(Modifier.height(4.dp))
                HudReadout("Status", "CALIBRATED", valueColor = ArcCyan)
            }
        }
    }

    HudPanel("Voice Profile", index = 4) {
        HudReadout(
            "Access",
            when {
                profile == null -> "ANY VOICE"
                !profile!!.coversCommands -> "WAKE WORD ONLY"
                else -> "ONLY YOU"
            }
        )

        Spacer(Modifier.height(8.dp))
        HudNote(voiceProfileDescription(profile))

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HudOutlinedButton(
                onClick = onEnrol,
                modifier = Modifier.weight(1f)
            ) { Text(if (profile == null) "Record Voice" else "Re-record") }

            if (profile != null) {
                HudOutlinedButton(
                    onClick = { vm.resetVoiceProfile() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset")
                }
            }
        }
    }
}

@Composable
private fun ModelRow(state: VoskModelManager.DownloadState, onDownload: () -> Unit) {
    when (state) {
        is VoskModelManager.DownloadState.NotInstalled -> {
            HudReadout("Wake model", "NOT INSTALLED", valueColor = ErrorColor)
            HudOutlinedButton(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                Text("Download voice model (~45 MB)")
            }
        }
        is VoskModelManager.DownloadState.Downloading -> {
            HudReadout("Wake model", "DOWNLOADING ${state.progress}%")
        }
        is VoskModelManager.DownloadState.Extracting -> {
            HudReadout("Wake model", "UNPACKING")
        }
        is VoskModelManager.DownloadState.Ready -> HudReadout("Wake model", "READY", valueColor = ArcCyan)
        is VoskModelManager.DownloadState.Failed -> {
            HudReadout("Wake model", "ERROR", valueColor = ErrorColor)
            HudOutlinedButton(onClick = onDownload, modifier = Modifier.fillMaxWidth()) { Text("Retry download") }
        }
    }
}

private fun voiceProfileDescription(profile: VoiceProfile?): String {
    return if (profile == null) {
        "Ultron currently responds to any voice. Record your voice profile (${VoiceProfile.ENROLMENT_PHRASES.size} short phrases) to recognize only you."
    } else {
        "Verified for both wake word and commands. Phrases: ${profile.sampleCount}, threshold %.2f.".format(profile.threshold)
    }
}

private const val OVERLAY_HINT = "Grant 'Display over other apps' permission and turn on again"

private fun hasMicPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

private fun openAssistantSettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun openOverlaySettings(context: Context) {
    try {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
