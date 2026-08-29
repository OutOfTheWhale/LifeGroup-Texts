package com.lifegrouptext.sms

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/** The outcome of one message to one recipient. */
sealed interface SendResult {
    val segments: Int

    data class Success(override val segments: Int) : SendResult
    data class Failure(val reason: String, override val segments: Int) : SendResult
}

/**
 * Sends one SMS, however long it is, and reports what actually happened.
 *
 * Two things the previous implementation got wrong:
 *  - it used `sendTextMessage`, which carries a single segment, so anything past one
 *    segment was rejected by the radio. [SmsManager.divideMessage] plus
 *    `sendMultipartTextMessage` is the API that handles long messages.
 *  - it passed null for the sent-status intents, so a failure was indistinguishable
 *    from a success. Every part now reports back through a broadcast, and this call
 *    does not return until they all have.
 */
class SmsSender(private val appContext: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun send(phone: String, body: String): SendResult {
        if (!hasPermission()) return SendResult.Failure("Permission to send texts was denied", 0)
        if (phone.isBlank()) return SendResult.Failure("No phone number", 0)
        if (body.isBlank()) return SendResult.Failure("Message is empty", 0)

        val manager = appContext.getSystemService(SmsManager::class.java)
            ?: return SendResult.Failure("This device cannot send texts", 0)

        val parts = try {
            manager.divideMessage(body)
        } catch (e: Exception) {
            return SendResult.Failure(e.message ?: "Could not split the message", 0)
        }
        if (parts.isNullOrEmpty()) return SendResult.Failure("Message is empty", 0)

        val action = "$SENT_ACTION.${counter.incrementAndGet()}"

        return withTimeoutOrNull(TIMEOUT_MS) {
            awaitSend(manager, phone, parts, action)
        } ?: SendResult.Failure("Timed out waiting for the network", parts.size)
    }

    private suspend fun awaitSend(
        manager: SmsManager,
        phone: String,
        parts: ArrayList<String>,
        action: String,
    ): SendResult = suspendCancellableCoroutine { cont ->
        val outstanding = AtomicInteger(parts.size)
        val firstFailure = AtomicReference<String?>(null)
        val finished = AtomicBoolean(false)

        // The receiver fires once per part; the send is only settled when the last
        // one reports. A single failed part fails the whole message, because a
        // half-delivered message is not something the recipient can read.
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (resultCode != Activity.RESULT_OK) {
                    firstFailure.compareAndSet(null, describe(resultCode))
                }
                if (outstanding.decrementAndGet() == 0) {
                    settle(cont, finished, this) {
                        val reason = firstFailure.get()
                        if (reason == null) {
                            SendResult.Success(parts.size)
                        } else {
                            SendResult.Failure(reason, parts.size)
                        }
                    }
                }
            }
        }

        ContextCompat.registerReceiver(
            appContext,
            receiver,
            IntentFilter(action),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        cont.invokeOnCancellation { runCatching { appContext.unregisterReceiver(receiver) } }

        val sentIntents = ArrayList<PendingIntent>(parts.size)
        for (index in parts.indices) {
            sentIntents += PendingIntent.getBroadcast(
                appContext,
                index,
                Intent(action).setPackage(appContext.packageName),
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        try {
            manager.sendMultipartTextMessage(phone, null, parts, sentIntents, null)
        } catch (e: Exception) {
            settle(cont, finished, receiver) {
                SendResult.Failure(describeException(e), parts.size)
            }
        }
    }

    /** Resume exactly once, unregistering the receiver on the way out. */
    private inline fun settle(
        cont: kotlinx.coroutines.CancellableContinuation<SendResult>,
        finished: AtomicBoolean,
        receiver: BroadcastReceiver,
        result: () -> SendResult,
    ) {
        if (!finished.compareAndSet(false, true)) return
        runCatching { appContext.unregisterReceiver(receiver) }
        if (cont.isActive) cont.resume(result())
    }

    /**
     * Turn a thrown telephony error into something a person can act on. The platform's
     * own messages name internal permissions and system services, which tells the user
     * nothing — the raw text is appended, trimmed, only as a hint for debugging.
     */
    private fun describeException(e: Exception): String {
        val plain = when {
            e is SecurityException -> "The phone blocked the message — check the SIM and permissions"
            e is IllegalArgumentException -> "That phone number was rejected"
            e.message?.contains("no service", ignoreCase = true) == true -> "No cell service"
            else -> "The phone could not send the message"
        }
        val detail = e.message
            ?.substringBefore('.')
            ?.trim()
            ?.takeIf { it.isNotEmpty() && it.length <= 60 }
        return if (detail == null) plain else "$plain ($detail)"
    }

    private fun describe(code: Int): String = when (code) {
        SmsManager.RESULT_ERROR_NO_SERVICE -> "No cell service"
        SmsManager.RESULT_ERROR_RADIO_OFF -> "Radio is off — check airplane mode"
        SmsManager.RESULT_ERROR_NULL_PDU -> "The carrier rejected the message"
        SmsManager.RESULT_ERROR_LIMIT_EXCEEDED -> "Too many messages sent too quickly"
        SmsManager.RESULT_ERROR_FDN_CHECK_FAILURE -> "Blocked by fixed dialling numbers"
        SmsManager.RESULT_ERROR_SHORT_CODE_NOT_ALLOWED,
        SmsManager.RESULT_ERROR_SHORT_CODE_NEVER_ALLOWED,
        -> "Short codes are not permitted"
        SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "The carrier refused the message"
        else -> "Send failed (code $code)"
    }

    private companion object {
        const val SENT_ACTION = "com.lifegrouptext.SMS_SENT"

        /**
         * A message normally settles in seconds. This only exists so one wedged send
         * cannot stall a whole group.
         */
        const val TIMEOUT_MS = 60_000L

        /** Keeps each send's broadcast action distinct from the last one's. */
        val counter = AtomicInteger(0)
    }
}
