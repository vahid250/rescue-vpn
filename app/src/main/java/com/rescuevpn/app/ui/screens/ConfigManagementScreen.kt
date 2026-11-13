package com.rescuevpn.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.provider.OpenableColumns
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rescuevpn.app.data.model.WireGuardProfile
import com.rescuevpn.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigManagementScreen(
    profiles: List<WireGuardProfile>,
    selectedProfileId: String?,
    onSelectProfile: (String) -> Unit,
    onAddProfile: (name: String, content: String) -> Pair<Boolean, String?>,
    onDeleteProfile: (String) -> Unit,
    onBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var configNameInput by remember { mutableStateOf("") }
    var configContentInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val fileName = context.contentResolver.query(
                    uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    } else null
                } ?: "wireguard.conf"

                val content = context.contentResolver.openInputStream(uri)?.use { input ->
                    BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
                } ?: throw IllegalStateException("خواندن فایل انتخاب‌شده ممکن نشد.")

                if (content.isBlank()) {
                    throw IllegalArgumentException("فایل انتخاب‌شده خالی است.")
                }

                configNameInput = fileName.removeSuffix(".conf").removeSuffix(".txt")
                configContentInput = content
                errorMessage = null
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "خواندن فایل ناموفق بود."
            }
        }
    }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = { Text("مدیریت کانفیگ‌های WireGuard", color = TextPrimary, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryEmerald,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "افزودن کانفیگ")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (profiles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "هیچ کانفیگی ثبت نشده است.\nبرای ورود کانفیگ واقعی دکمه + را بزنید.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(profiles) { profile ->
                        val isSelected = profile.id == selectedProfileId
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) SurfaceCardInner else SurfaceCard
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, if (isSelected) PrimaryEmerald else BorderSubtle, RoundedCornerShape(12.dp))
                                .clickable { onSelectProfile(profile.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onSelectProfile(profile.id) },
                                    colors = RadioButtonDefaults.colors(selectedColor = PrimaryEmerald)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = profile.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = "سرور: ${profile.endpoint}", color = TextSecondary, fontSize = 12.sp)
                                    Text(text = "AllowedIPs: ${profile.allowedIps}", color = Color(0xFF6B7280), fontSize = 11.sp)
                                }
                                IconButton(onClick = { onDeleteProfile(profile.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = DangerRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false; errorMessage = null },
            containerColor = SurfaceCard,
            title = { Text("ورود کانفیگ WireGuard (.conf)", color = TextPrimary, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = configNameInput,
                        onValueChange = { configNameInput = it },
                        label = { Text("نام سرور / کانفیگ") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = { filePicker.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("انتخاب فایل کانفیگ از گوشی")
                    }
                    Text(
                        text = "فایل .conf یا فایل متنی کانفیگ WireGuard را انتخاب کنید؛ محتوا خودکار وارد می‌شود.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    OutlinedTextField(
                        value = configContentInput,
                        onValueChange = { configContentInput = it },
                        label = { Text("محتوای کانفیگ WireGuard") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        maxLines = 10
                    )
                    if (errorMessage != null) {
                        Text(text = "❌ $errorMessage", color = DangerRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val (success, err) = onAddProfile(configNameInput, configContentInput)
                        if (success) {
                            showAddDialog = false
                            configNameInput = ""
                            configContentInput = ""
                            errorMessage = null
                        } else {
                            errorMessage = err
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                ) {
                    Text("اعتبارسنجی و ذخیره")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false; errorMessage = null }) {
                    Text("انصراف", color = TextSecondary)
                }
            }
        )
    }
}
