// Long lines on purpose: a table of regexes, each kept on one line so it can be read whole.
@file:Suppress("MaxLineLength")

package com.ultron.ai.core

import com.ultron.ai.domain.model.CameraMode
import com.ultron.ai.domain.model.CommandResult
import com.ultron.ai.domain.model.DeviceAction
import com.ultron.ai.core.mail.MailCommands
import com.ultron.ai.domain.model.MediaAction

class CommandRouter(private val now: () -> java.time.LocalDateTime = java.time.LocalDateTime::now) {

    private companion object {
        /** Minimum non-zero volume. */
        const val MIN_VOLUME = 10
    }

    private val openPatterns = listOf(
        Regex("^(?:open|launch|start)\\s+(.+)$", RegexOption.IGNORE_CASE),
        Regex("^(.+?)\\s+(?:open\\s+pannu|start\\s+pannu)$", RegexOption.IGNORE_CASE)
    )

    private val closePatterns = listOf(
        Regex("^(?:close|stop|quit|exit)\\s+(.+)$", RegexOption.IGNORE_CASE),
        Regex("^(.+?)\\s+(?:close\\s+pannu|moodu)$", RegexOption.IGNORE_CASE)
    )

    private val analyzeScreenPatterns = listOf(
        Regex("(?:analyze|analyse)\\s+(?:screen|display)", RegexOption.IGNORE_CASE),
        Regex("(?:what'?s on)\\s+(?:screen|the screen)", RegexOption.IGNORE_CASE)
    )

    private val analyzeFilePatterns = listOf(
        Regex("(?:analyze|analyse)\\s+(?:file)(?:\\s+(.+))?", RegexOption.IGNORE_CASE)
    )

    private val timerPatterns = listOf(
        Regex("(?:set (?:a )?timer|timer)\\s+(?:for)?\\s*(\\d+)\\s*(?:min(?:ute)?s?)", RegexOption.IGNORE_CASE),
        Regex("(?:set (?:a )?timer|timer)\\s+(?:for)?\\s*(\\d+)\\s*(?:sec(?:ond)?s?)", RegexOption.IGNORE_CASE),
        Regex("(\\d+)\\s*(?:min(?:ute)?s?|nimisham)\\s*timer(?:\\s*vai)?", RegexOption.IGNORE_CASE)
    )

    private val flashlightOff = Regex("(?:turn off|switch off|off pannu)\\s+(?:flashlight|torch|light)|(?:flashlight|torch|light)\\s+(?:off pannu|moodu|anai)", RegexOption.IGNORE_CASE)
    private val flashlightOn = Regex("(?:turn on|switch on|on pannu|podu)\\s+(?:flashlight|torch|light)|(?:flashlight|torch|light)\\s+(?:on pannu|podu)", RegexOption.IGNORE_CASE)

    private val flashlightPatterns = listOf(
        Regex("toggle\\s+(?:flashlight|torch)", RegexOption.IGNORE_CASE),
        Regex("^(?:flashlight|torch|light)$", RegexOption.IGNORE_CASE)
    )

    private val cameraWordPattern =
        Regex("camera", RegexOption.IGNORE_CASE)

    private val bareCapturePattern = Regex(
        "take\\s+a?\\s*photo|record\\s+a?\\s*video|take\\s+a?\\s*selfie|selfie",
        RegexOption.IGNORE_CASE
    )

    private val videoPattern = Regex("video|record video", RegexOption.IGNORE_CASE)
    private val selfiePattern = Regex("selfie|front camera", RegexOption.IGNORE_CASE)
    private val photoPattern = Regex(
        "photo|picture|shot", RegexOption.IGNORE_CASE
    )

    private val recorderPattern = Regex(
        "record\\s+(?:audio|sound|voice)|voice\\s+record|recorder",
        RegexOption.IGNORE_CASE
    )

    private val musicNounPattern = Regex(
        "music|track|song|player|playlist|paatu", RegexOption.IGNORE_CASE
    )
    private val trackNextPattern = Regex(
        "next|skip|adutha\\s+paatu", RegexOption.IGNORE_CASE
    )
    private val trackPrevPattern = Regex(
        "previous|prev|back|munnadi", RegexOption.IGNORE_CASE
    )
    private val pausePattern = Regex(
        "pause|niruthu", RegexOption.IGNORE_CASE
    )
    private val stopPattern = Regex(
        "stop|turn off", RegexOption.IGNORE_CASE
    )

