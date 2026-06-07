package com.skeler.pulse.sms

import android.content.Context
import android.net.Uri

internal object MmsAddressResolver {

    fun resolveAddress(context: Context, mmsId: Long): String {
        val uri = Uri.withAppendedPath(Uri.parse("content://mms/$mmsId"), "addr")
        val cursor = context.contentResolver.query(
            uri,
            arrayOf("address", "type"),
            null,
            null,
            null,
        ) ?: return ""

        return cursor.use { c ->
            while (c.moveToNext()) {
                val type = c.getInt(c.getColumnIndexOrThrow("type"))
                if (type == 137) { // FROM
                    return@use c.getString(c.getColumnIndexOrThrow("address")) ?: ""
                }
            }
            ""
        }
    }
}
