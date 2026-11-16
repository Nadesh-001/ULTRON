package com.ultron.ai.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ultron.ai.ui.theme.*
import com.ultron.ai.data.backup.SettingsTransfer
import com.ultron.ai.data.backup.SettingsArchive
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.DateFormat
import java.util.Date

/** "Transfer to another device": export everything to one encrypted file, or import one. */
@Composable
fun TransferSection(
    vm: TransferViewModel,
    say: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var exportPassphraseOpen by remember { mutableStateOf(false) }
    var exportPassphrase by remember { mutableStateOf<String?>(null) }
    var importPassphraseOpen by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val createDoc = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val pass = exportPassphrase
        if (uri != null && pass != null) {
            vm.export(uri, pass.toCharArray())
            exportPassphrase = null
        }
    }

    val openDoc = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            importPassphraseOpen = true
        }
    }

    HudPanel("Transfer & Backup", index = 15, modifier = modifier) {
        HudNote(
            "Groq key, voice profile, memory, learned commands, chat history, and settings in one " +
                "file encrypted with your passphrase. Save it and restore on any device."
        )

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HudOutlinedButton(
                onClick = { exportPassphraseOpen = true },
                modifier = Modifier.weight(1f)
            ) { Text("Export") }

            HudOutlinedButton(
                onClick = { openDoc.launch(arrayOf("*/*")) },
                modifier = Modifier.weight(1f)
            ) { Text("Import") }
        }
    }

    if (exportPassphraseOpen) {
        PassphraseDialog(
            export = true,
            onDismiss = { exportPassphraseOpen = false },
            onConfirm = { pass ->
                exportPassphraseOpen = false
                exportPassphrase = pass
                createDoc.launch("ultron-backup.ultron")
            }
        )
    }

    if (importPassphraseOpen && pendingImportUri != null) {
        PassphraseDialog(
            export = false,
            onDismiss = {
                importPassphraseOpen = false
                pendingImportUri = null
            },
            onConfirm = { pass ->
                importPassphraseOpen = false
                val uri = pendingImportUri ?: return@PassphraseDialog
                pendingImportUri = null
                vm.open(uri, pass.toCharArray())
            }
        )
    }

    val review = state as? TransferViewModel.State.Review
    if (review != null) {
        ConfirmImportDialog(
            summary = review.summary,
            onDismiss = { vm.dismiss() },
            onConfirm = { vm.confirmImport() }
        )
    }
}

@Composable
private fun PassphraseDialog(
    export: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pass by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val valid = pass.length >= SettingsArchive.MIN_PASSPHRASE && (!export || pass == confirm)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (export) "Archive Passphrase" else "Enter Passphrase") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (export) {
                    HudNote(
                        "Without this passphrase the archive cannot be unlocked by anyone. It cannot be recovered if forgotten."
                    )
                }
                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text("Passphrase") },
                    visualTransformation = PasswordVisualTransformation(),
                    supportingText = if (pass.length < SettingsArchive.MIN_PASSPHRASE) {
                        { Text("At least ${SettingsArchive.MIN_PASSPHRASE} characters") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (export) {
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it },
                        label = { Text("Confirm Passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pass) }, enabled = valid) {
                Text(if (export) "Save" else "Unlock")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ConfirmImportDialog(
    summary: SettingsTransfer.Summary,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restore data to this device?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Archive from ${date(summary.createdAt)}, Ultron ${summary.appVersion}:")
                Spacer(Modifier.height(4.dp))
                Text("• Groq API key: ${if (summary.hasApiKey) "present" else "none"}")
                Text("• Voice profile: ${if (summary.hasVoiceProfile) "present" else "none"}")
                Text("• Memory facts: ${summary.memories}")
                Text("• Learned commands: ${summary.learnedCommands}")
                Text("• Chat history: ${summary.chatMessages}")
                Text("• Errands: ${summary.errands}")
                Spacer(Modifier.height(4.dp))
                HudNote(
                    "Current local settings, memory, and chat history will be replaced."
                )
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Restore") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun date(epochMs: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMs))
