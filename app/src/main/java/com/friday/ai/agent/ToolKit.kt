package com.ultron.ai.agent

import com.ultron.ai.core.SpokenText

/**
 * Which tools a message is sent with.
 *
 * Groq's free tier allows 8,000 tokens a minute, and every round of an
 * answer carries the tools again. The whole set is about 2,000 tokens; with
 * the instructions, the memory and the conversation, one command that took
 * two or three rounds ran over the minute by itself, and the answer came
 * from the weaker backup model.
 *
 * So a message gets only what it can use:
 *  - **Light** — talk: web search, the calculator, and [ESCALATE].
 *  - **Focused** — the groups the message is about ("set an alarm" →
 *    alarms and timers; "text mom" → people), plus the light tools.
 *  - **Full** — something is clearly to be done, but nothing says what.
 *
 * A wrong guess costs one round, not a refusal: the model calls [ESCALATE]
 * and the same turn is repeated with every tool.
 */
object ToolKit {

    sealed interface Kit {
        data object LIGHT : Kit
        data object FULL : Kit

        /** The tools of [groups], plus the light ones. */
        data class Focused(val groups: Set<Group>) : Kit
    }

    /** Tools that belong together, and the words that call for them in English and Tanglish. */
    enum class Group(val tools: Set<String>, val stems: List<String>) {
        TIME(
            setOf("set_alarm", "set_timer"),
            listOf("alarm", "timer", "wake", "clock", "ezhuppu", "ezhundhiru", "time")
        ),
        PEOPLE(
            setOf("call", "send_message", "read_messages", "reply_message", "mail"),
            listOf(
                "call", "dial", "text", "message", "reply", "whatsapp", "telegram", "mail",
                "email", "sms", "phone", "pesu", "anupu", "solleenga", "sollu", "unread"
            )
        ),
        MEDIA(
            setOf("play", "media"),
            listOf(
                "spotify", "youtube", "music", "song", "track", "album", "playlist", "play",
                "pause", "skip", "next", "previous", "resume", "stop", "paatu", "paata"
            )
        ),
        PHONE(
            setOf("phone_control", "flashlight", "open_settings", "camera", "voice_recorder", "open_app", "ilovepdf_automate"),
            listOf(
                "volume", "louder", "quieter", "mute", "flashlight", "torch", "light",
                "brightness", "wifi", "bluetooth", "settings", "camera", "photo", "selfie",
                "recorder", "app", "audio", "nfc", "sound", "athigam", "kora", "podu", "moodu",
                "pdf", "ilovepdf", "compress", "merge", "convert"
            )
        ),
        PLANS(
            setOf("create_event", "move_event", "create_note", "remind_near_place"),
            listOf(
                "calendar", "meeting", "event", "note", "remind", "reminder", "reschedule",
                "schedule", "gnyabagam", "kuripu", "todo"
            )
        ),
        INFO(
            setOf("weather", "find_nearby", "briefing", "search_public_apis"),
            listOf(
                "weather", "rain", "temperature", "forecast", "climate", "nearby", "nearest",
                "briefing", "missed", "news", "mazhai", "api", "apis", "public-api", "endpoint"
            )
        ),
        MODES(
            setOf("run_mode"),
            listOf("mode", "routine", "rejimu")
        )
    }

    /** The tool the model calls to get the phone's tools. */
    const val ESCALATE = "use_phone"

    /** What any kit carries: looking things up and arithmetic. */
    val LIGHT_TOOLS = setOf("web_search", "calculate")

    /**
     * Action verbs in English and Tanglish that ask for something to be carried out.
     */
    private val actionVerbs = listOf(
        "turn", "set", "call", "text", "send", "open", "close", "start", "stop", "play", "pause",
        "remind", "wake", "take", "find", "read", "reply", "check", "message", "dial", "skip",
        "mute", "create", "launch", "adjust", "record",
        // Tanglish action verbs
        "pannu", "podu", "vai", "kodu", "eduda", "edunga", "solla", "anupu", "moodu", "kelu"
    )

    fun forText(text: String): Kit {
        val words = SpokenText.normalise(text).split(' ').filter { it.isNotBlank() }
        val groups = Group.entries.filter { g -> words.any { w -> g.stems.any { w.startsWith(it) } } }.toSet()
        val acts = words.any { w -> actionVerbs.any { w.startsWith(it) } }
        return when {
            groups.isNotEmpty() -> Kit.Focused(groups)
            // Something is to be done, but nothing says what: everything.
            acts -> Kit.FULL
            else -> Kit.LIGHT
        }
    }

    /** The names of the tools in [kit]; null for every tool. */
    fun names(kit: Kit): Set<String>? = when (kit) {
        Kit.FULL -> null
        Kit.LIGHT -> LIGHT_TOOLS
        is Kit.Focused -> LIGHT_TOOLS + kit.groups.flatMap { it.tools }
    }
}
