package com.rescuevpn.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rescuevpn.app.core.vpn.VpnConnectionState
import com.rescuevpn.app.data.model.WireGuardProfile
import com.rescuevpn.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    state: VpnConnectionState,
    selectedProfile: WireGuardProfile?,
    durationSeconds: Long,
    lastError: String?,
    onConnectToggle: () -> Unit,
    onNavigateConfigs: () -> Unit,
    onNavigateAbout: () -> Unit,
    onPanicWipe: () -> Unit
) {
    val isConnected = state == VpnConnectionState.CONNECTED
    val isConnecting = state == VpnConnectionState.CONNECTING

    val durationText = remember(durationSeconds) {
        val hours = durationSeconds / 3600
        val mins = (durationSeconds % 3600) / 60
        val secs = durationSeconds % 60
        if (hours > 0) String.format("%02d:%02d:%02d", hours, mins, secs)
        else String.format("%02d:%02d", mins, secs)
    }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "R&MA", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = when (state) {
                                VpnConnectionState.CONNECTED -> "تونل پایدار WireGuard فعال است"
                                VpnConnectionState.CONNECTING -> "در حال برقراری هندشیک..."
                                VpnConnectionState.NO_CONFIG -> "هیچ سروری تنظیم نشده است"
                                VpnConnectionState.FAILED -> "خطای اتصال"
                                VpnConnectionState.DISCONNECTED -> "قطع اتصال"
                            },
                            color = when (state) {
                                VpnConnectionState.CONNECTED -> PrimaryEmerald
                                VpnConnectionState.CONNECTING -> WarningAmber
                                else -> TextSecondary
                            },
                            fontSize = 12.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark),
                actions = {
                    IconButton(onClick = onNavigateAbout) {
                        Icon(Icons.Default.Info, contentDescription = "درباره", tint = TextSecondary)
                    }
                    IconButton(onClick = onPanicWipe) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "امحای داده‌ها", tint = DangerRed)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // کارت کانفیگ انتخابی
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateConfigs() }
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(SurfaceCardInner),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = CyanAccent)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedProfile?.name ?: "سروری تنظیم نشده است (افزودن کانفیگ)",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = selectedProfile?.endpoint ?: "برای اتصال، ابتدا کانفیگ وایرگارد معتبر وارد کنید",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = TextSecondary)
                }
            }

            // پیام خطا در صورت وجود
            if (lastError != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x22EF4444)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0x66EF4444), RoundedCornerShape(12.dp))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = lastError, color = TextPrimary, fontSize = 12.sp)
                    }
                }
            }

            // دکمه بزرگ مرکزی
            Surface(
                onClick = onConnectToggle,
                shape = CircleShape,
                color = when {
                    isConnected -> PrimaryEmerald
                    isConnecting -> WarningAmber
                    else -> SurfaceCard
                },
                modifier = Modifier
                    .size(180.dp)
                    .border(2.dp, if (isConnected) PrimaryEmerald else BorderSubtle, CircleShape),
                shadowElevation = 8.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.PowerSettingsNew,
                        contentDescription = "اتصال",
                        tint = if (isConnected || isConnecting) Color.White else TextPrimary,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when {
                            isConnected -> "قطع اتصال"
                            isConnecting -> "در حال هندشیک..."
                            else -> "اتصال امن"
                        },
                        color = if (isConnected || isConnecting) Color.White else TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            // اطلاعات مدت اتصال
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = PrimaryEmerald)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "مدت اتصال فعال:", color = TextSecondary, fontSize = 13.sp)
                    }
                    Text(
                        text = if (isConnected) durationText else "--:--",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
