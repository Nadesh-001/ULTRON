package com.ultron.ai.ui.settings

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultron.ai.data.backup.SettingsArchive
import com.ultron.ai.data.backup.SettingsTransfer
import com.ultron.ai.service.ULTRONWakeWordService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Settings export and import, step by step: pick a file, type the
 * passphrase, (for import) see what the file holds, confirm.
 */
class TransferViewModel(
    private val appContext: Context,
    private val transfer: SettingsTransfer,
    private val modes: com.ultron.ai.core.modes.ModeStore? = null,
    private val schedules: com.ultron.ai.core.modes.ModeSchedules? = null
) : ViewModel() {

    sealed interface State {
        data object Idle : State
        data object Working : State
        /** A file opened fine; nothing is replaced until [confirmImport]. */
        data class Review(val summary: SettingsTransfer.Summary) : State
        data class Done(val message: String) : State
        data class Failed(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private var pending: SettingsArchive.Content? = null

    fun export(uri: Uri, passphrase: CharArray) = work {
        val bytes = transfer.export(passphrase)
        withContext(Dispatchers.IO) {
            appContext.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
                ?: error("Failed to open file for writing")
        }
        State.Done("Saved (${bytes.size / KB} KB). Keep your passphrase safe.")
    }

    fun open(uri: Uri, passphrase: CharArray) = work {
        val bytes = withContext(Dispatchers.IO) {
            appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("Failed to open file")
        }
        val content = transfer.read(bytes, passphrase)
        pending = content
        State.Review(transfer.summary(content))
    }

    fun confirmImport() {
        val content = pending ?: return
        work {
            transfer.apply(content)
            pending = null
            // The database was replaced under the in-memory copy.
            modes?.load()
            schedules?.let { com.ultron.ai.service.ModeAlarms(appContext).rebook(it.all(), emptySet()) }
            // Wake word, thresholds and voice profile are read when the service starts.
            if (ULTRONWakeWordService.running) {
                ULTRONWakeWordService.stop(appContext)
                ULTRONWakeWordService.start(appContext)
            }
            State.Done("Done: Settings and data imported successfully.")
        }
    }

    fun dismiss() {
        pending = null
        _state.value = State.Idle
    }

    private fun work(block: suspend () -> State) {
        _state.value = State.Working
        viewModelScope.launch {
            _state.value = try {
                block()
            } catch (e: SettingsArchive.Failure) {
                State.Failed(describe(e))
            } catch (e: Exception) {
                Log.e(TAG, "Transfer failed: ${e.javaClass.simpleName}")
                State.Failed(e.message ?: "Transfer failed")
            }
        }
    }

    private fun describe(failure: SettingsArchive.Failure): String = when (failure) {
        is SettingsArchive.Failure.NotAnArchive -> "This is not an Ultron backup archive."
        is SettingsArchive.Failure.WrongPassphraseOrDamaged -> "Incorrect passphrase or archive file is damaged."
        is SettingsArchive.Failure.NewerFormat ->
            "Archive was created by a newer version of Ultron — please update the app first."
    }

    private companion object {
        const val TAG = "Transfer"
        const val KB = 1024
    }
}
