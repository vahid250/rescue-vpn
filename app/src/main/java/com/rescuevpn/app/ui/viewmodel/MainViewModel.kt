package com.rescuevpn.app.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import com.rescuevpn.app.core.vpn.RescueWireGuardService
import com.rescuevpn.app.core.vpn.TunnelStateManager
import com.rescuevpn.app.core.vpn.VpnConnectionState
import com.rescuevpn.app.data.model.WireGuardProfile
import com.rescuevpn.app.data.model.WireGuardValidator
import com.rescuevpn.app.data.security.SecureConfigStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val configStore = SecureConfigStore(application)

    val connectionState: StateFlow<VpnConnectionState> = TunnelStateManager.state
    val activeConfigName: StateFlow<String?> = TunnelStateManager.activeConfigName
    val lastError: StateFlow<String?> = TunnelStateManager.lastError
    val durationSeconds: StateFlow<Long> = TunnelStateManager.connectedDurationSeconds
    val receivedBytes: StateFlow<Long> = TunnelStateManager.receivedBytes
    val sentBytes: StateFlow<Long> = TunnelStateManager.sentBytes
    val lastHandshakeMillis: StateFlow<Long> = TunnelStateManager.lastHandshakeMillis

    private val _profiles = MutableStateFlow<List<WireGuardProfile>>(emptyList())
    val profiles: StateFlow<List<WireGuardProfile>> = _profiles

    private val _selectedProfileId = MutableStateFlow<String?>(null)
    val selectedProfileId: StateFlow<String?> = _selectedProfileId

    init {
        loadProfiles()
    }

    fun loadProfiles() {
        val ids = configStore.getAllConfigIds()
        val list = mutableListOf<WireGuardProfile>()
        for (id in ids) {
            val data = configStore.getConfig(id)
            if (data != null) {
                try {
                    val parsed = WireGuardValidator.validateAndParse(id, data.first, data.second)
                    list.add(parsed)
                } catch (_: Exception) {}
            }
        }
        _profiles.value = list
        val savedActiveId = configStore.getActiveConfigId()
        _selectedProfileId.value = if (savedActiveId != null && list.any { it.id == savedActiveId }) {
            savedActiveId
        } else {
            list.firstOrNull()?.id
        }
    }

    fun selectProfile(id: String) {
        _selectedProfileId.value = id
        configStore.saveActiveConfigId(id)
    }

    fun addProfile(name: String, rawContent: String): Pair<Boolean, String?> {
        val newId = UUID.randomUUID().toString().take(8)
        return try {
            val parsed = WireGuardValidator.validateAndParse(newId, name, rawContent)
            configStore.saveConfig(parsed.id, parsed.name, parsed.rawText)
            loadProfiles()
            selectProfile(parsed.id)
            Pair(true, null)
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "فرمت کانفیگ WireGuard نامعتبر است.")
        }
    }

    fun deleteProfile(id: String) {
        configStore.deleteConfig(id)
        if (_selectedProfileId.value == id) {
            configStore.saveActiveConfigId(null)
        }
        loadProfiles()
    }

    fun connect() {
        val profileId = _selectedProfileId.value
        if (profileId == null) {
            TunnelStateManager.setState(VpnConnectionState.NO_CONFIG)
            TunnelStateManager.setLastError("هیچ کانفیگی انتخاب نشده است. لطفاً ابتدا کانفیگ وایرگارد وارد کنید.")
            return
        }
        val data = configStore.getConfig(profileId)
        if (data == null) {
            TunnelStateManager.setState(VpnConnectionState.NO_CONFIG)
            return
        }

        val intent = Intent(getApplication(), RescueWireGuardService::class.java).apply {
            action = RescueWireGuardService.ACTION_START
            putExtra(RescueWireGuardService.EXTRA_CONFIG_NAME, data.first)
            putExtra(RescueWireGuardService.EXTRA_CONFIG_RAW, data.second)
        }
        getApplication<Application>().startService(intent)
    }

    fun disconnect() {
        val intent = Intent(getApplication(), RescueWireGuardService::class.java).apply {
            action = RescueWireGuardService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
    }

    fun wipeAllData() {
        disconnect()
        configStore.clearAll()
        loadProfiles()
    }
}
