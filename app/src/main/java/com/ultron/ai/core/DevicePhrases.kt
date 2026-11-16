package com.ultron.ai.core

import com.ultron.ai.domain.model.CommandResult
import com.ultron.ai.domain.model.DeviceAction

/**
 * How the phone's own settings are asked for in words: the ringer
 * ("text text", "text text"), brightness ("text text 30",
 * "text", "text") and whether a radio phrase means on or off.
 * Part of [CommandRouter]; kept apart so the router stays a list of kinds
 * of request.
 */
internal object DevicePhrases {

    private const val MIN_BRIGHTNESS = 5
    private const val MAX_LEVEL = 100

    private val offWords = Regex("(?:off|disable|turn off|switch off|off pannu|niruthu)")
    private val onWords = Regex("(?:on|enable|turn on|switch on|on pannu|podu)")

    private val ringerSilentPattern = Regex(
        "(?:silent mode|phone on silent|mute|silent)"
    )
    private val ringerVibratePattern = Regex("(?:vibrate|vibration|vibrate mode)")
    private val ringerNormalPattern = Regex(
        "(?:ringer on|turn off silent|unmute|normal mode|sound on)"
    )

    private val brightnessSetPattern =
        Regex("(?:brightness|velicham)\\s*(?:to)?\\s*(\\d{1,3})\\s*(?:%|percent)?")
    private val autoBrightnessPattern = Regex("(?:auto.?brightness|automatic brightness)")

    /** "turn on …" / "turn off …" / neither: which of the three a radio phrase asks for. */
    fun onOff(lower: String, on: DeviceAction, off: DeviceAction, neither: DeviceAction): DeviceAction = when {
        offWords.containsMatchIn(lower) -> off
        onWords.containsMatchIn(lower) -> on
        else -> neither
    }

    /** The ringer, not the media volume. */
    fun ringer(lower: String): CommandResult? = when {
        ringerNormalPattern.containsMatchIn(lower) -> CommandResult.DeviceControl(DeviceAction.RINGER_NORMAL)
        ringerVibratePattern.containsMatchIn(lower) -> CommandResult.DeviceControl(
            if (offWords.containsMatchIn(lower)) DeviceAction.RINGER_NORMAL else DeviceAction.RINGER_VIBRATE
        )
        ringerSilentPattern.containsMatchIn(lower) -> CommandResult.DeviceControl(DeviceAction.RINGER_SILENT)
        else -> null
    }

    fun brightness(lower: String): CommandResult? {
        val mentions = lower.contains("brightness") || lower.contains("velicham")
        brightnessSetPattern.find(lower)?.let { m ->
            m.groupValues[1].toIntOrNull()?.let {
                return CommandResult.DeviceControl(DeviceAction.BRIGHTNESS_SET, it.coerceIn(MIN_BRIGHTNESS, MAX_LEVEL))
            }
        }
        return when {
            autoBrightnessPattern.containsMatchIn(lower) -> CommandResult.DeviceControl(DeviceAction.BRIGHTNESS_AUTO)
            mentions && Regex("max|maximum|full").containsMatchIn(lower) ->
                CommandResult.DeviceControl(DeviceAction.BRIGHTNESS_SET, MAX_LEVEL)
            mentions && Regex("min|minimum|lowest").containsMatchIn(lower) ->
                CommandResult.DeviceControl(DeviceAction.BRIGHTNESS_SET, MIN_BRIGHTNESS)
            Regex("(?:^|\\s)brighter(?:\\s|$)|screen brighter").containsMatchIn(lower) ||
                mentions && Regex("up|increase|athigam").containsMatchIn(lower) ->
                CommandResult.DeviceControl(DeviceAction.BRIGHTNESS_UP)
            Regex("(?:^|\\s)dimmer(?:\\s|$)|screen dimmer").containsMatchIn(lower) ||
                mentions && Regex("down|decrease|kora").containsMatchIn(lower) ->
                CommandResult.DeviceControl(DeviceAction.BRIGHTNESS_DOWN)
            else -> null
        }
    }
}
