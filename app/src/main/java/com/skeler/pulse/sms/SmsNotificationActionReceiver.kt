package com.skeler.pulse.sms

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsNotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1L)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        when (intent.action) {
            ACTION_MARK_READ -> handleMarkRead(context, messageId, notificationId)
            ACTION_DELETE -> handleDelete(context, messageId, notificationId)
            ACTION_REPLY -> handleReply(context, intent, notificationId)
        }
    }

    private fun handleMarkRead(context: Context, messageId: Long, notificationId: Int) {
        if (messageId < 0) return
        val values = ContentValues().apply {
            put(Telephony.Sms.READ, 1)
            put(Telephony.Sms.SEEN, 1)
        }
        context.contentResolver.update(
            Telephony.Sms.CONTENT_URI,
            values,
            "${Telephony.Sms._ID} = ?",
            arrayOf(messageId.toString()),
        )
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    private fun handleDelete(context: Context, messageId: Long, notificationId: Int) {
        if (messageId < 0) return
        context.contentResolver.delete(
            Telephony.Sms.CONTENT_URI,
            "${Telephony.Sms._ID} = ?",
            arrayOf(messageId.toString()),
        )
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    private fun handleReply(context: Context, intent: Intent, notificationId: Int) {
        val messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1L)
        val results = RemoteInput.getResultsFromIntent(intent)
        val replyText = results?.getCharSequence(KEY_REPLY_TEXT)?.toString() ?: return
        if (replyText.isBlank()) return

        val address = intent.getStringExtra(EXTRA_SENDER_ADDRESS) ?: return

        if (messageId > 0) {
            val readValues = ContentValues().apply {
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.SEEN, 1)
            }
            context.contentResolver.update(
                Telephony.Sms.CONTENT_URI, readValues,
                "${Telephony.Sms._ID} = ?",
                arrayOf(messageId.toString()),
            )
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SystemSmsSender(context, Dispatchers.IO).sendSmsFireAndForget(address, replyText)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send reply via SMS", e)
            } finally {
                pendingResult.finish()
            }
        }

        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    companion object {
        private const val TAG = "SmsNotificationAction"
        const val ACTION_MARK_READ = "com.skeler.pulse.action.MARK_READ"
        const val ACTION_DELETE = "com.skeler.pulse.action.DELETE"
        const val ACTION_REPLY = "com.skeler.pulse.action.REPLY"
        const val KEY_REPLY_TEXT = "reply_text"
        const val EXTRA_MESSAGE_ID = "extra_message_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_SENDER_ADDRESS = "extra_sender_address"
    }
}
