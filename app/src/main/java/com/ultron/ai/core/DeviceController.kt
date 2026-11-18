package com.ultron.ai.core

import android.util.Log
import com.ultron.ai.domain.model.DeviceAction

/**
 * The phone's own settings: volume, Do Not Disturb, ringer, brightness,
 * Wi-Fi and Bluetooth.
 *
 * Every change comes back as a [DeviceActionResult] that says three things
 * a bare string can't: *what* happened (changed directly, or a settings
 * screen opened because Android forbids the change), *whether it is so*
 * (the state is read back afterwards), and *how to undo it* (the action
 * that restores what was there before). Replies claim only what was read
 * back; modes use the undo to put the phone back as it was.
 *
 * Android allows an app to change volume, ringer and brightness (the last
 * after a one-time "modify system settings" grant), and Do Not Disturb
 * after a one-time "Do Not Disturb access" grant. It does not allow
 * switching Wi-Fi or Bluetooth since Android 10: those open their panel,
 * and say so, unless they are already as asked.
 *
 * The decisions live here; the system calls are behind [DeviceKnobs], so
 * all of this is tested without a phone.
 */
@Suppress("TooManyFunctions") // one small function per kind of setting
class DeviceController(private val knobs: DeviceKnobs) {

    companion object {
        private const val TAG = "DeviceController"
        const val BRIGHTNESS_STEP = 20
        private const val DEFAULT_LEVEL = 50
        private const val MIN_BRIGHTNESS = 1
    }

    fun perform(action: DeviceAction, level: Int?, russian: Boolean = false): String =
        apply(action, level, russian).message

    fun apply(action: DeviceAction, level: Int?, russian: Boolean): DeviceActionResult = try {
        val say = Say(russian)
        when (action) {
            DeviceAction.VOLUME_UP -> volumeStep(raise = true, say)
            DeviceAction.VOLUME_DOWN -> volumeStep(raise = false, say)
            DeviceAction.VOLUME_SET -> volumeSet(level ?: DEFAULT_LEVEL, say)
            DeviceAction.MUTE -> mute(say)
            DeviceAction.UNMUTE -> unmute(say)
            DeviceAction.DND_ON -> dnd(true, say)
            DeviceAction.DND_OFF -> dnd(false, say)
            DeviceAction.RINGER_NORMAL -> ringer(Ringer.NORMAL, say)
            DeviceAction.RINGER_VIBRATE -> ringer(Ringer.VIBRATE, say)
            DeviceAction.RINGER_SILENT -> ringer(Ringer.SILENT, say)
            DeviceAction.BRIGHTNESS_SET -> brightness(level ?: DEFAULT_LEVEL, say)
            DeviceAction.BRIGHTNESS_UP -> brightness(knobs.brightnessPercent() + BRIGHTNESS_STEP, say)
            DeviceAction.BRIGHTNESS_DOWN -> brightness(knobs.brightnessPercent() - BRIGHTNESS_STEP, say)
            DeviceAction.BRIGHTNESS_AUTO -> autoBrightness(say)
            DeviceAction.WIFI_ON -> radio(Radio.WIFI, true, say)
            DeviceAction.WIFI_OFF -> radio(Radio.WIFI, false, say)
            DeviceAction.BLUETOOTH_ON -> radio(Radio.BLUETOOTH, true, say)
            DeviceAction.BLUETOOTH_OFF -> radio(Radio.BLUETOOTH, false, say)
            DeviceAction.OPEN_WIFI_PANEL -> panel(Radio.WIFI, say)
            DeviceAction.OPEN_BLUETOOTH_PANEL -> panel(Radio.BLUETOOTH, say)
        }
    } catch (e: SecurityException) {
        Log.w(TAG, "$action refused: ${e.message}")
        DeviceActionResult(
            DeviceActionResult.Mode.NEEDS_PERMISSION, null,
            Say(russian)("Android не разрешил это изменение", "Android refused that change")
        )
    } catch (e: Exception) {
        Log.e(TAG, "$action failed: ${e.message}")
        DeviceActionResult(
            DeviceActionResult.Mode.FAILED, null,
            Say(russian)("Не получилось изменить настройку", "Couldn't change that setting")
        )
    }

    // --- volume ----------------------------------------------------------
    //
    // Media volume only (STREAM_MUSIC), in the stream's own steps. A result is
    // verified when the index read back is the index asked for — never merely
    // because the level is above zero — and its undo puts back both the index
    // and the mute flag that were there before.

