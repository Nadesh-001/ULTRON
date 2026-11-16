package com.ultron.ai.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ultron.ai.command.LockPolicy
import com.ultron.ai.service.lock.LockScreen
import com.ultron.ai.ui.theme.*
import androidx.core.app.NotificationManagerCompat
import org.koin.androidx.compose.koinViewModel

private data class LevelItem(
    val level: LockPolicy.Level,
    val title: String,
    val subtitle: String
)

private val LEVELS = listOf(
    LevelItem(
        LockPolicy.Level.SAFE_ONLY,
        "Safe Actions Only",
        "Music, alarms, timer, weather, flashlight. Calls and messages require unlock."
    ),
    LevelItem(
        LockPolicy.Level.VOICE,
        "Voice Verified",
        "Enrolled voice profile authorizes private commands; otherwise requires Android unlock."
    ),
    LevelItem(
        LockPolicy.Level.UNLOCK,
        "Always Require Unlock",
        "Calls and messages always require fingerprint, face or PIN."
    )
)

@Composable
fun LockPanel(
    lockVm: LockLevelViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val current by lockVm.level.collectAsStateWithLifecycle()
    val outcome by LockScreen.lastOutcome.collectAsStateWithLifecycle()
    val notifications = NotificationManagerCompat.from(context).areNotificationsEnabled()
    val fullScreen = LockScreen.fullScreenAllowed(context)

    HudPanel("Lock Screen Policy", index = 14, modifier = modifier) {
        HudNote(
            "Controls what Ultron does while the phone is locked. Unlock screen is provided by Android: " +
                "Ultron never sees or stores your PIN."
        )

        Spacer(Modifier.height(12.dp))
        LEVELS.forEach { item ->
            HudRadioOption(
                selected = current == item.level,
                title = item.title,
                subtitle = item.subtitle,
                onClick = { lockVm.choose(item.level) }
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(8.dp))
        HudNote(
            "Lock screen overlay shows status only (listening, processing). " +
                "Spoken text and private details appear after 'Unlock for details'."
        )

        Spacer(Modifier.height(8.dp))
        HudReadout(
            "Notifications",
            if (notifications) "ALLOWED" else "DISABLED",
            valueColor = if (notifications) ArcCyan else ErrorColor
        )
        HudReadout(
            "Full-screen intents",
            if (fullScreen) "ALLOWED" else "DENIED",
            valueColor = if (fullScreen) ArcCyan else ErrorColor
        )
        HudReadout("Last outcome", outcomeText(outcome))

        if (!fullScreen && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Spacer(Modifier.height(8.dp))
            HudNote(
                "To display overlays above lock screen, grant Ultron full-screen notification permission."
            )
            Spacer(Modifier.height(8.dp))
            HudOutlinedButton(onClick = { openFullScreenSettings(context) }, modifier = Modifier.fillMaxWidth()) {
                Text("Grant full-screen notification permission")
            }
        }
    }
}

@Composable
fun LockPanel(
    vm: Any?,
    lockVm: LockLevelViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) = LockPanel(lockVm, modifier)

private fun outcomeText(outcome: LockScreen.Outcome?): String = when (outcome) {
    null -> "NONE YET"
    LockScreen.Outcome.ACTIVITY_VISIBLE -> "PANEL VISIBLE"
    LockScreen.Outcome.NOTIFICATION_ONLY -> "NOTIFICATION ONLY"
    LockScreen.Outcome.FSI_DENIED -> "NOTIFICATION (FSI DENIED)"
    LockScreen.Outcome.NOTIFICATIONS_BLOCKED -> "VOICE ONLY"
    LockScreen.Outcome.FAILED -> "FAILED"
}

private fun openFullScreenSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
            .onFailure {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
    }
}
