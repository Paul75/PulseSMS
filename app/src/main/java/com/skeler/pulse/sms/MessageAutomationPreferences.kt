package com.skeler.pulse.sms

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class MessageAutomationPreferences(
    private val context: Context,
) {
    private val store: DataStore<Preferences>
        get() = context.dataStore

    val autoCopyOtpCodes: Flow<Boolean> =
        store.data.map { prefs -> prefs[KEY_AUTO_COPY_OTP_CODES] ?: false }

    suspend fun isAutoCopyOtpCodesEnabled(): Boolean =
        autoCopyOtpCodes.first()

    suspend fun setAutoCopyOtpCodesEnabled(enabled: Boolean) {
        store.edit { prefs -> prefs[KEY_AUTO_COPY_OTP_CODES] = enabled }
    }

    companion object {
        private val Context.dataStore by preferencesDataStore(name = "message_automation_prefs")
        private val KEY_AUTO_COPY_OTP_CODES = booleanPreferencesKey("auto_copy_otp_codes")
    }
}