    /** One platform step up or down; honest at the ends of the scale. */
    private fun volumeStep(raise: Boolean, say: Say): DeviceActionResult {
        val before = knobs.mediaVolume()
        return when {
            before.fixed -> fixedVolume(say)
            !raise && before.muted ->
                DeviceActionResult.direct(true, "Media is already muted", null)
            else -> step(before, raise, say)
        }
    }

    private fun step(before: MediaVolume, raise: Boolean, say: Say): DeviceActionResult {
        // Raising a muted stream first gives back its own level, then steps from there.
        val base = if (before.muted) {
            knobs.setMediaMuted(false)
            knobs.mediaVolume()
        } else {
            before
        }
        val atEdge = if (raise) base.atMax else base.atMin
        if (atEdge) {
            val edge = if (raise) "Media volume is already at maximum"
            else "Media volume is already at minimum"
            return DeviceActionResult.direct(!base.muted, edge, restore(before).takeIf { before != base })
        }
        knobs.stepMediaVolume(raise)
        val after = knobs.mediaVolume()
        val wanted = base.index + if (raise) 1 else -1
        // Index 0 reads as muted on Android; that is the step down, not a failure.
        val verified = after.index == wanted && (wanted == 0 || !after.muted)
        return DeviceActionResult.direct(verified, volumeText(after, verified, say), restore(before))
    }

    /** An absolute level: mapped to the nearest index, then reported as read back. */
    private fun volumeSet(percent: Int, say: Say): DeviceActionResult {
        val before = knobs.mediaVolume()
        if (before.fixed) return fixedVolume(say)
        val wanted = before.indexFor(percent)
        val audible = wanted > 0
        if (before.index == wanted && before.muted != audible) {
            return DeviceActionResult.direct(true, volumeText(before, true, say, already = true), null)
        }
        knobs.setMediaVolumeIndex(wanted)
        if (audible && knobs.mediaVolume().muted) knobs.setMediaMuted(false)
        val after = knobs.mediaVolume()
        val verified = after.index == wanted && (!audible || !after.muted)
        val message = when {
            verified -> volumeText(after, true, say)
            after.index != before.index ->
                "Only got to ${after.percent}% instead of ${before.percentOf(wanted)}%"
            else -> "The volume didn't change"
        }
        // Not what was asked, but something did change: that part can still be put back.
        val undo = restore(before).takeIf { after != before }
        return if (verified) DeviceActionResult.direct(true, message, undo)
        else DeviceActionResult(DeviceActionResult.Mode.DIRECT, false, message, undo)
    }

    /** Mutes media, keeping its level for [unmute]. */
    private fun mute(say: Say): DeviceActionResult {
        val before = knobs.mediaVolume()
        if (before.fixed) return fixedVolume(say)
        if (before.muted) {
            return DeviceActionResult.direct(true, "Media is already muted", null)
        }
        knobs.setMediaMuted(true)
        val verified = knobs.mediaVolume().muted
        val message = if (verified) "Media muted"
        else "Media didn't mute"
        return DeviceActionResult.direct(verified, message, undo(DeviceAction.UNMUTE))
    }

    /** Takes the mute off and keeps the level it had; at zero, says so rather than promise sound. */
    private fun unmute(say: Say): DeviceActionResult {
        val before = knobs.mediaVolume()
        if (before.fixed) return fixedVolume(say)
        if (!before.muted) {
            val text = if (before.index > 0) {
                "Media is already on, ${before.percent}%"
            } else {
                silentText(say)
            }
            return DeviceActionResult.direct(true, text, null)
        }
        knobs.setMediaMuted(false)
        val after = knobs.mediaVolume()
        val verified = !after.muted
        val message = when {
            !verified -> "Media didn't unmute"
            after.index == 0 -> silentText(say)
            else -> callNote(after, "Media on, ${after.percent}%", say)
        }
        return DeviceActionResult.direct(verified, message, undo(DeviceAction.MUTE))
    }

    private fun volumeText(v: MediaVolume, verified: Boolean, say: Say, already: Boolean = false): String {
        val text = when {
            !verified -> "The volume didn't change"
            v.index == 0 || v.muted -> "Media muted"
            already -> "Volume is already ${v.percent}%"
            else -> "Volume ${v.percent}%"
        }
        return if (verified) callNote(v, text, say) else text
    }

