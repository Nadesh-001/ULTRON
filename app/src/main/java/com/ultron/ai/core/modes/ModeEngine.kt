// Long lines on purpose: each reply is a Russian/English pair kept side by side.
@file:Suppress("MaxLineLength")

package com.ultron.ai.core.modes

import com.ultron.ai.agent.ActionEnvelope
import com.ultron.ai.agent.AgentTools
import com.ultron.ai.agent.LearnedCommands
import com.ultron.ai.command.CommandExecutor
import com.ultron.ai.core.DeviceController
import com.ultron.ai.domain.model.CommandResult
import java.time.LocalDateTime

/**
 * Modes, end to end: "mode mode mode: …" → compiled, saved and read
 * back; "mode mode" → its steps run, with no model call; "mode mode
 * mode" → what the steps changed is put back.
 *
 * Device settings report their own undo ([DeviceController]); music a mode
 * started is paused and a flashlight it lit is turned off ([ModeSteps.undoOf]).
 * Things that can't be taken back — a message sent, an alarm set — stay.
 * Every reply is assembled from what the steps actually reported, so a
 * missing permission is said, not hidden.
 */
// One handler per kind of request; the constructor takes its collaborators and the two clocks tests replace.
@Suppress("TooManyFunctions", "LongParameterList")
class ModeEngine(
    private val store: ModeStore,
    /** [ModeCompiler.compile]; a function so the engine is tested without Groq. */
    private val compile: suspend (name: String, description: String) -> ModeCompiler.Result,
    private val device: DeviceController,
    private val clock: () -> Long = System::currentTimeMillis,
    private val now: () -> LocalDateTime = LocalDateTime::now,
    /** Schedules and run history; without them, modes simply have no timetable. */
    private val schedules: ModeSchedules? = null,
    /** The phone's Bluetooth and Wi-Fi, to tell which device "mode" is. */
    private val links: PhoneLinks? = null,
    /** NFC tags for modes; null where the phone has none. */
    private val nfc: NfcTags? = null
) {

    private companion object {
        /** "mode" this soon after creating a mode removes it. */
        const val UNDO_WINDOW_MS = 2 * 60 * 1000L
        const val MAX_NAME_WORDS = 4

        /**
         * Steps that act for the owner towards other people. A mode that has
         * one never starts on a timer, with nobody there to stop it.
         */
        val NEEDS_OWNER = setOf("call", "send_message", "reply_message", "mail")

        /** At least half of "mode …"'s words must be in a step for it to be that step. */
        const val MIN_STEP_MATCH = 0.5
    }

    @Volatile
    private var lastCreated: Pair<String, Long>? = null

    /** The mode that just ran, for "mode, mode mode". */
    @Volatile
    private var lastRun: Pair<String, Long>? = null

    /** A correction waiting for "mode": put [step] into the mode, in place of [replaces] if set. */
    private data class Proposal(val modeId: String, val step: ActionEnvelope, val replaces: Int?, val at: Long)

    @Volatile
    private var proposal: Proposal? = null

    /** "mode mode mode mode 23:00?" after a habit was noticed, waiting for "mode". */
    private data class HabitOffer(val modeId: String, val time: java.time.LocalTime, val at: Long)

    @Volatile
    private var habit: HabitOffer? = null

    private val correctionLead = Regex(
        "^(?:no|not that|wrong)[,.!\\s]+(?:instead\\s+)?"
    )

    /**
     * The mode request in [text], or null. Run, exit and describe only count
     * when the name is one of the owner's modes: "mode mode mode" is a
     * system setting, not a mode ULTRON doesn't have.
     */
    fun route(text: String): CommandResult.Mode? =
        correction(text) ?: tagRequest(text) ?: eventRequest(text) ?: scheduleRequest(text) ?: cancelRequest(text) ?: request(text)

    private val tagPhrases = listOf(
        Regex("(?:link|write|put)\\s+(?:the\\s+)?(.+?)\\s+mode\\s+(?:to|on)\\s+(?:an?\\s+)?(?:nfc\\s+)?tag", RegexOption.IGNORE_CASE),
        Regex("(?:link|write|put)\\s+(?:mode\\s+)?(.+?)\\s+to\\s+(?:an?\\s+)?(?:nfc\\s+)?tag", RegexOption.IGNORE_CASE)
    )

    private fun tagRequest(text: String): CommandResult.Mode? {
        val t = text.trim().lowercase().trimEnd('.', '!', '?')
        val rest = tagPhrases.firstNotNullOfOrNull { it.find(t)?.groupValues?.get(1) } ?: return null
        return nameAtStart(rest)?.let { CommandResult.Mode.Tag(it.id) }
    }

    private fun eventRequest(text: String): CommandResult.Mode? {
        if (schedules == null || links == null) return null
        val r = EventPhrases.parse(text) ?: return null
        return nameAtStart(r.rest)?.let { CommandResult.Mode.OnEvent(it.id, r.trigger.key, r.target, r.onConnect, r.exit) }
    }

    /** "mode" / "mode, mode mode" just after creating a mode. */
    private fun cancelRequest(text: String): CommandResult.Mode? {
        val (id, at) = lastCreated ?: return null
        if (clock() - at > UNDO_WINDOW_MS || !LearnedCommands.isCorrection(text)) return null
        lastCreated = null
        return CommandResult.Mode.CancelCreated(id)
    }

    private fun request(text: String): CommandResult.Mode? = when (val request = ModePhrases.parse(text)) {
        is CommandResult.Mode.Run -> request.takeIf { store.find(it.name) != null }
        is CommandResult.Mode.Exit -> request.takeIf { store.find(it.name) != null }
        is CommandResult.Mode.Describe -> request.takeIf { store.find(it.name) != null }
        is CommandResult.Mode.AddTo -> request.takeIf { split(it.rest) != null }
        is CommandResult.Mode.RemoveFrom -> request.takeIf { store.find(it.name) != null }
        else -> request
    }

    private fun scheduleRequest(text: String): CommandResult.Mode? {
        if (schedules == null) return null
        return when (val r = SchedulePhrases.parse(text, now())) {
            null -> null
            is SchedulePhrases.Request.Set -> nameAtStart(r.rest)?.let {
                CommandResult.Mode.Schedule(it.id, r.exit, r.time?.hour, r.time?.minute, Days.mask(r.days))
            }
            is SchedulePhrases.Request.Clear -> nameAtStart(r.rest)?.let { CommandResult.Mode.Unschedule(it.id) }
        }
    }

    /** The saved mode whose whole name the words begin with, longest first. */
    private fun nameAtStart(rest: String): Mode? {
        val words = rest.trim().split(' ').filter { it.isNotBlank() }
        return (minOf(MAX_NAME_WORDS, words.size) downTo 1).firstNotNullOfOrNull { k ->
            store.find(words.take(k).joinToString(" "), spare = 0)
        }
    }

    /** "mode, mode lo-fi" within the window after a run: the part after "mode" is the wish. */
    private fun correction(text: String): CommandResult.Mode.Correct? {
        val (id, at) = lastRun ?: return null
        if (clock() - at > UNDO_WINDOW_MS) return null
        val lower = text.trim().lowercase()
        val lead = correctionLead.find(lower) ?: return null
        val instead = text.trim().substring(lead.range.last + 1).trim()
        return CommandResult.Mode.Correct(id, instead).takeIf { instead.split(' ').size >= 2 }
    }

    /** "mode" / "mode" to "mode mode mode mode …?"; null when nothing is asked or it isn't an answer. */
    suspend fun answerPending(text: String, russian: Boolean): String? =
        answerHabit(text, russian) ?: answerProposal(text, russian)

    private suspend fun answerProposal(text: String, russian: Boolean): String? {
        val p = proposal?.takeIf { clock() - it.at <= UNDO_WINDOW_MS } ?: return null
        val say = Say(russian)
        return when (com.ultron.ai.core.mail.Confirmation.classify(text)) {
            com.ultron.ai.core.mail.Confirmation.Answer.OTHER -> null
            com.ultron.ai.core.mail.Confirmation.Answer.NO -> {
                proposal = null
                "OK, the mode stays as it was."
            }
            com.ultron.ai.core.mail.Confirmation.Answer.YES -> {
                proposal = null
                val mode = store.byId(p.modeId) ?: return "That mode is gone."
                val steps = mode.steps.toMutableList()
                if (p.replaces != null && p.replaces in steps.indices) steps[p.replaces] = p.step else steps += p.step
                store.put(mode.copy(steps = steps))
                "Got it. ${mode.name} mode: ${ModeSteps.summary(steps, russian)}."
            }
        }
    }

    /** Carries out [request]; [runner] runs one ordinary command, [router] reads a phrase into one. */
    suspend fun handle(
        request: CommandResult.Mode,
        russian: Boolean,
        router: (String) -> CommandResult = { CommandResult.ChatMessage(it) },
        runner: suspend (CommandResult) -> String
    ): String {
        val say = Say(russian)
        return when (request) {
            is CommandResult.Mode.Create -> create(request.name, request.description, say)
            is CommandResult.Mode.Run -> store.find(request.name)?.let { run(it, say, runner) } ?: missing(request.name, say)
            is CommandResult.Mode.Exit -> store.find(request.name)?.let { exit(it, say, runner) } ?: missing(request.name, say)
            is CommandResult.Mode.Describe -> store.find(request.name)?.let { describe(it, say) } ?: missing(request.name, say)
            is CommandResult.Mode.Delete -> store.find(request.name)?.let { delete(it, say) } ?: missing(request.name, say)
            is CommandResult.Mode.CancelCreated -> cancel(request.id, say)
            is CommandResult.Mode.Correct -> correct(request, say, router, runner)
            is CommandResult.Mode.AddTo -> add(request.rest, say)
            is CommandResult.Mode.RemoveFrom -> store.find(request.name)?.let { remove(it, request.what, say) }
                ?: missing(request.name, say)
            is CommandResult.Mode.Schedule -> schedule(request, say)
            is CommandResult.Mode.Unschedule -> unschedule(request.id, say)
            is CommandResult.Mode.OnEvent -> onEvent(request, say)
            is CommandResult.Mode.Tag -> tag(request.id, say)
            CommandResult.Mode.ListAll -> list(say)
        }
    }

    private suspend fun create(name: String, description: String?, say: Say): String {
        if (description == null) {
            return "What should $name mode do? Say it in one go, e.g. \"create $name mode: play calm music and Do Not Disturb\"."
        }
        return when (val compiled = compile(name, description)) {
            is ModeCompiler.Result.Failed -> if (compiled.reason == "no_key") {
                "I need the Groq key in Settings to understand the mode."
            } else {
                "I couldn't work out what $name mode should do. Say it more concretely: what to turn on or off."
            }
            is ModeCompiler.Result.Steps -> {
                val (mode, replaced) = store.save(name, description, compiled.steps)
                lastCreated = mode.id to clock()
                val what = ModeSteps.summary(mode.steps, say.russian)
                val skipped = if (compiled.skipped.isEmpty()) "" else " Part of it is beyond what I can do, so I left it out."
                val head = if (replaced) "Updated $name mode" else "$name mode"
                "$head: $what.$skipped Say \"$name mode\" to start it, or \"cancel\" if I got it wrong."
            }
        }
    }

    private suspend fun run(
        mode: Mode,
        say: Say,
        runner: suspend (CommandResult) -> String,
        automatic: Boolean = false,
        steps: List<ActionEnvelope> = mode.steps
    ): String {
        val (messages, undo) = carryOut(steps, say, runner)
        schedules?.logRun(mode.id, clock(), automatic)
        lastRun = mode.id to clock()
        // Already on: the phone's state from before the first run is what "mode" must restore.
        store.put(
            mode.copy(undo = mode.undo ?: undo, lastRunAt = clock(), runCount = mode.runCount + 1)
        )
        val reply = "${mode.name} mode. " + sentences(messages)
        return if (automatic) reply else reply + habitQuestion(mode, say)
    }

    // --- NFC tags ---------------------------------------------------------------

    private fun tag(id: String, say: Say): String {
        val mode = store.byId(id) ?: return "That mode is gone."
        val tags = nfc?.takeIf { it.available() }
            ?: return "NFC is off or missing — turn it on and say it again."
        tags.startWriting(mode.id, mode.name)
        return "Hold an NFC tag to the back of the phone. Touching it will then turn ${mode.name} mode on and off."
    }

    /**
     * A tag or a launcher shortcut for [modeId]: the mode is toggled. Like a
     * schedule, a tag can be copied, so steps that act towards people are
     * left out.
     */
    suspend fun toggle(modeId: String, russian: Boolean, runner: suspend (CommandResult) -> String): String {
        val say = Say(russian)
        val mode = store.byId(modeId) ?: return "This tag was for a mode that no longer exists."
        return if (mode.active) exit(mode, say, runner)
        else run(mode, say, runner, automatic = true, steps = mode.steps.filterNot { it.tool in NEEDS_OWNER })
    }

    // --- events --------------------------------------------------------------

    private suspend fun onEvent(request: CommandResult.Mode.OnEvent, say: Say): String {
        val mode = store.byId(request.id) ?: return "That mode is gone."
        val timetable = schedules ?: return "Not available."
        val trigger = Trigger.of(request.trigger) ?: return "I didn't get the trigger."
        if (!request.exit && mode.steps.any { it.tool in NEEDS_OWNER }) {
            return "${mode.name} mode calls or messages someone, so I won't start it by itself."
        }
        val (value, note) = when (trigger) {
            Trigger.CHARGER -> "" to ""
            Trigger.BLUETOOTH -> bluetoothDevice(request.target, say) ?: return bluetoothHelp(request.target, say)
            Trigger.WIFI -> links?.wifi()?.let { it to "" } ?: return "Connect to that Wi-Fi network and say it again, with location on — Android won't name the network without it."
        }
        val event = timetable.addEvent(mode.id, trigger, value, request.onConnect, request.exit)
        val verb = if (request.exit) "turn off" else "turn on"
        val described = describeEvent(event, say.russian)
        val gap = if (described.startsWith("when")) ", " else " "
        return "I'll $verb ${mode.name} mode" + gap + described + "." + note
    }

    /** The device "mode mode" means: a paired one by name, else the one connected right now. */
    private suspend fun bluetoothDevice(target: String, say: Say): Pair<String, String>? {
        val bt = links?.bluetooth() ?: return null
        if (!bt.permitted) return null
        EventPhrases.device(target, bt.paired)?.let { return it to "" }
        val connected = bt.connected.singleOrNull() ?: return null
        return connected to " I took what's connected now: \"$connected\"."
    }

    private suspend fun bluetoothHelp(target: String, say: Say): String {
        val bt = links?.bluetooth()
        return if (bt != null && !bt.permitted) {
            "I need the Nearby devices permission to see Bluetooth. Turn it on in Diagnostics and say it again."
        } else {
            "I don't know which device \"$target\" is. Connect to it and say it again."
        }
    }

    /**
     * Something happened to the phone's links: runs or ends the modes waiting
     * for it, without steps that need the owner there. Returns what was done,
     * to be said or shown; null when nothing was waiting for this.
     */
    suspend fun onLink(trigger: Trigger, value: String, connected: Boolean, russian: Boolean, runner: suspend (CommandResult) -> String): String? {
        val timetable = schedules ?: return null
        val say = Say(russian)
        val replies = timetable.events()
            .filter { it.trigger == trigger && it.onConnect == connected && (trigger == Trigger.CHARGER || it.value == value) }
            .mapNotNull { e ->
                val mode = store.byId(e.modeId) ?: return@mapNotNull null
                when {
                    e.exit && mode.active -> exit(mode, say, runner)
                    e.exit -> null
                    else -> run(mode, say, runner, automatic = true, steps = mode.steps.filterNot { it.tool in NEEDS_OWNER })
                }
            }
        return replies.joinToString(" ").ifBlank { null }
    }

    // --- schedules ---------------------------------------------------------

    private suspend fun schedule(request: CommandResult.Mode.Schedule, say: Say): String {
        val mode = store.byId(request.id) ?: return "That mode is gone."
        val timetable = schedules ?: return "Schedules aren't available."
        val verb = if (request.exit) "turn off" else "turn on"
        scheduleRefusal(request, mode, verb, say)?.let { return it }
        val time = java.time.LocalTime.of(request.hour ?: 0, request.minute ?: 0)
        val s = timetable.set(mode.id, request.exit, time, Days.of(request.days))
        return "I'll $verb ${mode.name} mode " +
            ScheduleMath.describe(s.days, s.time, say.russian) + "."
    }

    /** Why [request] can't be scheduled as it is: no time said, or a mode that acts towards people. */
    private fun scheduleRefusal(request: CommandResult.Mode.Schedule, mode: Mode, verb: String, say: Say): String? = when {
        request.hour == null || request.minute == null -> "At what time should I $verb ${mode.name} mode? For example: \"$verb ${mode.name} mode every day at 11 pm\"."
        !request.exit && mode.steps.any { it.tool in NEEDS_OWNER } -> "${mode.name} mode calls or messages someone, so I won't start it on a timer with nobody there."
        else -> null
    }

    private suspend fun unschedule(id: String, say: Say): String {
        val mode = store.byId(id) ?: return "That mode is gone."
        schedules?.clear(id)
        return "${mode.name} mode no longer runs on a schedule."
    }

    /**
     * A schedule's alarm went off. Runs (or ends) the mode without the steps
     * that need the owner there, and returns what happened, for a quiet
     * notification; null when there was nothing to do.
     */
    suspend fun fire(scheduleId: String, russian: Boolean, runner: suspend (CommandResult) -> String): String? {
        val s = schedules?.byId(scheduleId) ?: return null
        val mode = store.byId(s.modeId)
        if (mode == null) {
            schedules.delete(s.id)
            return null
        }
        schedules.fired(s, clock())
        val say = Say(russian)
        return when {
            s.exit && mode.active -> exit(mode, say, runner)
            s.exit -> null
            else -> run(mode, say, runner, automatic = true, steps = mode.steps.filterNot { it.tool in NEEDS_OWNER })
        }
    }

    /**
     * After a run by hand: if the owner keeps turning this mode on around the
     * same time, offer to do it by itself. Once per mode, whatever the answer.
     */
    private suspend fun habitQuestion(mode: Mode, say: Say): String {
        val s = schedules ?: return ""
        if (s.forMode(mode.id).isNotEmpty() || s.wasSuggested(mode.id)) return ""
        val time = Habits.suggest(s.manualRuns(mode.id, clock())) ?: return ""
        s.markSuggested(mode.id)
        habit = HabitOffer(mode.id, time, clock())
        val at = "%d:%02d".format(time.hour, time.minute)
        return " You've been turning it on around $at for a few days — start it every day at $at by itself?"
    }

    private suspend fun answerHabit(text: String, russian: Boolean): String? {
        val offer = habit?.takeIf { clock() - it.at <= UNDO_WINDOW_MS } ?: return null
        val say = Say(russian)
        return when (com.ultron.ai.core.mail.Confirmation.classify(text)) {
            com.ultron.ai.core.mail.Confirmation.Answer.OTHER -> null
            com.ultron.ai.core.mail.Confirmation.Answer.NO -> {
                habit = null
                "OK, only when you say so."
            }
            com.ultron.ai.core.mail.Confirmation.Answer.YES -> {
                habit = null
                val mode = store.byId(offer.modeId) ?: return "That mode is gone."
                schedule(CommandResult.Mode.Schedule(mode.id, false, offer.time.hour, offer.time.minute, Days.mask(Days.ALL)), say)
            }
        }
    }

    /**
     * Does what the owner asked instead, then offers to keep it: in place of
     * the step of the same kind ("lo-fi" instead of "mode mode"), or as
     * a new step.
     */
    private suspend fun correct(
        request: CommandResult.Mode.Correct,
        say: Say,
        router: (String) -> CommandResult,
        runner: suspend (CommandResult) -> String
    ): String {
        lastRun = null
        val mode = store.byId(request.id) ?: return "That mode is gone."
        val command = router(request.instead)
        if (command is CommandResult.ChatMessage || command is CommandResult.Mode) {
            return "Tell me what to do instead, e.g. \"no, play lo-fi\"."
        }
        val reply = when (command) {
            is CommandResult.DeviceControl -> device.apply(command.action, command.level, say.russian).message
            else -> runner(command)
        }
        return offer(mode, command, reply, say)
    }

    /** After a correction was carried out: the question whether to keep it in [mode]. */
    private fun offer(mode: Mode, command: CommandResult, reply: String, say: Say): String {
        val step = ModeSteps.envelopeOf(command)
        if (step == null || CommandExecutor.isFailure(reply)) return reply
        val replaces = mode.steps.indexOfFirst { ModeSteps.sameKind(it, step) }.takeIf { it >= 0 }
        proposal = Proposal(mode.id, step, replaces, clock())
        val question = if (replaces != null) {
            val old = ModeSteps.describe(mode.steps[replaces], say.russian)
            " Keep this in ${mode.name} mode instead of \"$old\"?"
        } else {
            " Add this to ${mode.name} mode?"
        }
        return reply.trim().let { if (it.last() in ".!?…") it else "$it." } + question
    }

    /** "mode mode mode" → (mode mode, "mode mode"): the longest saved name at the start. */
    private fun split(rest: String): Pair<Mode, String>? {
        val words = rest.trim().split(' ').filter { it.isNotBlank() }
        for (k in minOf(MAX_NAME_WORDS, words.size - 1) downTo 1) {
            // Exact: with a spare word allowed, "mode mode" would pass for "mode".
            val mode = store.find(words.take(k).joinToString(" "), spare = 0) ?: continue
            return mode to words.drop(k).joinToString(" ")
        }
        return null
    }

    private suspend fun add(rest: String, say: Say): String {
        val (mode, what) = split(rest) ?: return "Which mode should I add it to?"
        return when (val compiled = compile(mode.name, what)) {
            is ModeCompiler.Result.Failed -> "I couldn't work out what to add to ${mode.name} mode."
            is ModeCompiler.Result.Steps -> {
                val steps = mode.steps + compiled.steps
                store.put(mode.copy(steps = steps, description = mode.description + "; " + what))
                "Added to ${mode.name} mode: ${ModeSteps.summary(compiled.steps, say.russian)}. Now it does: ${ModeSteps.summary(steps, say.russian)}."
            }
        }
    }

    /** The step [what] sounds most like, by its words; removed if it's a clear match. */
    private suspend fun remove(mode: Mode, what: String, say: Say): String {
        val scored = mode.steps.mapIndexed { i, step ->
            i to ModeNames.overlap(what, ModeSteps.describe(step, true) + " " + ModeSteps.describe(step, false) + " " + step.toJson())
        }
        val best = scored.maxByOrNull { it.second }?.takeIf { it.second >= MIN_STEP_MATCH }
            ?: return "${mode.name} mode has no such step. It does: ${ModeSteps.summary(mode.steps, say.russian)}."
        val steps = mode.steps.filterIndexed { i, _ -> i != best.first }
        store.put(mode.copy(steps = steps))
        val removed = ModeSteps.describe(mode.steps[best.first], say.russian)
        val left = if (steps.isEmpty()) "Nothing is left in it."
        else "Left: ${ModeSteps.summary(steps, say.russian)}."
        return "Removed from ${mode.name} mode: $removed. " + left
    }

    private suspend fun exit(mode: Mode, say: Say, runner: suspend (CommandResult) -> String): String {
        val undo = mode.undo ?: return "${mode.name} mode isn't on."
        val (messages, _) = carryOut(undo, say, runner)
        store.put(mode.copy(undo = null))
        return if (messages.isEmpty()) {
            "${mode.name} mode is off — nothing to put back."
        } else {
            "${mode.name} mode is off. " + sentences(messages)
        }
    }

    /** Runs [steps] in order; returns what each said and what undoes them, newest first. */
    private suspend fun carryOut(
        steps: List<ActionEnvelope>,
        say: Say,
        runner: suspend (CommandResult) -> String
    ): Pair<List<String>, List<ActionEnvelope>> {
        val messages = mutableListOf<String>()
        val undo = ArrayDeque<ActionEnvelope>()
        steps.forEach { step ->
            when (val c = (AgentTools.interpret(step.tool, step.args, now()) as? AgentTools.Call.Command)?.command) {
                null -> messages += "Couldn't do \"${ModeSteps.describe(step, say.russian)}\""
                is CommandResult.DeviceControl -> {
                    val result = device.apply(c.action, c.level, say.russian)
                    messages += result.message
                    // Newest first, but a result's own steps keep their order (level, then mute).
                    result.undo?.steps?.asReversed()?.forEach { undo.addFirst(ModeSteps.deviceEnvelope(it.action, it.level)) }
                }
                else -> {
                    val reply = runner(c)
                    messages += reply
                    if (!CommandExecutor.isFailure(reply)) ModeSteps.undoOf(step)?.let(undo::addFirst)
                }
            }
        }
        return messages to undo.toList()
    }

    private fun describe(mode: Mode, say: Say): String {
        val state = if (mode.active) "It's on now." else "It's off now."
        return "${mode.name} mode: " + ModeSteps.summary(mode.steps, say.russian) + ". " + state
    }

    private suspend fun delete(mode: Mode, say: Say): String {
        store.delete(mode.id)
        schedules?.clear(mode.id)
        val note = if (mode.active) {
            " The settings it changed stay as they are."
        } else {
            ""
        }
        return "Deleted ${mode.name} mode." + note
    }

    private suspend fun cancel(id: String, say: Say): String {
        val mode = store.byId(id) ?: return "Nothing to cancel."
        store.delete(id)
        return "Removed ${mode.name} mode. Describe it again, differently."
    }

    private fun list(say: Say): String {
        val modes = store.all()
        if (modes.isEmpty()) {
            return "No modes yet. Say, for example: \"create rest mode: silent ringer and lowest brightness\"."
        }
        val names = modes.joinToString { it.name + if (it.active) " (on)" else "" }
        return "Your modes: $names."
    }

    private fun missing(name: String, say: Say) = "There's no \"$name\" mode. Say \"create $name mode\" and what it should do."

    /** Joined so it reads as one spoken answer: each reply a sentence. */
    private fun sentences(messages: List<String>) =
        messages.filter { it.isNotBlank() }.joinToString(" ") { m -> m.trim().let { if (it.last() in ".!?…") it else "$it." } }

    private class Say(val russian: Boolean) {
        operator fun invoke(ru: String, en: String) = if (russian) ru else en
    }
}
