package com.ultron.ai.core.capabilities

/**
 * The diagnostics screen as data: one row per check that decides whether an
 * Ultron feature works, what it means for the user, and how to fix it.
 */
object Diagnostics {

    enum class Group(val title: String) {
        ESSENTIALS("Essentials"),
        VOICE("Voice"),
        PHONE("Phone & People"),
        INTEGRATIONS("Integrations"),
        DEVICE("Device")
    }

    enum class Status {
        OK,
        PROBLEM,
        OFF,
        INFO
    }

    enum class Fix {
        MICROPHONE, POST_NOTIFICATIONS, OVERLAY, NOTIFICATION_ACCESS, BATTERY,
        CONTACTS, PHONE, CALENDAR, LOCATION, DND_ACCESS, WRITE_SETTINGS, NEARBY_DEVICES,
        ASSISTANT,
        ULTRON_SETTINGS
    }

    data class Row(
        val id: String,
        val group: Group,
        val title: String,
        val status: Status,
        val detail: String,
        val fix: Fix? = null
    )

    fun rows(c: UltronCapabilities): List<Row> = essentials(c) + voice(c) + phone(c) + integrations(c) + device(c)

    fun problems(rows: List<Row>): Int = rows.count { it.status == Status.PROBLEM }

    private class Check(
        val id: String,
        val title: String,
        val okDetail: String,
        val badDetail: String,
        val fix: Fix?,
        val optional: Boolean = false
    ) {
        fun row(group: Group, ok: Boolean) = Row(
            id, group, title,
            status = when {
                ok -> Status.OK
                optional -> Status.OFF
                else -> Status.PROBLEM
            },
            detail = if (ok) okDetail else badDetail,
            fix = if (ok) null else fix
        )
    }

    private fun essentials(c: UltronCapabilities) = listOf(
        Check("groq", "Groq API Key", "Configured", "Not set: Ultron cannot process language or respond", Fix.ULTRON_SETTINGS)
            .row(Group.ESSENTIALS, c.integrations.groqKey),
        Check("microphone", "Microphone", "Granted", "Denied: Ultron cannot hear you", Fix.MICROPHONE)
            .row(Group.ESSENTIALS, c.permissions.microphone),
        Check(
            "overlay", "Display Over Apps", "Granted",
            "Denied: assistant overlay panel cannot appear", Fix.OVERLAY
        ).row(Group.ESSENTIALS, c.permissions.overlay),
        Check(
            "post_notifications", "Notifications", "Granted",
            "Denied: Android may restrict background listening", Fix.POST_NOTIFICATIONS
        ).row(Group.ESSENTIALS, c.permissions.postNotifications)
    )

    private fun voice(c: UltronCapabilities): List<Row> {
        val v = c.voice
        val wakeWord = Check(
            "wake_word", "Wake Word 'Ultron'", "Enabled",
            "Disabled: Ultron listens only when the app is open", Fix.ULTRON_SETTINGS, optional = true
        ).row(Group.VOICE, v.wakeWordEnabled)
        val sideButton = Check(
            "assistant", "Side Button", "Calls Ultron: selected as default digital assistant",
            "Calls another assistant. Choose Ultron: Default Apps -> Digital Assistant",
            Fix.ASSISTANT, optional = true
        ).row(Group.VOICE, c.assistant.active)
        if (!v.wakeWordEnabled) return listOf(wakeWord, sideButton)
        return listOf(
            wakeWord,
            sideButton,
            Check(
                "wake_model", "Wake Model", "Downloaded",
                "Not downloaded: wake word cannot run offline", Fix.ULTRON_SETTINGS
            ).row(Group.VOICE, v.wakeModelReady),
            Check(
                "listening", "Background Listener", "Active",
                "Stopped: listener is currently inactive", Fix.ULTRON_SETTINGS
            ).row(Group.VOICE, v.listening),
            Check(
                "battery", "Battery Optimization", "Exempted",
                "Restricted: Android may sleep background service", Fix.BATTERY
            ).row(Group.VOICE, c.permissions.batteryExempt),
            Check(
                "voice_profile", "Voice Profile", "Enrolled: responds only to your voice",
                "Not enrolled: any voice can command", Fix.ULTRON_SETTINGS
            ).row(Group.VOICE, v.voiceProfile)
        )
    }

    private fun phone(c: UltronCapabilities) = listOf(
        Check(
            "notification_access", "Notification Access", "Enabled",
            "Disabled: cannot read/reply chats, control media, or announce calls",
            Fix.NOTIFICATION_ACCESS
        ).row(Group.PHONE, c.permissions.notificationListener),
        Check(
            "contacts", "Contacts", "Granted",
            "Denied: cannot resolve contacts by name", Fix.CONTACTS
        ).row(Group.PHONE, c.permissions.contacts),
        Check(
            "phone", "Phone Calls", "Granted: places calls directly",
            "Denied: opens dialer instead of calling directly", Fix.PHONE, optional = true
        ).row(Group.PHONE, c.permissions.phone),
        Check(
            "calendar", "Calendar", "Granted",
            "Denied: cannot save or reschedule calendar events directly", Fix.CALENDAR
        ).row(Group.PHONE, c.permissions.calendar),
        Check(
            "location", "Location", "Granted",
            "Denied: local weather and errand proximity reminders unavailable", Fix.LOCATION
        ).row(Group.PHONE, c.permissions.location),
        Check(
            "dnd_access", "Do Not Disturb Access", "Granted",
            "Denied: DND and silent mode open settings instead", Fix.DND_ACCESS
        ).row(Group.PHONE, c.permissions.dndAccess),
        Check(
            "write_settings", "Modify System Settings", "Granted",
            "Denied: brightness cannot be adjusted via voice", Fix.WRITE_SETTINGS, optional = true
        ).row(Group.PHONE, c.permissions.writeSettings),
        Check(
            "nearby_devices", "Nearby Devices", "Granted",
            "Denied: modes cannot trigger from Bluetooth devices", Fix.NEARBY_DEVICES, optional = true
        ).row(Group.PHONE, c.permissions.nearbyDevices)
    )

    private fun integrations(c: UltronCapabilities) = listOf(
        Check(
            "gmail", "Gmail", "Connected", "Not connected: email commands disabled",
            Fix.ULTRON_SETTINGS, optional = true
        ).row(Group.INTEGRATIONS, c.integrations.gmail)
    )

    private fun device(c: UltronCapabilities): List<Row> {
        val d = c.device
        fun info(id: String, title: String, detail: String) = Row(id, Group.DEVICE, title, Status.INFO, detail)
        fun yesNo(on: Boolean) = if (on) "Yes" else "No"
        return listOf(
            info("model", "Device", "${d.manufacturer} ${d.model} · Android API ${d.sdk}".trim()),
            info("nfc", "NFC", yesNo(d.nfc)),
            info("uwb", "UWB", yesNo(d.uwb)),
            info("stylus", "Stylus", yesNo(d.stylus)),
            info("external_display", "External Display", if (d.externalDisplay) "Connected" else "None"),
            info(
                "shizuku", "Shizuku",
                if (c.privileged.shizukuInstalled) "Installed"
                else "Not installed (optional)"
            )
        )
    }
}
