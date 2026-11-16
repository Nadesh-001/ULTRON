package com.ultron.ai.service.mail

import android.util.Log
import com.ultron.ai.core.ContactMatcher
import com.ultron.ai.core.ContactsReader
import com.ultron.ai.core.mail.Confirmation
import com.ultron.ai.core.mail.MailCommands
import com.ultron.ai.core.mail.MailSpeech
import com.ultron.ai.core.mail.MimeMessage
import com.ultron.ai.data.local.dao.UserPreferenceDao
import com.ultron.ai.data.remote.GmailApi
import java.io.IOException

/**
 * Everything Friday does with mail, behind one spoken interface.
 *
 * Nothing is sent without an explicit "text". A spoken mail passes through
 * speech recognition before it reaches here; reading it back and waiting is
 * the only point at which a misheard name or word can still be caught.
 */
class MailAssistant(
    private val auth: GmailAuth,
    private val api: GmailApi,
    private val contacts: ContactsReader,
    private val retell: MailRetelling,
    private val prefDao: UserPreferenceDao,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val people = MailPeople(api)

    /** What to say, and whether the next thing the user says answers it. */
    data class Answer(val text: String, val awaitsConfirmation: Boolean = false)

    companion object {
        const val PREF_ACCOUNT = "gmail_account"
        private const val TAG = "MailAssistant"

        /** A confirmation older than this is stale; "text" no longer sends anything. */
        private const val PENDING_TTL_MS = 2 * 60_000L

        /** Longer mail is retold rather than read: two minutes of speech helps nobody. */
        private const val SUMMARISE_OVER = 350
        private const val READ_ALOUD_MAX = 600
    }

    private class NoAccess(message: String) : Exception(message)

    private class Pending(val draft: MimeMessage.Draft, val threadId: String?, val createdAt: Long)

    @Volatile
    private var pending: Pending? = null

    suspend fun handle(request: MailCommands.Request, russian: Boolean): Answer {
        if (prefDao.get(PREF_ACCOUNT).isNullOrBlank()) {
            return Answer(
                "Mail isn't connected. Connect Gmail in Friday's settings."
            )
        }
        return try {
            when (request) {
                MailCommands.Request.CheckUnread -> Answer(withToken(russian) { checkUnread(it, russian) })
                is MailCommands.Request.Read -> Answer(withToken(russian) { read(it, request.from, russian) })
                is MailCommands.Request.Reply -> withToken(russian) { prepareReply(it, request, russian) }
                is MailCommands.Request.Compose -> withToken(russian) { prepareNew(it, request, russian) }
            }
        } catch (e: NoAccess) {
            Answer(e.message.orEmpty())
        } catch (e: IOException) {
            Log.w(TAG, "Gmail request failed: ${e.message}")
            Answer("Couldn't reach Gmail.")
        }
    }

    /**
     * Answers a pending "text?" if [text] is a yes or a no.
     *
     * @return what to say, or null when nothing was pending or [text] is a
     *   new request. In that case the draft is dropped, so a "text" said later
     *   in some other context can never send it.
     */
    suspend fun answerPending(text: String, russian: Boolean): String? {
        val p = pending ?: return null
        pending = null
        if (clock() - p.createdAt > PENDING_TTL_MS) return null
        return when (Confirmation.classify(text)) {
            Confirmation.Answer.YES -> try {
                withToken(russian) { api.send(it, MimeMessage.raw(p.draft), p.threadId) }
                "Sent."
            } catch (e: NoAccess) {
                e.message
            } catch (e: IOException) {
                Log.w(TAG, "Send failed: ${e.message}")
                "It didn't go — Gmail didn't answer."
            }
            Confirmation.Answer.NO -> "Cancelled."
            Confirmation.Answer.OTHER -> {
                Log.i(TAG, "Draft dropped: the reply was a new request")
                null
            }
        }
    }

    // --- the four requests ---------------------------------------------------

    private suspend fun checkUnread(token: String, russian: Boolean): String {
        val listing = api.list(token, "is:unread in:inbox", MailSpeech.NAMED)
        return MailSpeech.unread(listing.estimate, people.metadataOf(token, listing.ids), russian)
    }

    private suspend fun read(token: String, from: String?, russian: Boolean): String {
        val target = if (from == null) {
            api.list(token, "in:inbox", 1).ids.firstOrNull()?.let { api.metadata(token, it) }
        } else {
            people.findSender(token, from)
        }
        target ?: return when {
            from == null && russian -> "Во входящих пусто."
            from == null -> "Your inbox is empty."
            russian -> "Не нашла писем от «$from» среди последних."
            else -> "No recent mail from “$from”."
        }

        val mail = api.full(token, target.id)
        val text = (mail.body ?: mail.snippet).trim()
        val spoken = when {
            text.isEmpty() -> ""
            text.length <= SUMMARISE_OVER -> text
            else -> retell.summarise(text, russian) ?: (text.take(READ_ALOUD_MAX) + "…")
        }
        // Read means read: it should stop showing as unread in Gmail too.
        runCatching { api.markRead(token, mail.id) }
        return "${MailSpeech.header(mail, russian)} $spoken".trim()
    }

    private suspend fun prepareReply(token: String, r: MailCommands.Request.Reply, russian: Boolean): Answer {
        val target = people.findSender(token, r.to) ?: return Answer(
            "No recent mail from “${r.to}” to reply to."
        )
        val references = listOfNotNull(target.references, target.messageId).joinToString(" ").ifBlank { null }
        val draft = MimeMessage.Draft(
            to = target.fromAddress,
            subject = MimeMessage.replySubject(target.subject),
            body = MimeMessage.tidyBody(r.body),
            inReplyTo = target.messageId,
            references = references
        )
        pending = Pending(draft, target.threadId, clock())
        val subject = target.subject.ifBlank { "no subject" }
        return Answer(MailSpeech.confirmReply(target.fromName, subject, draft.body, russian), awaitsConfirmation = true)
    }

    private suspend fun prepareNew(token: String, r: MailCommands.Request.Compose, russian: Boolean): Answer {
        val person = ContactMatcher.findBest(r.to, contacts.loadEmailContacts())
            ?: people.findCorrespondent(token, r.to)
            ?: return Answer(
                "I couldn't find an address for “${r.to}”. Add one to the contact and I will."
            )
        val body = MimeMessage.tidyBody(r.body)
        val draft = MimeMessage.Draft(to = person.number, subject = MimeMessage.subjectFrom(body), body = body)
        pending = Pending(draft, threadId = null, createdAt = clock())
        return Answer(
            MailSpeech.confirmNew(person.name, person.number, draft.subject, body, russian),
            awaitsConfirmation = true
        )
    }

    // --- plumbing ------------------------------------------------------------

    /**
     * Runs [block] with a valid token. A 401 means the cached token went stale
     * mid-session: it is dropped and the call made once more with a fresh one.
     */
    private suspend fun <T> withToken(russian: Boolean, block: suspend (String) -> T): T {
        val token = token(russian)
        return try {
            block(token)
        } catch (e: GmailApi.HttpError) {
            if (e.code != HTTP_UNAUTHORIZED) throw e
            auth.invalidate(token)
            block(token(russian))
        }
    }

    private suspend fun token(russian: Boolean): String = when (val r = auth.authorize()) {
        is GmailAuth.Result.Token -> r.value
        is GmailAuth.Result.NeedsConsent -> throw NoAccess(
            if (russian) {
                "Gmail просит заново подтвердить доступ. " +
                    "Откройте настройки Friday и нажмите «Подключить Gmail»."
            }
            else "Gmail wants access confirmed again. Open Friday's settings and tap “Connect Gmail”."
        )
        is GmailAuth.Result.Failed -> throw NoAccess(r.reason)
    }
}

private const val HTTP_UNAUTHORIZED = 401