    /** During a call the media slider is not the call's: say which one moved. */
    private fun callNote(v: MediaVolume, text: String, say: Say): String =
        if (v.duringCall) text + " (media; call volume unchanged)"
        else text

    private fun silentText(say: Say) =
        "Media is unmuted but its volume is zero — say, for example, \"volume 50\""

    private fun fixedVolume(say: Say) = DeviceActionResult(
        DeviceActionResult.Mode.FAILED, false,
        "This audio output has fixed volume — Android won't change it"
    )

    /** Puts back [before]: its level, then its mute flag. */
    private fun restore(before: MediaVolume): DeviceActionResult.Undo {
        val level = undo(DeviceAction.VOLUME_SET, before.percent)
        return if (before.muted) level.copy(then = undo(DeviceAction.MUTE)) else level
    }

    // --- Do Not Disturb ----------------------------------------------------

    private fun dnd(on: Boolean, say: Say): DeviceActionResult {
        if (!knobs.dndAccess()) {
            knobs.openDndAccessSettings()
            return DeviceActionResult(
                DeviceActionResult.Mode.NEEDS_PERMISSION, null,
                "I need Do Not Disturb access — I've opened the setting"
            )
        }
        val before = knobs.dndOn()
        if (before == on) return DeviceActionResult.direct(true, dndText(on, say, already = true), null)
        knobs.setDnd(on)
        val verified = knobs.dndOn() == on
        val message = if (verified) dndText(on, say, already = false)
        else "Do Not Disturb didn't change"
        val undo = undo(if (before) DeviceAction.DND_ON else DeviceAction.DND_OFF)
        return DeviceActionResult.direct(verified, message, undo)
    }

    private fun dndText(on: Boolean, say: Say, already: Boolean): String = when {
        on && already -> "Do Not Disturb is already on"
        !on && already -> "Do Not Disturb is already off"
        on -> "Do Not Disturb on"
        else -> "Do Not Disturb off"
    }

    // --- ringer --------------------------------------------------------------

    private fun ringer(mode: Ringer, say: Say): DeviceActionResult {
        val before = knobs.ringer()
        if (before == mode) return DeviceActionResult.direct(true, ringerText(mode, say, already = true), null)
        // Fully silent counts as a Do Not Disturb change since Android 7, and needs the same access.
        if ((mode == Ringer.SILENT || before == Ringer.SILENT) && !knobs.dndAccess()) {
            knobs.openDndAccessSettings()
            return DeviceActionResult(
                DeviceActionResult.Mode.NEEDS_PERMISSION, null,
                "Silent mode needs Do Not Disturb access — I've opened the setting"
            )
        }
        knobs.setRinger(mode)
        val verified = knobs.ringer() == mode
        val message = if (verified) ringerText(mode, say, already = false)
        else "The ringer didn't change"
        return DeviceActionResult.direct(verified, message, undo(before.action))
    }

    private fun ringerText(mode: Ringer, say: Say, already: Boolean): String {
        val name = when (mode) {
            Ringer.NORMAL -> "ringer on"
            Ringer.VIBRATE -> "vibrate only"
            Ringer.SILENT -> "fully silent"
        }
        return if (already) "Already $name" else name.replaceFirstChar(Char::uppercase)
    }

    // --- brightness ------------------------------------------------------------

    private fun brightness(target: Int, say: Say): DeviceActionResult {
        if (!knobs.canWriteSettings()) return needsWriteSettings(say)
        val wasAuto = knobs.brightnessAuto()
        val before = knobs.brightnessPercent()
        val wanted = target.coerceIn(MIN_BRIGHTNESS, 100)
        knobs.setBrightnessAuto(false)
        knobs.setBrightnessPercent(wanted)
        val now = knobs.brightnessPercent()
        val verified = !knobs.brightnessAuto() && kotlin.math.abs(now - wanted) <= 1
        val message = if (verified) "Brightness $now%"
        else "The brightness didn't change"
        val undo = if (wasAuto) undo(DeviceAction.BRIGHTNESS_AUTO) else undo(DeviceAction.BRIGHTNESS_SET, before)
        return DeviceActionResult.direct(verified, message, undo)
    }

