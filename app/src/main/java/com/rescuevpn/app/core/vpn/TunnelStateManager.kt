package com.rescuevpn.app.core.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class VpnConnectionState { DISCONNECTED, CONNECTING, CONNECTED, FAILED, NO_CONFIG }

object TunnelStateManager {
    private val _state = MutableStateFlow(VpnConnectionState.DISCONNECTED)
    val state: StateFlow<VpnConnectionState> = _state
    private val _activeConfigName = MutableStateFlow<String?>(null)
    val activeConfigName: StateFlow<String?> = _activeConfigName
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError
    private val _connectedDurationSeconds = MutableStateFlow(0L)
    val connectedDurationSeconds: StateFlow<Long> = _connectedDurationSeconds
    private val _receivedBytes = MutableStateFlow(0L)
    val receivedBytes: StateFlow<Long> = _receivedBytes
    private val _sentBytes = MutableStateFlow(0L)
    val sentBytes: StateFlow<Long> = _sentBytes
    private val _lastHandshakeMillis = MutableStateFlow(0L)
    val lastHandshakeMillis: StateFlow<Long> = _lastHandshakeMillis

    fun setState(newState: VpnConnectionState) { _state.value = newState }
    fun setActiveConfigName(name: String?) { _activeConfigName.value = name }
    fun setLastError(error: String?) { _lastError.value = error }
    fun setDuration(seconds: Long) { _connectedDurationSeconds.value = seconds }
    fun setTraffic(rx: Long, tx: Long, handshakeMillis: Long) {
        _receivedBytes.value = rx
        _sentBytes.value = tx
        _lastHandshakeMillis.value = handshakeMillis
    }
    fun resetTraffic() = setTraffic(0L, 0L, 0L)
}
