package com.ultron.ai.core

/**
 * The phrase list Friday can understand with no network at all.
 *
 * Speech recognition normally goes through Groq Whisper, so losing signal
 * currently makes the assistant completely mute — no alarm, no torch, nothing,
 * exactly when you might be underground or on a plane. Device actions don't
 * need the internet to *run*, only to be *heard*, so the on-device recogniser
 * is given a small closed grammar of them.
 *
 * Deliberately narrow: a constrained grammar is far more accurate than open
 * transcription, and anything that genuinely needs the network (a question for
 * the model) is better refused clearly than half-understood.
 */
object OfflineCommands {

    /**
     * Phrases the offline decoder is allowed to return. Cyrillic only — the
     * on-device model is Russian and a word outside its vocabulary can break
     * the recogniser.
     */
    val GRAMMAR: List<String> = listOf(
        // torch
        "turn on flashlight", "turn off flashlight", "flashlight", "torch", "torch podu", "torch on", "torch off",
        // volume
        "louder", "quieter", "mute", "unmute", "volume up", "volume down",
        // apps
        "open camera", "open music", "open phone", "open messages", "open browser",
        // camera modes and recorder
        "take photo", "selfie", "record video", "voice recorder",
        // settings screens
        "open settings", "open wifi", "open bluetooth", "open nfc",
        "open display", "open sound", "open battery", "open location",
        // do not disturb
        "do not disturb", "turn on do not disturb", "turn off do not disturb",
        // ending conversation
        "stop", "bye", "goodbye", "cancel", "pothum"
    )

    /**
     * True when [text] is something the device can carry out by itself.
     *
     * Used to decide whether an offline transcription is worth acting on, or
     * whether the user should simply be told there's no connection.
     */
    fun isOfflineCapable(command: Any?): Boolean = when (command) {
        is com.ultron.ai.domain.model.CommandResult.ToggleFlashlight,
        is com.ultron.ai.domain.model.CommandResult.Flashlight,
        is com.ultron.ai.domain.model.CommandResult.SendMessage,
        is com.ultron.ai.domain.model.CommandResult.DeviceControl,
        is com.ultron.ai.domain.model.CommandResult.SetAlarm,
        is com.ultron.ai.domain.model.CommandResult.SetTimer,
        is com.ultron.ai.domain.model.CommandResult.OpenApp,
        is com.ultron.ai.domain.model.CommandResult.CloseApp,
        // Launching an activity needs no network, only the phone.
        is com.ultron.ai.domain.model.CommandResult.OpenSettings,
        is com.ultron.ai.domain.model.CommandResult.OpenCamera,
        is com.ultron.ai.domain.model.CommandResult.RecordAudio -> true
        else -> false
    }

    /** What Friday says when it heard something it can't do without a network. */
    fun offlineRefusal(russian: Boolean): String =
        "No connection — I can only control the phone right now."
}
