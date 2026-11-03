package com.rescuevpn.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rescuevpn.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = { Text("درباره Rescue VPN AI", color = TextPrimary, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth().border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Rescue VPN AI (نسخه ۱.۰.۰)", color = PrimaryEmerald, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "این نرم‌افزار یک کلاینت متن‌باز، بدون تبلیغات و کاملاً مستقل مبتنی بر موتور رسمی WireGuard Android SDK است. تمام فرآیندهای رمزنگاری و تونل‌سازی مستقیماً در دستگاه کاربر با استاندارد ChaCha20-Poly1305 انجام می‌شود.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 22.sp
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth().border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("محدودیت‌های فنی و شفافیت عملکرد", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "۱. این اپلیکیشن هیچ سرور یا کلید پیش‌فرضی ندارد. اتصال تنها با وارد کردن کانفیگ معتبر کاربر برقرار می‌شود.\n" +
                        "۲. وضعیت اتصال تنها زمانی سبز (CONNECTED) خواهد شد که موتور GoBackend وایرگارد فعال‌شدن اینترفیس را تایید کند.\n" +
                        "۳. کلیدهای خصوصی در Android KeyStore رمزگذاری می‌شوند و هرگز به سرور ثالثی ارسال نخواهند شد.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