    private val inPlayer = Regex("\\s(?:in|on)\\s+\\p{L}{2,}", RegexOption.IGNORE_CASE)

    private val barePause = Regex(
        "^(?:pause|pause music|pause the music)[.!]*$", RegexOption.IGNORE_CASE
    )

    private val nowPlayingPattern = Regex(
        "(?:what'?s\\s+(?:this\\s+song|playing)|what\\s+song\\s+is\\s+(?:this|playing)|enna\\s+paatu)",
        RegexOption.IGNORE_CASE
    )

    private val timePattern = Regex(
        "^(?:what\\s+time\\s+is\\s+it|what'?s\\s+the\\s+time|time\\s+enna(?:\\s+aachu)?|ippo\\s+time\\s+enna)[?.!]*$",
        RegexOption.IGNORE_CASE
    )
    private val playPattern = Regex(
        "play|resume|put\\s+on|podu", RegexOption.IGNORE_CASE
    )

    private val settingsVerbPattern = Regex(
        "open|show|go\\s+to|settings",
        RegexOption.IGNORE_CASE
    )

    private val searchPatterns = listOf(
        Regex("^(?:google|search|search for|look up)\\s+(.+)$", RegexOption.IGNORE_CASE)
    )

    private val reschedulePatterns = listOf(
        Regex("^(?:move|reschedule|shift)\\s+(?:(?:the\\s+)?(.+?)\\s+)?(?:to|at)\\s+(.+)$", RegexOption.IGNORE_CASE),
        Regex("^(?:change time of)\\s+(?:(.+?)\\s+)?(?:to|at)\\s+(.+)$", RegexOption.IGNORE_CASE)
    )

    private val calendarPatterns = listOf(
        Regex("^(?:remind me(?:\\s+to)?|add (?:an? )?(?:event|reminder|meeting))\\s+(.+)$", RegexOption.IGNORE_CASE),
        Regex("^(?:schedule|create (?:an? )?(?:event|meeting))\\s+(.+)$", RegexOption.IGNORE_CASE)
    )

    private val weatherPatterns = listOf(
        Regex("(?:what'?s|hows|how is)\\s+the\\s+weather(?:\\s+in\\s+(.+?))?[?.!]*$", RegexOption.IGNORE_CASE),
        Regex("^weather(?:\\s+in\\s+(.+?))?[?.!]*$", RegexOption.IGNORE_CASE),
        Regex("(?:climate|weather)\\s+epdi\\s+irukku(?:\\s+(?:in|la)\\s+(.+?))?[?.!]*$", RegexOption.IGNORE_CASE)
    )

    private val missedPatterns = listOf(
        Regex("^(?:what did i miss|what'?s new|any notifications)[?.!]*$", RegexOption.IGNORE_CASE)
    )

    private val briefPatterns = listOf(
        Regex("^(?:briefing|brief|good morning)[?.!]*$", RegexOption.IGNORE_CASE),
        Regex("^(?:what'?s today|tell me about today)[?.!]*$", RegexOption.IGNORE_CASE)
    )

    private val notePatterns = listOf(
        Regex("^(?:note(?: that| down)?|save a note|take a note)\\s+(.+)$", RegexOption.IGNORE_CASE)
    )

    private val volumeSetPattern =
        Regex("(?:volume|sound)\\s*(?:to)?\\s*(\\d{1,3})\\s*(?:%|percent)?", RegexOption.IGNORE_CASE)

    private val volumeMaxPattern =
        Regex("(?:volume|sound)\\s+(?:to\\s+)?(?:max(?:imum)?|full)", RegexOption.IGNORE_CASE)
    private val volumeMinPattern =
        Regex("(?:volume|sound)\\s+(?:to\\s+)?(?:min(?:imum)?)", RegexOption.IGNORE_CASE)

