@file:Suppress("MaxLineLength")

package com.ultron.ai.core.modes

import com.ultron.ai.agent.ActionEnvelope
import com.ultron.ai.domain.model.DeviceAction
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

/**
 * A mode's steps in words, and what undoes the ones that aren't device settings.
 */
object ModeSteps {

    /** One step, described in English. */
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    fun describe(e: ActionEnvelope, russian: Boolean = false): String {
        fun s(key: String) = (e.args[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
        return when (e.tool) {
            "phone_control" -> device(s("target"), s("state"), s("level"), false)
            "play" -> {
                val query = s("query").orEmpty()
                val app = s("app")?.let { " in $it" }.orEmpty()
                "play \"$query\"$app"
            }
            "media" -> when (s("action")) {
                "pause", "stop" -> "pause the music"
                "next" -> "next track"
                "previous" -> "previous track"
                else -> "resume the music"
            }
            "flashlight" -> when (s("state")) {
                "on" -> "turn the flashlight on"
                "off" -> "turn the flashlight off"
                else -> "toggle the flashlight"
            }
            "set_alarm" -> {
                val time = "%s:%02d".format(s("hour") ?: "?", s("minute")?.toIntOrNull() ?: 0)
                "alarm at $time"
            }
            "set_timer" -> {
                val minutes = (s("seconds")?.toIntOrNull() ?: 0) / SECONDS_PER_MINUTE
                "a $minutes-minute timer"
            }
            "open_app" -> "open ${s("name")}"
            "weather" -> "tell the weather"
            "briefing" -> "the morning briefing"
            "read_messages" -> "read new messages"
            "send_message" -> "message ${s("contact")}: \"${s("text").orEmpty()}\""
            "call" -> "call ${s("contact")}"
            "open_settings" -> "open ${s("screen")} settings"
            "find_nearby" -> "find nearby: ${s("query")}"
            "web_search" -> "look up: ${s("question")}"
            "create_note" -> "a note \"${s("text")}\""
            "camera" -> "open the camera"
            "voice_recorder" -> "start the voice recorder"
            "run_mode" -> "${s("name")} mode"
            "ilovepdf_automate" -> "iLovePDF ${s("action")}"
            "search_public_apis" -> "search public APIs for ${s("query")}"
            else -> e.tool
        }
    }

    private fun device(target: String?, state: String?, level: String?, russian: Boolean = false): String {
        val on = state == "on"
        return when (target) {
            "volume" -> when {
                level != null -> "volume $level%"
                state == "off" -> "mute"
                state == "up" -> "louder"
                state == "down" -> "quieter"
                else -> "unmute"
            }
            "dnd" -> if (on) "Do Not Disturb" else "Do Not Disturb off"
            "ringer" -> when (state) {
                "silent", "off" -> "fully silent"
                "vibrate" -> "vibrate only"
                else -> "ringer on"
            }
            "brightness" -> when {
                level != null -> "brightness $level%"
                state == "auto" -> "auto-brightness"
                state == "up" -> "brighter"
                else -> "dimmer"
            }
            "wifi" -> if (on) "Wi-Fi on" else "Wi-Fi off"
            "bluetooth" -> if (on) "Bluetooth on" else "Bluetooth off"
            else -> "a phone setting"
        }
    }

    fun summary(steps: List<ActionEnvelope>, russian: Boolean = false): String =
        steps.joinToString(", ") { describe(it, false) }

    fun undoOf(step: ActionEnvelope): ActionEnvelope? {
        fun s(key: String) = (step.args[key] as? JsonPrimitive)?.contentOrNull
        return when {
            step.tool == "play" && s("kind") != "video" -> envelope("media") { put("action", "pause") }
            step.tool == "media" && s("action") == "play" -> envelope("media") { put("action", "pause") }
            step.tool == "flashlight" && s("state") == "on" -> envelope("flashlight") { put("state", "off") }
            else -> null
        }
    }

    fun envelopeOf(command: com.ultron.ai.domain.model.CommandResult): ActionEnvelope? = when (command) {
        is com.ultron.ai.domain.model.CommandResult.PlayMedia -> envelope("play") {
            put("query", command.query)
            put("kind", command.kind.name.lowercase())
            command.appHint?.let { put("app", it) }
        }
        is com.ultron.ai.domain.model.CommandResult.DeviceControl -> deviceEnvelope(command.action, command.level)
        is com.ultron.ai.domain.model.CommandResult.MediaControl ->
            envelope("media") { put("action", command.action.name.lowercase()) }
        is com.ultron.ai.domain.model.CommandResult.Flashlight ->
            envelope("flashlight") { put("state", if (command.on) "on" else "off") }
        is com.ultron.ai.domain.model.CommandResult.OpenApp -> envelope("open_app") { put("name", command.appName) }
        else -> null
    }

    fun sameKind(a: ActionEnvelope, b: ActionEnvelope): Boolean {
        if (a.tool != b.tool) return false
        if (a.tool != "phone_control") return true
        return (a.args["target"] as? JsonPrimitive)?.contentOrNull == (b.args["target"] as? JsonPrimitive)?.contentOrNull
    }

    @Suppress("CyclomaticComplexMethod")
    fun deviceEnvelope(action: DeviceAction, level: Int?): ActionEnvelope {
        val (target, state) = when (action) {
            DeviceAction.VOLUME_UP -> "volume" to "up"
            DeviceAction.VOLUME_DOWN -> "volume" to "down"
            DeviceAction.VOLUME_SET -> "volume" to null
            DeviceAction.MUTE -> "volume" to "off"
            DeviceAction.UNMUTE -> "volume" to "on"
            DeviceAction.DND_ON -> "dnd" to "on"
            DeviceAction.DND_OFF -> "dnd" to "off"
            DeviceAction.RINGER_NORMAL -> "ringer" to "normal"
            DeviceAction.RINGER_VIBRATE -> "ringer" to "vibrate"
            DeviceAction.RINGER_SILENT -> "ringer" to "silent"
            DeviceAction.BRIGHTNESS_SET -> "brightness" to null
            DeviceAction.BRIGHTNESS_UP -> "brightness" to "up"
            DeviceAction.BRIGHTNESS_DOWN -> "brightness" to "down"
            DeviceAction.BRIGHTNESS_AUTO -> "brightness" to "auto"
            DeviceAction.WIFI_ON -> "wifi" to "on"
            DeviceAction.WIFI_OFF -> "wifi" to "off"
            DeviceAction.BLUETOOTH_ON -> "bluetooth" to "on"
            DeviceAction.BLUETOOTH_OFF -> "bluetooth" to "off"
            DeviceAction.OPEN_WIFI_PANEL -> "wifi" to null
            DeviceAction.OPEN_BLUETOOTH_PANEL -> "bluetooth" to null
        }
        return envelope("phone_control") {
            put("target", target)
            state?.let { put("state", it) }
            level?.let { put("level", it) }
        }
    }

    private fun envelope(tool: String, args: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit) =
        ActionEnvelope(tool, buildJsonObject(args))

    private const val SECONDS_PER_MINUTE = 60
}
