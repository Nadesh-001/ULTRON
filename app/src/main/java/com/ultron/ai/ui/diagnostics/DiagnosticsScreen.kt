package com.ultron.ai.ui.diagnostics

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ultron.ai.core.capabilities.Diagnostics
import com.ultron.ai.core.capabilities.Diagnostics.Fix
import com.ultron.ai.core.capabilities.Diagnostics.Status
import com.ultron.ai.service.ULTRONNotificationListener
import com.ultron.ai.ui.settings.requestBatteryExemption
import com.ultron.ai.ui.theme.ArcAmber
import com.ultron.ai.ui.theme.ArcCyan
import com.ultron.ai.ui.theme.ArcCyanDim
import com.ultron.ai.ui.theme.HudBackground
import com.ultron.ai.ui.theme.HudLabelStyle
import com.ultron.ai.ui.theme.HudOutlinedButton
import com.ultron.ai.ui.theme.HudPanel
import com.ultron.ai.ui.theme.HudStatus
import com.ultron.ai.ui.theme.HudTopBar
import com.ultron.ai.ui.theme.OnBackground
import com.ultron.ai.ui.theme.OnSurfaceMuted
import com.ultron.ai.ui.theme.StatusDot

/**
 * "Is everything in order?" — every permission, setting and integration a
 * ULTRON feature depends on, what its absence breaks, and one tap to fix it.
 * Drawn as a systems check: one panel per group, a lit dot per line.
 */
@Composable
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel,
    onNavigateBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    // Back from a system screen, the answer may have changed.
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    val fix = rememberFixer(onOpenSettings) { viewModel.refresh() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val current = rows
    val problems = current?.let(Diagnostics::problems)

    HudBackground {
        Column(Modifier.fillMaxSize()) {
            HudTopBar(
                title = "DIAGNOSTICS",
                status = when (problems) {
                    null -> HudStatus("Checking", ArcAmber, live = true)
                    0 -> HudStatus("All systems normal")
                    else -> HudStatus("Attention required: $problems", ArcAmber)
                },
                navigation = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OnBackground)
                    }
                },
                actions = {
                    IconButton(onClick = { scope.launch { share(context, viewModel.report()) } }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share report", tint = ArcCyan)
                    }
                }
            )
            if (current == null) return@Column
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                Diagnostics.Group.entries.forEachIndexed { i, group ->
                    val inGroup = current.filter { it.group == group }
                    if (inGroup.isEmpty()) return@forEachIndexed
                    item(key = group.name) {
                        val accent = if (inGroup.any { it.status == Status.PROBLEM }) ArcAmber else ArcCyan
                        HudPanel(group.title, index = i + 1, accent = accent) {
                            inGroup.forEach { row -> DiagnosticRow(row, onFix = fix) }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(row: Diagnostics.Row, onFix: (Fix) -> Unit) {
    val (label, tint) = statusLook(row.status)
    Row(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusDot(tint, live = row.status == Status.PROBLEM)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    row.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnBackground,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(8.dp))
                // The word carries the status as well as the colour, so it doesn't rely on colour alone.
                Text(label, style = HudLabelStyle, color = tint)
            }
            Text(row.detail, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMuted)
        }
        row.fix?.let { fix ->
            Spacer(Modifier.width(8.dp))
            HudOutlinedButton(onClick = { onFix(fix) }) { Text("Fix") }
        }
    }
}

private fun statusLook(status: Status): Pair<String, Color> = when (status) {
    Status.OK -> "OK" to ArcCyan
    Status.PROBLEM -> "FAIL" to ArcAmber
    Status.OFF -> "OFF" to OnSurfaceMuted
    Status.INFO -> "INFO" to ArcCyanDim
}

/**
 * Turns a [Fix] into the system action that grants it. A runtime permission
 * is requested in place; if Android won't show its dialog any more (denied
 * for good), the app's own settings page is opened instead, since that is
 * then the only place it can be granted.
 */
@Composable
private fun rememberFixer(onOpenSettings: () -> Unit, onChanged: () -> Unit): (Fix) -> Unit {
    val context = LocalContext.current
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val activity = context.findActivity()
        // Nothing granted and no dialog left to show: only the app's settings page can grant it now.
        // (Coarse location instead of fine is a grant, not a refusal.)
        val refusedForGood = result.values.none { it } && activity != null &&
            result.keys.none { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
        if (refusedForGood) context.openAppDetails()
        onChanged()
    }
    return { fix ->
        when (fix) {
            Fix.ULTRON_SETTINGS -> onOpenSettings()
            Fix.OVERLAY -> context.startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            Fix.NOTIFICATION_ACCESS -> ULTRONNotificationListener.openSettings(context)
            Fix.BATTERY -> requestBatteryExemption(context)
            Fix.ASSISTANT -> com.ultron.ai.ui.openDefaultAssistantSettings(context)
            Fix.DND_ACCESS -> context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            Fix.WRITE_SETTINGS -> context.startActivity(
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            else -> permissionsFor(fix).takeIf { it.isNotEmpty() }?.let(request::launch)
        }
    }
}

private fun permissionsFor(fix: Fix): Array<String> = when (fix) {
    Fix.MICROPHONE -> arrayOf(Manifest.permission.RECORD_AUDIO)
    Fix.POST_NOTIFICATIONS ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        else emptyArray()
    Fix.CONTACTS -> arrayOf(Manifest.permission.READ_CONTACTS)
    Fix.PHONE -> arrayOf(Manifest.permission.CALL_PHONE)
    Fix.CALENDAR -> arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
    Fix.LOCATION -> arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    Fix.NEARBY_DEVICES ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
        else emptyArray()
    else -> emptyArray()
}

/** The system share sheet with the report as text: mail, a messenger, notes. */
private fun share(context: Context, report: String) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, "Ultron — Diagnostic Report")
        .putExtra(Intent.EXTRA_TEXT, report)
    context.startActivity(Intent.createChooser(send, "Diagnostic Report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun Context.openAppDetails() = startActivity(
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