    private val findNearbyPatterns = listOf(
        Regex("(?:find|show)\\s+(?:the\\s+)?nearest\\s+(.+)$", RegexOption.IGNORE_CASE),
        Regex("where(?:'?s| is)\\s+the\\s+nearest\\s+(.+)$", RegexOption.IGNORE_CASE)
    )

    fun route(said: String): CommandResult {
        val input = Filler.strip(said)
        val whole = routeOne(input)
        if (whole is CommandResult.AnalyzeScreen || whole is CommandResult.AnalyzeFile) return whole
        val routed = when (val split = CompoundRequest.split(input, ::routeOne)) {
            is CompoundRequest.Split.Steps -> CommandResult.Sequence(split.steps)
            CompoundRequest.Split.Mixed ->
                if (whole is CommandResult.Weather) whole else CommandResult.ChatMessage(input.trim())
            CompoundRequest.Split.Single -> whole
        }
        return if (routed is CommandResult.ChatMessage) CommandResult.ChatMessage(said.trim()) else routed
    }

    private fun routeOne(input: String): CommandResult {
        val trimmed = input.trim()

        for (p in analyzeScreenPatterns) {
            if (p.containsMatchIn(trimmed)) return CommandResult.AnalyzeScreen
        }

        for (p in analyzeFilePatterns) {
            val m = p.find(trimmed) ?: continue
            return CommandResult.AnalyzeFile(m.groupValues.getOrNull(1)?.trim()?.ifBlank { null })
        }

        if (flashlightOff.containsMatchIn(trimmed)) return CommandResult.Flashlight(on = false)
        if (flashlightOn.containsMatchIn(trimmed)) return CommandResult.Flashlight(on = true)
        for (p in flashlightPatterns) {
            if (p.containsMatchIn(trimmed)) return CommandResult.ToggleFlashlight
        }

        routeCamera(trimmed)?.let { return it }
        MediaRequest.parse(trimmed)?.let { return it }
        routeMedia(trimmed)?.let { return it }
        ChatRequest.parse(trimmed)?.let { return it }
        MailCommands.parse(trimmed)?.let { mail ->
            return if (mail is MailCommands.Request.Reply) {
                CommandResult.ReplyMessage(mail.to, mail.body, ChatRequest.looseReply(trimmed))
            } else {
                CommandResult.Mail(mail)
            }
        }
        MessageRequest.parse(trimmed)?.let { return it }
        ChatRequest.parseBareReply(trimmed)?.let { return it }

        if (timePattern.matches(trimmed)) return CommandResult.TellTime

        for (p in missedPatterns) {
            if (p.containsMatchIn(trimmed)) return CommandResult.WhatDidIMiss
        }

        for (p in briefPatterns) {
            if (p.containsMatchIn(trimmed)) return CommandResult.MorningBrief
        }

        for (p in notePatterns) {
            val m = p.find(trimmed) ?: continue
            val text = m.groupValues[1].trim()
            if (text.isNotBlank()) return CommandResult.CreateNote(text)
        }

        routeDeviceControl(trimmed)?.let { return it }
        routeSettings(trimmed)?.let { return it }

        val dayOffset = dayOffsetOf(trimmed)
        WeatherRequest.parse(trimmed, dayOffset)?.let { return it }
        val weatherText = if (dayOffset == 0) trimmed else stripDayWords(trimmed)
        for (p in weatherPatterns) {
            val m = p.find(weatherText) ?: continue
            val place = m.groupValues.getOrNull(1)?.trim()?.ifBlank { null }
            return CommandResult.Weather(place, dayOffset)
        }

        for (p in reschedulePatterns) {
            val m = p.find(trimmed) ?: continue
            val whenText = m.groupValues[2].trim()
            if (whenText.isBlank() || DateTimeParser.parse(whenText) == null) continue
            val hint = m.groupValues[1].trim()
                .replace(
                    Regex("^(?:meeting|event|reminder)\\s*", RegexOption.IGNORE_CASE),
                    ""
                )
                .trim()
                .ifBlank { null }
            return CommandResult.RescheduleEvent(hint, whenText)
        }

        for (p in calendarPatterns) {
            val m = p.find(trimmed) ?: continue
            val rest = m.groupValues[1].trim()
            if (rest.isBlank()) continue
            if (DateTimeParser.parse(rest) == null) {
                if (ErrandPlaces.isLocationWorthy(rest)) {
                    return CommandResult.CreateErrand(cleanErrand(rest))
                }
                continue
            }
            return CommandResult.CreateEvent(title = stripTimeWords(rest), whenText = rest)
        }

        AlarmRequest.parse(trimmed, now())?.let { a ->
            return CommandResult.SetAlarm(a.time.hour, a.time.minute, label = null, date = a.date)
        }

        for (p in timerPatterns) {
            val m = p.find(trimmed) ?: continue
            val value = m.groupValues[1].toIntOrNull() ?: continue
            val isMinutes = m.value.lowercase().let {
                it.contains("min") || it.contains("nimisham")
            }
            val seconds = if (isMinutes) value * 60 else value
            return CommandResult.SetTimer(seconds, null)
        }

        CallRequest.parse(trimmed)?.let { return it }

        for (p in findNearbyPatterns) {
            val m = p.find(trimmed) ?: continue
            return CommandResult.FindNearby(m.groupValues[1].trim())
        }

        for (p in searchPatterns) {
            val m = p.find(trimmed) ?: continue
            return CommandResult.LookUp(m.groupValues[1].trim())
        }

        for (p in openPatterns) {
            val m = p.find(trimmed) ?: continue
            val appName = m.groupValues[1].trim().lowercase()
            return CommandResult.OpenApp(appName, com.ultron.ai.core.apps.AppMatcher.aliasFor(appName))
        }

        for (p in closePatterns) {
            val m = p.find(trimmed) ?: continue
            val appName = m.groupValues[1].trim().lowercase()
            return CommandResult.CloseApp(appName, com.ultron.ai.core.apps.AppMatcher.aliasFor(appName))
        }

        return CommandResult.ChatMessage(trimmed)
    }