    private fun autoBrightness(say: Say): DeviceActionResult {
        if (!knobs.canWriteSettings()) return needsWriteSettings(say)
        if (knobs.brightnessAuto()) {
            val already = "Auto-brightness is already on"
            return DeviceActionResult.direct(true, already, null)
        }
        val before = knobs.brightnessPercent()
        knobs.setBrightnessAuto(true)
        val verified = knobs.brightnessAuto()
        val message = if (verified) "Auto-brightness on"
        else "Auto-brightness didn't turn on"
        return DeviceActionResult.direct(verified, message, undo(DeviceAction.BRIGHTNESS_SET, before))
    }

    private fun needsWriteSettings(say: Say): DeviceActionResult {
        knobs.openWriteSettingsPermission()
        return DeviceActionResult(
            DeviceActionResult.Mode.NEEDS_PERMISSION, null,
            "To change brightness, allow ULTRON to modify system settings — I've opened that screen"
        )
    }

    // --- Wi-Fi and Bluetooth -----------------------------------------------

    private fun radio(radio: Radio, on: Boolean, say: Say): DeviceActionResult {
        val name = radio.label
        val state = knobs.radioOn(radio)
        if (state == on) {
            val already = if (on) "$name is already on"
            else "$name is already off"
            return DeviceActionResult.direct(true, already, null)
        }
        knobs.openPanel(radio)
        val wanted = if (on) "turn on" else "turn off"
        return DeviceActionResult(
            DeviceActionResult.Mode.OPENED_SYSTEM_UI, null,
            "Android doesn't let apps switch $name — I've opened its settings for you to $wanted"
        )
    }

    private fun panel(radio: Radio, say: Say): DeviceActionResult {
        knobs.openPanel(radio)
        return DeviceActionResult(
            DeviceActionResult.Mode.OPENED_SYSTEM_UI, null,
            "Opened ${radio.label} settings"
        )
    }

    private fun undo(action: DeviceAction, level: Int? = null) = DeviceActionResult.Undo(action, level)

    private class Say(private val russian: Boolean) {
        operator fun invoke(ru: String, en: String) = if (russian) ru else en
    }
}

/** What a device change did, whether it is verified, and what puts it back. */
data class DeviceActionResult(
    val mode: Mode,
    /** The state was read back: true = it is so, false = it isn't, null = can't be read. */
    val verified: Boolean?,
    val message: String,
    /** The action restoring the previous state; null when nothing changed or it can't be undone. */
    val undo: Undo? = null
) {
    enum class Mode { DIRECT, OPENED_SYSTEM_UI, NEEDS_PERMISSION, FAILED }

    /** [then] runs right after this one: a level and a mute flag are two steps to restore. */
    data class Undo(val action: DeviceAction, val level: Int?, val then: Undo? = null) {
        /** This step and the ones after it, in the order they run. */
        val steps: List<Undo> get() = generateSequence(this) { it.then }.toList()
    }

    val succeeded: Boolean get() = mode == Mode.DIRECT && verified == true

    companion object {
        fun direct(verified: Boolean, message: String, undo: Undo?) =
            DeviceActionResult(Mode.DIRECT, verified, message, undo?.takeIf { verified })
    }
}

enum class Ringer(val action: DeviceAction) {
    NORMAL(DeviceAction.RINGER_NORMAL),
    VIBRATE(DeviceAction.RINGER_VIBRATE),
    SILENT(DeviceAction.RINGER_SILENT)
}

enum class Radio(val label: String) { WIFI("Wi-Fi"), BLUETOOTH("Bluetooth") }

/** The system calls [DeviceController] needs; [AndroidDeviceKnobs] makes them. */
@Suppress("TooManyFunctions") // a read and a write per setting
interface DeviceKnobs {
    fun mediaVolume(): MediaVolume
    fun setMediaVolumeIndex(index: Int)
    /** One platform step on the media stream (ADJUST_RAISE / ADJUST_LOWER). */
    fun stepMediaVolume(raise: Boolean)
    fun setMediaMuted(muted: Boolean)
    fun dndAccess(): Boolean
    fun dndOn(): Boolean
    fun setDnd(on: Boolean)
    fun openDndAccessSettings()
    fun ringer(): Ringer
    fun setRinger(mode: Ringer)
    fun canWriteSettings(): Boolean
    fun openWriteSettingsPermission()
    fun brightnessPercent(): Int
    fun setBrightnessPercent(percent: Int)
    fun brightnessAuto(): Boolean
    fun setBrightnessAuto(auto: Boolean)
    /** Null when the state can't be read. */
    fun radioOn(radio: Radio): Boolean?
    fun openPanel(radio: Radio)
}
