package com.rescuevpn.app.ui

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.rescuevpn.app.core.vpn.VpnConnectionState
import com.rescuevpn.app.ui.screens.*
import com.rescuevpn.app.ui.theme.RescueVpnTheme
import com.rescuevpn.app.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val vpnPrepareLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.connect()
        } else {
            Toast.makeText(this, "مجوز VPN صادر نشد.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RescueVpnTheme {
                var currentScreen by remember { mutableStateOf("dashboard") }

                val connectionState by viewModel.connectionState.collectAsState()
                val profiles by viewModel.profiles.collectAsState()
                val selectedId by viewModel.selectedProfileId.collectAsState()
                val duration by viewModel.durationSeconds.collectAsState()
                val lastError by viewModel.lastError.collectAsState()

                val selectedProfile = profiles.firstOrNull { it.id == selectedId }

                when (currentScreen) {
                    "dashboard" -> {
                        MainDashboardScreen(
                            state = connectionState,
                            selectedProfile = selectedProfile,
                            durationSeconds = duration,
                            lastError = lastError,
                            onConnectToggle = {
                                if (connectionState == VpnConnectionState.CONNECTED) {
                                    viewModel.disconnect()
                                } else {
                                    prepareAndConnect()
                                }
                            },
                            onNavigateConfigs = { currentScreen = "configs" },
                            onNavigateAbout = { currentScreen = "about" },
                            onPanicWipe = {
                                viewModel.wipeAllData()
                                Toast.makeText(this, "تمام کانفیگ‌ها و اطلاعات با موفقیت امحا شد.", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    "configs" -> {
                        ConfigManagementScreen(
                            profiles = profiles,
                            selectedProfileId = selectedId,
                            onSelectProfile = { viewModel.selectProfile(it) },
                            onAddProfile = { name, content -> viewModel.addProfile(name, content) },
                            onDeleteProfile = { viewModel.deleteProfile(it) },
                            onBack = { currentScreen = "dashboard" }
                        )
                    }
                    "about" -> {
                        AboutScreen(onBack = { currentScreen = "dashboard" })
                    }
                }
            }
        }
    }

    private fun prepareAndConnect() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPrepareLauncher.launch(intent)
        } else {
            viewModel.connect()
        }
    }
}