    private fun cleanErrand(text: String): String = text
        .replace(
            Regex("^(?:buy|get|pick up)\\s+", RegexOption.IGNORE_CASE),
            ""
        )
        .trim()
        .ifBlank { text.trim() }

    private fun stripDayWords(text: String): String = text
        .replace(
            Regex("(?:today|tomorrow|day after tomorrow)", RegexOption.IGNORE_CASE),
            " "
        )
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun routeCamera(text: String): CommandResult? {
        val lower = text.lowercase()

        if (recorderPattern.containsMatchIn(lower)) return CommandResult.RecordAudio
        if (!cameraWordPattern.containsMatchIn(lower) &&
            !bareCapturePattern.containsMatchIn(lower)
        ) return null

        return when {
            videoPattern.containsMatchIn(lower) -> CommandResult.OpenCamera(CameraMode.VIDEO)
            selfiePattern.containsMatchIn(lower) -> CommandResult.OpenCamera(CameraMode.SELFIE)
            photoPattern.containsMatchIn(lower) -> CommandResult.OpenCamera(CameraMode.PHOTO)
            else -> CommandResult.OpenCamera(CameraMode.JUST_OPEN)
        }
    }

    private fun routeMedia(text: String): CommandResult? {
        val lower = text.lowercase()
        val named = MusicApps.match(lower)
        val aboutMusic = musicNounPattern.containsMatchIn(lower) || named != null
        return when {
            nowPlayingPattern.containsMatchIn(lower) -> CommandResult.NowPlaying
            barePause.matches(lower) -> CommandResult.MediaControl(MediaAction.PAUSE, null)
            !aboutMusic -> null
            else -> mediaAction(lower)?.let {
                CommandResult.MediaControl(it, if (named != null || inPlayer.containsMatchIn(lower)) lower else null)
            }
        }
    }

    private fun mediaAction(lower: String): MediaAction? = when {
        trackNextPattern.containsMatchIn(lower) -> MediaAction.NEXT
        trackPrevPattern.containsMatchIn(lower) -> MediaAction.PREVIOUS
        pausePattern.containsMatchIn(lower) -> MediaAction.PAUSE
        stopPattern.containsMatchIn(lower) -> MediaAction.STOP
        playPattern.containsMatchIn(lower) -> MediaAction.PLAY
        else -> null
    }

