package com.rescuevpn.app.core.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class VpnConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    FAILED,
    NO_CONFIG
}

object TunnelStateManager {
    private val _state = MutableStateFlow(VpnConnectionState.DISCONNECTED)
    val state: StateFlow<VpnConnectionState> = _state

    private val _activeConfigName = MutableStateFlow<String?>(null)
    val activeConfigName: StateFlow<String?> = _activeConfigName

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    private val _connectedDurationSeconds = MutableStateFlow(0L)
    val connectedDurationSeconds: StateFlow<Long> = _connectedDurationSeconds

    fun setState(newState: VpnConnectionState) {
        _state.value = newState
    }

    fun setActiveConfigName(name: String?) {
        _activeConfigName.value = name
    }

    fun setLastError(error: String?) {
        _lastError.value = error
    }

    fun setDuration(seconds: Long) {
        _connectedDurationSeconds.value = seconds
    }
}
