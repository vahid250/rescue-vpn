package com.rescuevpn.app.core.vpn

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import com.rescuevpn.app.RescueApp
import com.rescuevpn.app.data.model.WireGuardValidator
import com.rescuevpn.app.ui.MainActivity
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import kotlinx.coroutines.*
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class RescueWireGuardService : Service() {

    companion object {
        const val ACTION_START = "com.rescuevpn.app.ACTION_START"
        const val ACTION_STOP = "com.rescuevpn.app.ACTION_STOP"
        const val EXTRA_CONFIG_RAW = "extra_config_raw"
        const val EXTRA_CONFIG_NAME = "extra_config_name"
    }

    private var backend: Backend? = null
    private var currentTunnel: Tunnel? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var timerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        try {
            backend = GoBackend(applicationContext)
        } catch (e: Exception) {
            TunnelStateManager.setLastError("خطا در بارگذاری موتور GoBackend: ${e.localizedMessage}")
            TunnelStateManager.setState(VpnConnectionState.FAILED)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val rawConfig = intent.getStringExtra(EXTRA_CONFIG_RAW)
                val configName = intent.getStringExtra(EXTRA_CONFIG_NAME) ?: "WireGuard"
                if (rawConfig.isNullOrBlank()) {
                    TunnelStateManager.setState(VpnConnectionState.NO_CONFIG)
                    TunnelStateManager.setLastError("کانفیگ معتبری برای برقراری تونل ارائه نشده است.")
                    stopSelf()
                } else {
                    startTunnel(configName, rawConfig)
                }
            }
            ACTION_STOP -> {
                stopTunnel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTunnel(name: String, rawConfig: String) {
        TunnelStateManager.setState(VpnConnectionState.CONNECTING)
        TunnelStateManager.setLastError(null)
        TunnelStateManager.setActiveConfigName(name)
        startForeground(1001, buildNotification("در حال برقراری هندشیک WireGuard..."))

        try {
            val normalizedConfig = WireGuardValidator.normalizeForWireGuard(rawConfig)
            val stream = ByteArrayInputStream(normalizedConfig.toByteArray(StandardCharsets.UTF_8))
            val parsedConfig = Config.parse(stream)

            val tunnel = object : Tunnel {
                override fun getName(): String = name
                override fun onStateChange(newState: Tunnel.State) {
                    when (newState) {
                        Tunnel.State.UP -> {
                            // وضعیت CONNECTED تنها پس از بالا آمدن واقعی اینترفیس در هسته لینوکس
                            TunnelStateManager.setState(VpnConnectionState.CONNECTED)
                            updateNotification("متصل به: $name (تونل رمزنگاری‌شده فعال)")
                            startTimer()
                        }
                        Tunnel.State.DOWN -> {
                            TunnelStateManager.setState(VpnConnectionState.DISCONNECTED)
                            stopTimer()
                        }
                        Tunnel.State.TOGGLE -> Unit
                    }
                }
            }
            currentTunnel = tunnel
            backend?.setState(tunnel, Tunnel.State.UP, parsedConfig)
        } catch (e: Exception) {
            TunnelStateManager.setState(VpnConnectionState.FAILED)
            TunnelStateManager.setLastError("خطا در ایجاد تونل: ${e.localizedMessage}")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun stopTunnel() {
        stopTimer()
        try {
            currentTunnel?.let {
                backend?.setState(it, Tunnel.State.DOWN, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        currentTunnel = null
        TunnelStateManager.setState(VpnConnectionState.DISCONNECTED)
        TunnelStateManager.setActiveConfigName(null)
        TunnelStateManager.setDuration(0L)
    }

    private fun startTimer() {
        timerJob?.cancel()
        TunnelStateManager.setDuration(0L)
        timerJob = serviceScope.launch {
            var seconds = 0L
            while (isActive && TunnelStateManager.state.value == VpnConnectionState.CONNECTED) {
                delay(1000)
                seconds++
                TunnelStateManager.setDuration(seconds)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return androidx.core.app.NotificationCompat.Builder(this, RescueApp.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("R&MA")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as? android.app.NotificationManager
        manager?.notify(1001, buildNotification(text))
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        stopTunnel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
