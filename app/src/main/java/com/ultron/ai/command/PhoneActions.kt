package com.ultron.ai.command

import com.ultron.ai.core.AlarmRequest
import com.ultron.ai.core.AlarmSetter
import com.ultron.ai.core.AppLauncher
import com.ultron.ai.core.DeviceController
import com.ultron.ai.core.MediaControls
import com.ultron.ai.core.MediaLauncher
import com.ultron.ai.core.MediaSearch
import com.ultron.ai.core.people.Caller
import com.ultron.ai.core.people.Messenger
import com.ultron.ai.service.messages.MessageAssistant
import com.ultron.ai.data.local.dao.UserPreferenceDao
import com.ultron.ai.domain.model.CommandResult
import java.time.LocalDateTime
import java.time.LocalTime

/** Commands that operate the phone: apps, calls, settings, camera, playback. */
// One collaborator per kind of thing the phone does; grouping them would only hide that.
@Suppress("LongParameterList")
class PhoneActions(
    private val apps: AppLauncher,
    private val device: DeviceController,
    private val media: MediaLauncher,
    private val playback: MediaControls,
    private val prefDao: UserPreferenceDao,
    private val alarms: AlarmSetter,
    private val messenger: Messenger,
    private val mediaSearch: MediaSearch,
    private val caller: Caller,
    private val messages: MessageAssistant,
    /** Holds a "text text?" for calls and messages alike. */
    private val people: com.ultron.ai.core.people.PeopleDirectory? = null
) {

    /** The answer to "text text?" or to "text SMS…?", carried out; null when [text] is neither. */
    suspend fun answerPending(text: String, russian: Boolean): String? =
        people?.answerPending(text) ?: messenger.answerPending(text, russian)

    /** Forgets questions that got no answer; true when one was waiting. */
    fun dropPending(): Boolean {
        val asked = people?.drop() == true
        return messenger.dropPending() || asked
    }

    suspend fun run(c: CommandResult.Phone, russian: Boolean): String = when (c) {
        is CommandResult.OpenApp -> apps.openApp(c.packageHint, c.appName, russian)
        is CommandResult.CloseApp -> apps.closeApp(c.packageHint, c.appName)
        is CommandResult.PhoneCall -> caller.call(c.target, c.via, russian)
        is CommandResult.ReplyMessage -> messages.reply(c.to, c.body, russian, c.loose)
        is CommandResult.SendMessage -> messenger.message(c.target, c.body, c.via, russian)
        is CommandResult.PlayMedia -> mediaSearch.play(c.query, c.kind, c.appHint, russian)
        is CommandResult.SetAlarm -> setAlarm(c, russian)
        is CommandResult.SetTimer -> apps.setTimer(c.seconds, c.label)
        is CommandResult.ToggleFlashlight -> apps.setFlashlight(null)
        is CommandResult.Flashlight -> apps.setFlashlight(c.on)
        is CommandResult.WebSearch -> apps.webSearch(c.query)
        is CommandResult.FindNearby -> apps.findNearby(c.query, prefDao.get("maps_provider") ?: "auto")
        is CommandResult.DeviceControl -> device.perform(c.action, c.level, russian)
        is CommandResult.OpenSettings -> media.openSettings(c.phrase)
        is CommandResult.OpenCamera -> media.openCamera(c.mode)
        is CommandResult.RecordAudio -> media.recordAudio()
        is CommandResult.MediaControl -> playback.perform(c.action, c.appHint, russian)
        is CommandResult.NowPlaying -> playback.nowPlaying(russian)
        is CommandResult.ListApps -> apps.listApps(c.query)
        is CommandResult.ILovePdfAutomate -> apps.automatePdf(c.action, c.fileHint)
    }

    private suspend fun setAlarm(c: CommandResult.SetAlarm, russian: Boolean): String {
        val time = LocalTime.of(c.hour, c.minute)
        // A day the Clock cannot ring on is refused, not quietly moved.
        c.date?.let { return AlarmRequest.wrongDay(AlarmRequest.Alarm(time, it), LocalDateTime.now(), russian) }
        return alarms.set(time, c.label, russian)
    }
}