    private fun routeSettings(text: String): CommandResult? {
        val lower = text.lowercase()
        if (!settingsVerbPattern.containsMatchIn(lower)) return null
        return if (SettingsScreens.match(lower) != null) CommandResult.OpenSettings(lower) else null
    }

    private fun routeDeviceControl(text: String): CommandResult? {
        val lower = text.lowercase()
        (DevicePhrases.ringer(lower) ?: DevicePhrases.brightness(lower))?.let { return it }

        volumeSetPattern.find(lower)?.let { m ->
            m.groupValues[1].toIntOrNull()?.let { level ->
                return CommandResult.DeviceControl(DeviceAction.VOLUME_SET, level.coerceIn(0, 100))
            }
        }

        val mentionsVolume = lower.contains("volume") || lower.contains("sound")

        return when {
            volumeMaxPattern.containsMatchIn(lower) -> CommandResult.DeviceControl(DeviceAction.VOLUME_SET, 100)
            volumeMinPattern.containsMatchIn(lower) -> CommandResult.DeviceControl(DeviceAction.VOLUME_SET, MIN_VOLUME)

            lower.matches(Regex(".*(?:mute|silent|sound off).*")) ->
                CommandResult.DeviceControl(DeviceAction.MUTE)

            lower.matches(Regex(".*(?:unmute|sound on).*")) ->
                CommandResult.DeviceControl(DeviceAction.UNMUTE)

            mentionsVolume && (lower.contains("up") || lower.contains("increase") || lower.contains("louder")) ->
                CommandResult.DeviceControl(DeviceAction.VOLUME_UP)

            mentionsVolume && (lower.contains("down") || lower.contains("decrease") || lower.contains("quieter")) ->
                CommandResult.DeviceControl(DeviceAction.VOLUME_DOWN)

            lower.contains("louder") -> CommandResult.DeviceControl(DeviceAction.VOLUME_UP)
            lower.contains("quieter") -> CommandResult.DeviceControl(DeviceAction.VOLUME_DOWN)

            lower.matches(Regex(".*(?:do not disturb|dnd).*")) ->
                if (lower.contains("off")) {
                    CommandResult.DeviceControl(DeviceAction.DND_OFF)
                } else {
                    CommandResult.DeviceControl(DeviceAction.DND_ON)
                }

            lower.contains("wi-fi") || lower.contains("wifi") ->
                CommandResult.DeviceControl(
                    DevicePhrases.onOff(lower, DeviceAction.WIFI_ON, DeviceAction.WIFI_OFF, DeviceAction.OPEN_WIFI_PANEL)
                )

            lower.contains("bluetooth") ->
                CommandResult.DeviceControl(
                    DevicePhrases.onOff(lower, DeviceAction.BLUETOOTH_ON, DeviceAction.BLUETOOTH_OFF, DeviceAction.OPEN_BLUETOOTH_PANEL)
                )

            else -> null
        }
    }

    private fun stripTimeWords(text: String): String {
        val cleaned = text
            .replace(
                Regex(
                    """(?:today|tomorrow|day after tomorrow)""",
                    RegexOption.IGNORE_CASE
                ), " "
            )
            .replace(
                Regex(
                    """(?:at|on)\s*\d{1,2}(?:[:.]\d{2})?\s*(?:am|pm)?""",
                    RegexOption.IGNORE_CASE
                ), " "
            )
            .replace(
                Regex(
                    """(?:in)\s+\d+\s*(?:minutes?|mins?|hours?|days?|weeks?)""",
                    RegexOption.IGNORE_CASE
                ), " "
            )
            .replace(
                Regex(
                    """(?:monday|tuesday|wednesday|thursday|ULTRON|saturday|sunday)""",
                    RegexOption.IGNORE_CASE
                ), " "
            )
            .replace(Regex("""^\s*(?:about|that)\s+""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\s+"), " ")
            .trim(' ', ',', '.', '-', '—')

        return cleaned.ifBlank { text.trim() }
    }
}

/** Which day a weather question is about. */
private fun dayOffsetOf(text: String): Int {
    val lower = text.lowercase()
    return when {
        lower.contains("day after tomorrow") -> 2
        lower.contains("tomorrow") -> 1
        else -> 0
    }
}
