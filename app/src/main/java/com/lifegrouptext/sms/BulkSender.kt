package com.lifegrouptext.sms

import com.lifegrouptext.data.SendLogRepository
import com.lifegrouptext.domain.Contact
import com.lifegrouptext.domain.SendLogEntry
import com.lifegrouptext.domain.SendStatus
import com.lifegrouptext.domain.personalize
import kotlinx.coroutines.delay

/** How far along a group send is. */
data class SendProgress(
    val completed: Int,
    val total: Int,
    val currentName: String,
)

/** What a whole group send came to. */
data class SendSummary(
    val sent: Int,
    val failed: Int,
) {
    val total: Int get() = sent + failed
}

/**
 * Sends one message to a list of people, one at a time, recording each outcome.
 *
 * Messages go out sequentially with a short pause between them: carriers throttle
 * bursts, and Android itself starts prompting the user when an app that is not the
 * default messaging app sends many texts in quick succession.
 */
class BulkSender(
    private val smsSender: SmsSender,
    private val sendLogRepository: SendLogRepository,
) {

    suspend fun send(
        recipients: List<Contact>,
        body: String,
        onProgress: (SendProgress) -> Unit = {},
    ): SendSummary {
        if (recipients.isEmpty() || body.isBlank()) return SendSummary(0, 0)

        val cleaned = SmsText.sanitize(body)
        val now = System.currentTimeMillis()
        val log = ArrayList<SendLogEntry>(recipients.size)
        var sent = 0
        var failed = 0

        recipients.forEachIndexed { index, contact ->
            onProgress(SendProgress(completed = index, total = recipients.size, currentName = contact.name))

            val personalized = personalize(cleaned, contact.firstName)
            val result = smsSender.send(contact.phone, personalized)

            when (result) {
                is SendResult.Success -> sent++
                is SendResult.Failure -> failed++
            }

            log += SendLogEntry(
                id = 0,
                contactName = contact.name,
                phone = contact.phone,
                body = personalized,
                sentAt = now,
                status = if (result is SendResult.Success) SendStatus.SENT else SendStatus.FAILED,
                segments = result.segments,
                failureReason = (result as? SendResult.Failure)?.reason,
            )

            if (index < recipients.lastIndex) delay(BETWEEN_MESSAGES_MS)
        }

        onProgress(SendProgress(recipients.size, recipients.size, ""))
        sendLogRepository.record(log)
        return SendSummary(sent = sent, failed = failed)
    }

    private companion object {
        const val BETWEEN_MESSAGES_MS = 1_500L
    }
}
