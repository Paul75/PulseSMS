package com.skeler.pulse

import android.app.Application
import android.content.IntentFilter
import android.os.Build
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.skeler.pulse.sms.NotificationPreferences
import com.skeler.pulse.sms.QuickComposeNotificationManager
import com.skeler.pulse.sms.SmsNotificationHelper
import com.skeler.pulse.sms.SmsProcessingHelper
import com.skeler.pulse.sms.SmsReceiver
import com.skeler.pulse.sync.worker.SyncWorkerDependenciesHolder

class PulseApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
        SyncWorkerDependenciesHolder.dependencies = object : com.skeler.pulse.sync.worker.SyncWorkerDependencies {
            override fun messageSyncOrchestrator() = appContainer.syncComponent.messageSyncOrchestrator
        }
        SmsNotificationHelper.createNotificationChannel(this)
        QuickComposeNotificationManager.createChannel(this)

        kotlinx.coroutines.runBlocking {
            if (NotificationPreferences(this@PulseApplication).isQuickComposeEnabled()) {
                QuickComposeNotificationManager.show(this@PulseApplication)
            }
        }

        registerSmsFallbackReceiver()
    }

    private fun registerSmsFallbackReceiver() {
        val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
        val receiverFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.RECEIVER_EXPORTED
        } else {
            0
        }
        ContextCompat.registerReceiver(
            this,
            SmsReceiver(),
            filter,
            receiverFlags,
        )
    }
}
