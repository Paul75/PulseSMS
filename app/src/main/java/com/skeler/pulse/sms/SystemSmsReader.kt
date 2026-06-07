package com.skeler.pulse.sms

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import com.skeler.pulse.R
import com.skeler.pulse.contact.normalizeAddressForDisplay
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Reads real SMS messages from the Android system content provider (`content://sms`).
 *
 * This replaces the fake in-memory data with actual phone messages.
 * Requires [android.permission.READ_SMS] permission.
 */
class SystemSmsReader(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    private val contentResolver: ContentResolver get() = context.contentResolver
    private val smsSender = SystemSmsSender(context = context, ioDispatcher = ioDispatcher)

    /**
     * Observes all conversation threads as a reactive [Flow].
     * Emits a new list whenever the SMS content provider changes.
     */
    fun observeThreads(): Flow<List<SmsThread>> = callbackFlow {
        var readJob: Job? = null
        fun scheduleRead() {
            readJob?.cancel()
            readJob = launch(ioDispatcher) {
                try {
                    trySend(readThreads())
                } catch (e: SecurityException) {
                    Log.w("SystemSmsReader", "READ_SMS permission not granted", e)
                    trySend(emptyList())
                }
            }
        }

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                scheduleRead()
            }
        }
        contentResolver.registerContentObserver(
            Telephony.Sms.CONTENT_URI, true, observer
        )
        contentResolver.registerContentObserver(
            Telephony.Mms.CONTENT_URI, true, observer
        )
        // Initial emission
        scheduleRead()
        awaitClose {
            readJob?.cancel()
            contentResolver.unregisterContentObserver(observer)
        }
    }.conflate().distinctUntilChanged()

    /**
     * Observes messages for a specific address/thread as a reactive [Flow].
     */
    fun observeMessages(address: String, threadId: Long? = null): Flow<List<SystemSms>> = callbackFlow {
        var readJob: Job? = null
        fun scheduleRead() {
            readJob?.cancel()
            readJob = launch(ioDispatcher) {
                try {
                    val smsMessages = readMessages(address = address, threadId = threadId)
                    val mmsMessages = readMmsMessages(threadId = threadId, address = address)
                    trySend(mergeMessages(smsMessages, mmsMessages))
                } catch (e: SecurityException) {
                    Log.w("SystemSmsReader", "READ_SMS permission not granted", e)
                    trySend(emptyList())
                }
            }
        }

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                scheduleRead()
            }
        }
        contentResolver.registerContentObserver(
            Telephony.Sms.CONTENT_URI, true, observer
        )
        contentResolver.registerContentObserver(
            Telephony.Mms.CONTENT_URI, true, observer
        )
        scheduleRead()
        awaitClose {
            readJob?.cancel()
            contentResolver.unregisterContentObserver(observer)
        }
    }.conflate().distinctUntilChanged()

    /**
     * Reads all conversation threads from the SMS provider,
     * grouped by sender address and sorted by most recent first.
     */
    fun readThreads(): List<SmsThread> {
        val threads = linkedMapOf<Long, MutableThreadAccumulator>()

        // Read SMS threads
        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            SMS_PROJECTION,
            null, null,
            "${Telephony.Sms.DATE} DESC",
        )
        cursor?.use {
            val columns = SmsCursorColumns(it)
            while (it.moveToNext()) {
                val address = it.getString(columns.address) ?: "Unknown"
                val resolvedThreadId = it.getLong(columns.threadId).let { providerThreadId ->
                    if (providerThreadId > 0L) providerThreadId else syntheticThreadId(address)
                }
                val accumulator = threads.getOrPut(resolvedThreadId) {
                    MutableThreadAccumulator(
                        threadId = resolvedThreadId,
                        address = address.normalizeAddressForDisplay(),
                        snippet = (it.getString(columns.body) ?: "").take(120),
                        date = it.getLong(columns.date),
                    )
                }
                accumulator.messageCount += 1
                val isUnread = it.getInt(columns.read) != 1
                val isInbound = it.getInt(columns.type) == Telephony.Sms.MESSAGE_TYPE_INBOX
                if (isUnread && isInbound) {
                    accumulator.unreadCount += 1
                }
            }
        }

        // Read MMS threads and merge
        try {
            val mmsCursor = contentResolver.query(
                Telephony.Mms.CONTENT_URI,
                arrayOf("_id", "date", "read", "msg_box", "thread_id"),
                "msg_box = ?",
                arrayOf(Telephony.Mms.MESSAGE_BOX_INBOX.toString()),
                "date DESC",
            )
            mmsCursor?.use { c ->
                val idIdx = c.getColumnIndexOrThrow("_id")
                val dateIdx = c.getColumnIndexOrThrow("date")
                val readIdx = c.getColumnIndexOrThrow("read")
                val msgBoxIdx = c.getColumnIndexOrThrow("msg_box")
                val threadIdIdx = c.getColumnIndexOrThrow("thread_id")
                while (c.moveToNext()) {
                    val providerThreadId = c.getLong(threadIdIdx)
                    val mmsId = c.getLong(idIdx)
                    val dateSecs = c.getLong(dateIdx)
                    val isRead = c.getInt(readIdx) == 1
                    val dateMs = dateSecs * 1000L
                    if (providerThreadId <= 0L) continue
                    val accumulator = threads.getOrPut(providerThreadId) {
                        val addr = try {
                            MmsAddressResolver.resolveAddress(context, mmsId)
                                .ifEmpty { "Unknown" }
                        } catch (e: SecurityException) {
                            "Unknown"
                        }
                        MutableThreadAccumulator(
                            threadId = providerThreadId,
                            address = addr.normalizeAddressForDisplay(),
                            snippet = context.getString(R.string.mms_body_placeholder),
                            date = dateMs,
                        )
                    }
                    accumulator.messageCount += 1
                    val isInbound = c.getInt(msgBoxIdx) == Telephony.Mms.MESSAGE_BOX_INBOX
                    if (!isRead && isInbound) accumulator.unreadCount += 1
                    if (dateMs > accumulator.date) {
                        accumulator.date = dateMs
                        accumulator.snippet = context.getString(R.string.mms_body_placeholder)
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot read MMS for thread listing", e)
        }

        val merged = threads.values
            .groupBy { it.address }
            .values
            .map { group ->
                val latest = group.maxBy { it.date }
                SmsThread(
                    threadId = group.firstOrNull { it.threadId > 0L }?.threadId
                        ?: group.first().threadId,
                    address = latest.address,
                    snippet = latest.snippet,
                    date = latest.date,
                    messageCount = group.sumOf { it.messageCount },
                    unreadCount = group.sumOf { it.unreadCount },
                )
            }
            .sortedByDescending { it.date }

        return merged
    }

    /**
     * Reads all messages for a specific address, sorted oldest first.
     */
    fun readMessages(address: String, threadId: Long? = null): List<SystemSms> {
        val normalized = address.normalizeAddressForDisplay()
        val criteria = messageReadCriteria(threadId = threadId, address = address)
        val exactMessages = readMessages(criteria = criteria, normalizedAddress = normalized)
        if (exactMessages.isNotEmpty() || threadId.isProviderThreadId()) return exactMessages

        val fullScanCriteria = SmsProviderCriteria(
            selection = null,
            selectionArgs = emptyArray(),
            shouldFilterByAddress = true,
        )
        return readMessages(criteria = fullScanCriteria, normalizedAddress = normalized)
    }

    private fun readMessages(criteria: SmsProviderCriteria, normalizedAddress: String): List<SystemSms> {
        val messages = mutableListOf<SystemSms>()

        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            SMS_PROJECTION,
            criteria.selection,
            criteria.selectionArgs.ifEmpty { null },
            "${Telephony.Sms.DATE} ASC",
        ) ?: return emptyList()

        cursor.use {
            val columns = SmsCursorColumns(it)
            while (it.moveToNext()) {
                val sms = it.toSystemSms(columns)
                if (!criteria.shouldFilterByAddress || sms.address.normalizeAddressForDisplay() == normalizedAddress) {
                    messages.add(sms)
                }
            }
        }
        return messages
    }

    private fun readMmsMessages(threadId: Long?, address: String): List<SystemSms> {
        val resolvedThreadId = threadId ?: runCatching {
            Telephony.Threads.getOrCreateThreadId(context, address).takeIf { it > 0L }
        }.getOrNull()
        if (resolvedThreadId == null || resolvedThreadId <= 0L) return emptyList()

        val cursor = try {
            contentResolver.query(
                Telephony.Mms.CONTENT_URI,
                arrayOf("_id", "date", "read", "msg_box", "thread_id"),
                "thread_id = ? AND msg_box = ?",
                arrayOf(resolvedThreadId.toString(), Telephony.Mms.MESSAGE_BOX_INBOX.toString()),
                "date ASC",
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "MMS read requires READ_MMS permission", e)
            return emptyList()
        } ?: return emptyList()

        return cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow("_id")
            val dateIdx = c.getColumnIndexOrThrow("date")
            val readIdx = c.getColumnIndexOrThrow("read")
            val msgBoxIdx = c.getColumnIndexOrThrow("msg_box")

            val messages = mutableListOf<SystemSms>()
            while (c.moveToNext()) {
                val mmsId = c.getLong(idIdx)
                val dateSecs = c.getLong(dateIdx)
                val readInt = c.getInt(readIdx)
                val msgBox = c.getInt(msgBoxIdx)

                val fromAddress = try {
                    MmsAddressResolver.resolveAddress(context, mmsId).ifEmpty { address }
                } catch (e: SecurityException) {
                    address
                }
                val body = try {
                    MmsPartResolver.resolveTextBody(context, mmsId).orEmpty()
                } catch (e: SecurityException) {
                    ""
                }
                val partUri = try {
                    val uri = MmsPartResolver.resolveFirstAttachmentUri(context, mmsId)
                    if (uri != null) Log.i(TAG, "MMS part URI: $uri for mmsId=$mmsId")
                    else Log.w(TAG, "No attachment part found for mmsId=$mmsId")
                    uri
                } catch (e: SecurityException) {
                    Log.w(TAG, "SecurityException reading MMS parts for mmsId=$mmsId", e)
                    null
                }

                val smsType = when (msgBox) {
                    Telephony.Mms.MESSAGE_BOX_INBOX -> Telephony.Sms.MESSAGE_TYPE_INBOX
                    else -> Telephony.Sms.MESSAGE_TYPE_SENT
                }

                messages.add(
                    SystemSms(
                        id = mmsId,
                        isMms = true,
                        address = fromAddress,
                        body = body.ifEmpty { context.getString(R.string.mms_body_placeholder) },
                        date = dateSecs * 1000L,
                        type = smsType,
                        read = readInt == 1,
                        threadId = resolvedThreadId,
                        mmsPartUri = partUri,
                    ),
                )
            }
            messages
        }
    }

    private fun mergeMessages(sms: List<SystemSms>, mms: List<SystemSms>): List<SystemSms> {
        if (mms.isEmpty()) return sms
        return (sms + mms).sortedBy { it.date }
    }

    fun markThreadAsRead(threadId: Long?, address: String) {
        setThreadUnreadState(threadId = threadId, address = address, unread = false)
    }

    fun setThreadUnreadState(threadId: Long?, address: String, unread: Boolean) {
        val targetReadValue = if (unread) 1 else 0
        val targetIds = resolveSmsIds(
            threadId = threadId,
            address = address,
            inboundOnly = true,
            readValue = targetReadValue,
        )
        if (targetIds.isNotEmpty()) {
            val (selection, selectionArgs) = targetIds.toIdSelection(Telephony.Sms._ID)
            val values = ContentValues().apply {
                put(Telephony.Sms.READ, if (unread) 0 else 1)
                put(Telephony.Sms.SEEN, if (unread) 0 else 1)
            }
            contentResolver.update(Telephony.Sms.CONTENT_URI, values, selection, selectionArgs)
        }
        if (threadId != null && threadId > 0L) {
            val mmsValues = ContentValues().apply {
                put("read", if (unread) 0 else 1)
            }
            contentResolver.update(
                Telephony.Mms.CONTENT_URI,
                mmsValues,
                "thread_id = ? AND read = ?",
                arrayOf(threadId.toString(), targetReadValue.toString()),
            )
        }
    }

    fun deleteThread(threadId: Long?, address: String) {
        // Delete MMS first (before SMS observer fires) to avoid race where readThreads
        // sees MMS data after SMS is already gone
        if (threadId != null && threadId > 0L) {
            val mmsCursor = contentResolver.query(
                Telephony.Mms.CONTENT_URI,
                arrayOf("_id"),
                "thread_id = ?",
                arrayOf(threadId.toString()),
                null,
            )
            mmsCursor?.use { c ->
                val idIdx = c.getColumnIndexOrThrow("_id")
                while (c.moveToNext()) {
                    val mmsId = c.getLong(idIdx)
                    contentResolver.delete(
                        Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, mmsId.toString()),
                        null, null,
                    )
                    deleteMmsPartCacheFile(mmsId)
                }
            }
        }
        val targetIds = resolveSmsIds(threadId = threadId, address = address)
        if (targetIds.isNotEmpty()) {
            val (selection, selectionArgs) = targetIds.toIdSelection(Telephony.Sms._ID)
            contentResolver.delete(Telephony.Sms.CONTENT_URI, selection, selectionArgs)
        }
        // Force re-read by notifying both URIs (some ROMs don't notify on MMS delete)
        contentResolver.notifyChange(Telephony.Mms.CONTENT_URI, null)
    }

    private fun deleteMmsPartCacheFile(mmsId: Long) {
        val file = java.io.File(context.cacheDir, "mms_parts/$mmsId")
        if (file.exists()) file.delete()
    }

    fun deleteMessage(messageId: Long) {
        contentResolver.delete(
            Telephony.Sms.CONTENT_URI,
            "${Telephony.Sms._ID} = ?",
            arrayOf(messageId.toString()),
        )
    }

    fun deleteMessages(messages: List<SystemSms>) {
        if (messages.isEmpty()) return
        val smsIds = messages.filter { !it.isMms }.map { it.id }
        val mmsIds = messages.filter { it.isMms }.map { it.id }
        if (smsIds.isNotEmpty()) {
            val (selection, selectionArgs) = smsIds.toIdSelection(Telephony.Sms._ID)
            contentResolver.delete(Telephony.Sms.CONTENT_URI, selection, selectionArgs)
        }
        if (mmsIds.isNotEmpty()) {
            for (mmsId in mmsIds) {
                contentResolver.delete(
                    Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, mmsId.toString()),
                    null, null,
                )
                deleteMmsPartCacheFile(mmsId)
            }
            contentResolver.notifyChange(Telephony.Mms.CONTENT_URI, null)
        }
    }

    private fun resolveSmsIds(
        threadId: Long?,
        address: String,
        inboundOnly: Boolean = false,
        readValue: Int? = null,
    ): List<Long> {
        val normalizedAddress = address.normalizeAddressForDisplay()
        val criteria = smsIdCriteria(
            threadId = threadId,
            address = address,
            inboundOnly = inboundOnly,
            readValue = readValue,
        )
        val exactIds = resolveSmsIds(criteria = criteria, normalizedAddress = normalizedAddress)
        if (exactIds.isNotEmpty() || threadId.isProviderThreadId()) return exactIds

        val fullScanCriteria = SmsProviderCriteria(
            selection = buildList {
                if (inboundOnly) add("${Telephony.Sms.TYPE} = ?")
                if (readValue != null) add("${Telephony.Sms.READ} = ?")
            }.joinToString(separator = " AND ").ifBlank { null },
            selectionArgs = buildList {
                if (inboundOnly) add(Telephony.Sms.MESSAGE_TYPE_INBOX.toString())
                if (readValue != null) add(readValue.toString())
            }.toTypedArray(),
            shouldFilterByAddress = true,
        )
        return resolveSmsIds(criteria = fullScanCriteria, normalizedAddress = normalizedAddress)
    }

    private fun resolveSmsIds(criteria: SmsProviderCriteria, normalizedAddress: String): List<Long> {
        val ids = mutableListOf<Long>()
        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS),
            criteria.selection,
            criteria.selectionArgs.ifEmpty { null },
            null,
        ) ?: return emptyList()
        cursor.use {
            val idColumn = it.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addressColumn = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            while (it.moveToNext()) {
                val rawAddress = it.getString(addressColumn).orEmpty()
                if (!criteria.shouldFilterByAddress || rawAddress.normalizeAddressForDisplay() == normalizedAddress) {
                    ids.add(it.getLong(idColumn))
                }
            }
        }
        return ids
    }

    private fun List<Long>.toIdSelection(idColumn: String): Pair<String, Array<String>> =
        "$idColumn IN (${joinToString { "?" }})" to map(Long::toString).toTypedArray()

    suspend fun sendSms(address: String, body: String, subscriptionId: Int? = null) =
        smsSender.sendSms(address = address, body = body, subscriptionId = subscriptionId)

    companion object {
        private const val TAG = "SystemSmsReader"
        private val SMS_PROJECTION = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.READ,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.STATUS,
        )
    }
}
