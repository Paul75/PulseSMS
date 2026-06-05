package com.skeler.pulse.sms

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.skeler.pulse.R

/**
 * BroadcastReceiver for incoming MMS messages (WAP Push).
 *
 * Registered with `android.provider.Telephony.WAP_PUSH_DELIVER` intent filter
 * and `application/vnd.wap.mms-message` MIME type.
 *
 * This broadcast is only delivered when Pulse is the default SMS app.
 *
 * Current implementation acknowledges receipt to prevent message loss.
 * Full MMS download and rendering is deferred to Phase 2.
 *
 * Uses [goAsync] to move provider writes off the main thread and avoid ANR
 * when the content provider is under contention.
 */
class MmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION) return

        val pendingResult = goAsync()

        Thread {
            try {
                try {
                    val sender = context.getString(R.string.mms_sender_label)
                    val body = context.getString(R.string.mms_body_placeholder)
                    val persistedUri = writeMmsPlaceholderToProvider(context, sender, body)
                    if (persistedUri != null) {
                        val messageId = ContentUris.parseId(persistedUri)
                        SmsNotificationHelper.notifyIncomingSms(
                            context = context,
                            sender = sender,
                            body = body,
                            messageId = messageId,
                        )
                    } else {
                        SmsNotificationHelper.notifyIncomingSms(
                            context = context,
                            sender = sender,
                            body = context.getString(R.string.mms_body_save_failed),
                        )
                    }
                } catch (e: Exception) {
                    Log.e("MmsReceiver", "Unhandled exception processing incoming MMS", e)
                }
            } finally {
                pendingResult.finish()
            }
        }.start()
    }

    private fun writeMmsPlaceholderToProvider(
        context: Context,
        sender: String,
        body: String,
    ) = try {
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, sender)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, now)
            put(Telephony.Sms.DATE_SENT, now)
            put(Telephony.Sms.READ, 0)
            put(Telephony.Sms.SEEN, 0)
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
            put(Telephony.Sms.THREAD_ID, Telephony.Threads.getOrCreateThreadId(context, sender))
        }
        context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
    } catch (e: Exception) {
        Log.e("MmsReceiver", "Failed to write MMS placeholder to provider", e)
        null
    }
}
