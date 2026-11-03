package com.rescuevpn.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class RescueApp : Application() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "rescue_vpn_wireguard_channel"
        lateinit var instance: RescueApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.vpn_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.vpn_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
