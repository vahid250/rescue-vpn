package com.rescuevpn.app.core.vpn

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import com.rescuevpn.app.RescueApp
import com.rescuevpn.app.data.model.WireGuardValidator
import com.rescuevpn.app.ui.MainActivity
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

/**
 * All backend state transitions are processed by one command consumer.
 * This prevents concurrent UP/DOWN calls from racing when users switch profiles
 * or disconnect while a start operation is still in progress.
 */
class RescueWireGuardService : Service() {

    companion object {
        const val ACTION_START = "com.rescuevpn.app.ACTION_START"
        const val ACTION_STOP = "com.rescuevpn.app.ACTION_STOP"
        const val EXTRA_CONFIG_RAW = "extra_config_raw"
        const val EXTRA_CONFIG_NAME = "extra_config_name"
        private const val NOTIFICATION_ID = 1001
    }

    private sealed interface Command {
        data class Start(val name: String, val rawConfig: String) : Command
        data class Stop(val startId: Int) : Command
    }

    private var backend: GoBackend? = null
    @Volatile private var currentTunnel: Tunnel? = null
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.Main.immediate)
    private val commands = Channel<Command>(Channel.UNLIMITED)
    private var commandJob: Job? = null
    private var timerJob: Job? = null
    private var statsJob: Job? = null
    private var activeConfigFingerprint: Int? = null

    override fun onCreate() {
        super.onCreate()
        try {
            backend = GoBackend(applicationContext)
        } catch (e: Exception) {
            TunnelStateManager.setLastError("خطا در بارگذاری موتور GoBackend: ${e.localizedMessage}")
            TunnelStateManager.setState(VpnConnectionState.FAILED)
        }
        // Start the consumer even if backend initialization failed, so commands
        // are acknowledged and reported instead of silently accumulating in the channel.
        commandJob = serviceScope.launch {
            for (firstCommand in commands) {
                // Collapse requests that arrived during the previous transition.
                // The newest user intent wins: a final Stop cancels queued starts,
                // while a later Start intentionally supersedes an earlier Stop.
                var latestCommand = firstCommand
                while (true) {
                    val queued = commands.tryReceive().getOrNull() ?: break
                    latestCommand = queued
                }
                when (val command = latestCommand) {
                    is Command.Start -> handleStart(command.name, command.rawConfig)
                    is Command.Stop -> handleStop(command.startId)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val raw = intent.getStringExtra(EXTRA_CONFIG_RAW)
                val name = intent.getStringExtra(EXTRA_CONFIG_NAME) ?: "WireGuard"
                if (raw.isNullOrBlank()) {
                    TunnelStateManager.setState(VpnConnectionState.NO_CONFIG)
                    TunnelStateManager.setLastError("کانفیگ معتبری برای برقراری تونل ارائه نشده است.")
                } else {
                    startForeground(NOTIFICATION_ID, buildNotification("در صف برقراری تونل WireGuard..."))
                    val result = commands.trySend(Command.Start(name, raw))
                    if (result.isFailure) {
                        TunnelStateManager.setState(VpnConnectionState.FAILED)
                        TunnelStateManager.setLastError("صف فرمان‌های اتصال در دسترس نیست.")
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    }
                }
            }
            ACTION_STOP -> {
                val result = commands.trySend(Command.Stop(startId))
                if (result.isFailure) {
                    TunnelStateManager.setLastError("فرمان قطع تونل ثبت نشد؛ لطفاً دوباره تلاش کنید.")
                }
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun handleStart(name: String, rawConfig: String) {
        TunnelStateManager.setState(VpnConnectionState.CONNECTING)
        TunnelStateManager.setLastError(null)
        TunnelStateManager.setActiveConfigName(name)
        updateNotification("در حال برقراری تونل WireGuard...")

        val activeBackend = backend
        if (activeBackend == null) {
            failStart(IllegalStateException("موتور WireGuard آماده نیست"))
            return
        }

        val fingerprint = rawConfig.hashCode()
        val previous = currentTunnel
        if (previous != null && activeConfigFingerprint == fingerprint) {
            try {
                val alreadyUp = withContext(Dispatchers.IO) {
                    activeBackend.getState(previous) == Tunnel.State.UP
                }
                if (alreadyUp) {
                    startStatsPolling(previous, activeBackend)
                    TunnelStateManager.setState(VpnConnectionState.CONNECTED)
                    updateNotification("اینترفیس WireGuard فعال است")
                    return
                }
            } catch (_: Exception) {
                // Continue with a clean re-apply when state cannot be queried.
            }
        }

        try {
            val normalized = WireGuardValidator.normalizeForWireGuard(rawConfig)
            val parsed = Config.parse(
                ByteArrayInputStream(normalized.toByteArray(StandardCharsets.UTF_8))
            )
            val tunnel = object : Tunnel {
                override fun getName(): String = name
                override fun onStateChange(newState: Tunnel.State) {
                    if (currentTunnel !== this) return
                    when (newState) {
                        Tunnel.State.UP -> {
                            TunnelStateManager.setState(VpnConnectionState.CONNECTED)
                            updateNotification("اینترفیس WireGuard فعال است؛ وضعیت handshake جداگانه بررسی می‌شود")
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

            // Publish the new identity before backend calls so callbacks from the
            // previous tunnel are ignored from this point onward.
            currentTunnel = tunnel
            stopStatsPolling()
            stopTimer()

            withContext(Dispatchers.IO) {
                if (previous != null && previous !== tunnel &&
                    activeBackend.getState(previous) == Tunnel.State.UP
                ) {
                    activeBackend.setState(previous, Tunnel.State.DOWN, null)
                }
                activeBackend.setState(tunnel, Tunnel.State.UP, parsed)
            }
            activeConfigFingerprint = fingerprint
            startStatsPolling(tunnel, activeBackend)
        } catch (e: Exception) {
            // Parsing can fail before the old tunnel is touched. If it is still UP,
            // preserve it and restore truthful UI state instead of orphaning a live tunnel.
            val previousStillUp = try {
                previous != null && withContext(Dispatchers.IO) {
                    activeBackend.getState(previous) == Tunnel.State.UP
                }
            } catch (_: Exception) {
                false
            }
            if (previousStillUp && previous != null) {
                currentTunnel = previous
                TunnelStateManager.setActiveConfigName(previous.getName())
                TunnelStateManager.setState(VpnConnectionState.CONNECTED)
                startTimer()
                startStatsPolling(previous, activeBackend)
                TunnelStateManager.setLastError(
                    "تعویض سرور انجام نشد؛ اتصال قبلی حفظ شد. " +
                        (e.localizedMessage ?: "خطای نامشخص")
                )
                updateNotification("اتصال قبلی فعال است؛ تعویض سرور ناموفق بود")
            } else {
                currentTunnel = null
                activeConfigFingerprint = null
                failStart(e)
            }
        }
    }

    private suspend fun failStart(error: Exception) {
        stopStatsPolling()
        stopTimer()
        TunnelStateManager.setState(VpnConnectionState.FAILED)
        val detail = error.localizedMessage?.takeIf { it.isNotBlank() } ?: "جزئیات بیشتری ارائه نشد"
        TunnelStateManager.setLastError(
            "خطا در ایجاد تونل (${error.javaClass.simpleName}): $detail"
        )
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private suspend fun handleStop(startId: Int) {
        stopTimer()
        stopStatsPolling()
        val tunnel = currentTunnel
        currentTunnel = null
        activeConfigFingerprint = null
        try {
            if (tunnel != null) {
                withContext(Dispatchers.IO) {
                    backend?.setState(tunnel, Tunnel.State.DOWN, null)
                }
            }
            TunnelStateManager.setState(VpnConnectionState.DISCONNECTED)
            TunnelStateManager.setActiveConfigName(null)
            TunnelStateManager.setDuration(0L)
            TunnelStateManager.resetTraffic()
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            TunnelStateManager.setState(VpnConnectionState.FAILED)
            TunnelStateManager.setLastError(
                "قطع تونل کامل نشد (${e.javaClass.simpleName}): " +
                    (e.localizedMessage ?: "خطای نامشخص")
            )
            stopForeground(STOP_FOREGROUND_REMOVE)
        } finally {
            // Do not destroy the service if a newer start request arrived after this stop.
            stopSelfResult(startId)
        }
    }

    private fun startStatsPolling(tunnel: Tunnel, activeBackend: GoBackend) {
        stopStatsPolling()
        TunnelStateManager.resetTraffic()
        statsJob = serviceScope.launch {
            while (isActive && currentTunnel === tunnel) {
                try {
                    val stats = withContext(Dispatchers.IO) { activeBackend.getStatistics(tunnel) }
                    val latestHandshake = stats.peers()
                        .mapNotNull { key -> stats.peer(key)?.latestHandshakeEpochMillis() }
                        .maxOrNull() ?: 0L
                    TunnelStateManager.setTraffic(stats.totalRx(), stats.totalTx(), latestHandshake)
                } catch (e: Exception) {
                    if (currentTunnel === tunnel) {
                        TunnelStateManager.setLastError(
                            "خواندن آمار تونل ناموفق بود (${e.javaClass.simpleName}): " +
                                (e.localizedMessage?.takeIf { it.isNotBlank() } ?: "خطای نامشخص")
                        )
                    }
                }
                delay(2000)
            }
        }
    }

    private fun stopStatsPolling() {
        statsJob?.cancel()
        statsJob = null
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
        manager?.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        // Never perform a blocking backend DOWN call on the main thread here.
        // Normal user disconnect is serialized through Command.Stop before stopSelfResult.
        commands.close()
        serviceScope.cancel()
        currentTunnel = null
        super.onDestroy()
    }
}
