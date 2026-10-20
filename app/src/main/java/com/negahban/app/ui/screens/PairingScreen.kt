package com.negahban.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.negahban.app.sync.SyncApi
import com.negahban.app.sync.SyncWorker
import com.negahban.app.ui.theme.Background
import com.negahban.app.ui.theme.NeonGreen
import com.negahban.app.ui.theme.NeonPurple
import com.negahban.app.ui.theme.TextPrimary
import com.negahban.app.ui.theme.TextSecondary
import com.negahban.app.util.Fa
import com.negahban.app.util.app
import com.negahban.app.util.deviceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * اتصال گوشی به پنل مدیریت وب.
 * در راه‌اندازی اولیه و در تنظیمات استفاده می‌شود.
 */
@Composable
fun PairingContent(
    compact: Boolean,
    onConnected: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val app = context.app
    val scope = rememberCoroutineScope()

    var serverUrl by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var ok by remember { mutableStateOf(false) }

    val state by app.settings.state.collectAsState(initial = null)
    val s = state
    val connected = s?.deviceToken?.isNotEmpty() == true

    Column {
        // وضعیت فعلی
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (connected) NeonGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (connected) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                    contentDescription = null,
                    tint = if (connected) NeonGreen else TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (connected) "متصل به پنل مدیریت" else "به هیچ پنلی متصل نیست",
                    color = if (connected) NeonGreen else TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (!connected) {
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("آدرس سرور (https://myapp.up.railway.app)") },
                singleLine = true,
                isError = error != null,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = code,
                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) code = it },
                label = { Text("کد جفت‌سازی ۶ رقمی از پنل") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                isError = error != null,
                supportingText = {
                    if (error != null) Text(error ?: "", color = MaterialTheme.colorScheme.error)
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    if (busy) return@Button
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            val url = SyncApi.normalizeUrl(serverUrl)
                            if (code.length != 6) throw Exception("کد باید ۶ رقم باشد")
                            val name = "گوشی " + deviceModel().take(24)
                            val (deviceId, token) = withContext(Dispatchers.IO) {
                                SyncApi.register(url, code, name)
                            }
                            app.settings.setDeviceRegistration(url, deviceId, token)
                            SyncWorker.schedule(context)
                            SyncWorker.syncNow(context)
                            ok = true
                            onConnected?.invoke()
                        } catch (e: Exception) {
                            error = e.message ?: "اتصال ناموفق بود"
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy && serverUrl.isNotBlank() && code.length == 6,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple, contentColor = Background),
                shape = MaterialTheme.shapes.medium
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        color = Background,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        "اتصال به پنل",
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
            if (ok) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "متصل شد! گزارش‌ها هر " + Fa.num(15) + " دقیقه ارسال می‌شوند.",
                    color = NeonGreen,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else if (!compact) {
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = {
                    scope.launch { app.settings.clearDeviceRegistration() }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("قطع اتصال", color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { SyncWorker.syncNow(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ارسال فوری گزارش", color = NeonPurple)
            }
        }
    }
}
