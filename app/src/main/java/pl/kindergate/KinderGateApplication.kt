package pl.kindergate

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KinderGateApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)

            // Persistent monitoring service notification
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_MONITOR,
                    getString(R.string.notification_channel_monitor),
                    NotificationManager.IMPORTANCE_LOW // low = no sound, stays visible
                ).apply {
                    description = getString(R.string.notification_channel_monitor_desc)
                    setShowBadge(false)
                }
            )

            // High-priority alert channel for tamper events
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ALERT,
                    getString(R.string.notification_channel_alert),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = getString(R.string.notification_channel_alert_desc)
                }
            )
        }
    }

    companion object {
        const val CHANNEL_MONITOR = "kg_monitor"
        const val CHANNEL_ALERT = "kg_alert"
    }
}
