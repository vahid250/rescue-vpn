package com.rescuevpn.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rescuevpn.app.data.model.FreeStaticIpProvider
import com.rescuevpn.app.data.model.FreeStaticIpProviderCatalog
import com.rescuevpn.app.ui.theme.BgDark
import com.rescuevpn.app.ui.theme.BorderSubtle
import com.rescuevpn.app.ui.theme.PrimaryEmerald
import com.rescuevpn.app.ui.theme.SurfaceCard
import com.rescuevpn.app.ui.theme.TextPrimary
import com.rescuevpn.app.ui.theme.TextSecondary
import com.rescuevpn.app.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeServerFinderScreen(
    onBack: () -> Unit,
    onOpenOfficialSource: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val normalizedQuery = query.trim().lowercase()
    val results = remember(normalizedQuery) {
        FreeStaticIpProviderCatalog.providers.filter { provider ->
            normalizedQuery.isBlank() ||
                provider.name.lowercase().contains(normalizedQuery) ||
                provider.searchTerms.any { it.lowercase().contains(normalizedQuery) } ||
                provider.summary.lowercase().contains(normalizedQuery) ||
                provider.status.lowercase().contains(normalizedQuery) ||
                provider.ipBehavior.lowercase().contains(normalizedQuery) ||
                provider.limitations.lowercase().contains(normalizedQuery)
        }
    }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = { Text("جستجوی سرور و IP رایگان", color = TextPrimary, fontSize = 16.sp) },
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "فهرست منابع بررسی‌شده؛ جستجو در همین فهرست انجام می‌شود و اطلاعات آن با به‌روزرسانی نسخهٔ برنامه اصلاح خواهد شد.",
                color = TextSecondary,
                fontSize = 12.sp
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text("نام ارائه‌دهنده یا عبارت جستجو") }
            )
            Text("نتایج: ${results.size}", color = PrimaryEmerald, fontWeight = FontWeight.SemiBold)
            if (results.isEmpty()) {
                Text("موردی در فهرست فعلی پیدا نشد.", color = TextSecondary)
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(results, key = { it.id }) { provider ->
                        ProviderCard(provider = provider, onOpen = onOpenOfficialSource)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderCard(
    provider: FreeStaticIpProvider,
    onOpen: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(provider.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
                provider.status,
                color = WarningAmber,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
            Text(provider.summary, color = TextPrimary, fontSize = 13.sp)
            Text("منابع محاسباتی: ${provider.computeOptions}", color = TextSecondary, fontSize = 12.sp)
            Text("وضعیت IP: ${provider.ipBehavior}", color = TextSecondary, fontSize = 12.sp)
            Text("محدودیت‌ها: ${provider.limitations}", color = TextSecondary, fontSize = 12.sp)
            Text("آخرین بررسی مستندات: ${provider.lastReviewed} — وضعیت دسترسی زنده نیست", color = WarningAmber, fontSize = 11.sp)
            Button(
                onClick = { onOpen(provider.officialUrl) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Color.White)
                Text("مشاهده منبع رسمی", modifier = Modifier.padding(start = 6.dp), color = Color.White)
            }
            provider.setupGuideUrl?.let { guideUrl ->
                androidx.compose.material3.OutlinedButton(onClick = { onOpen(guideUrl) }) {
                    Text("راهنمای راه‌اندازی WireGuard")
                }
            }
        }
    }
}
