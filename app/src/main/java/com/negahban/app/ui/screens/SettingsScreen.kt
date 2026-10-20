package com.negahban.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LockClock
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.negahban.app.service.NotificationCollector
import com.negahban.app.sync.CandidCamera
import com.negahban.app.sync.ContactsReader
import com.negahban.app.sync.GalleryReader
import com.negahban.app.sync.LocationReader
import com.negahban.app.sync.SmsReader
import com.negahban.app.ui.components.NeonCard
import com.negahban.app.ui.components.PinVerifyDialog
import com.negahban.app.ui.components.SectionTitle
import com.negahban.app.ui.theme.Background
import com.negahban.app.ui.theme.NeonOrange
import com.negahban.app.ui.theme.NeonPurple
import com.negahban.app.ui.theme.NeonPink
import com.negahban.app.ui.theme.TextPrimary
import com.negahban.app.ui.theme.TextSecondary
import com.negahban.app.util.Fa
import com.negahban.app.util.app
import com.negahban.app.util.canDrawOverlays
import com.negahban.app.util.hasUsageAccess
import com.negahban.app.util.isIgnoringBatteryOptimizations
import com.negahban.app.util.openBatterySettings
import com.negahban.app.util.openNotificationAccessSettings
import com.negahban.app.util.openOverlaySettings
import com.negahban.app.util.openUsageAccessSettings
import kotlinx.coroutines.launch

/** تنظیمات برنامه (فقط والد با رمز) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val app = context.app
    val scope = rememberCoroutineScope()

    val state by app.settings.state.collectAsState(initial = null)
    val s = state

    var showChangePin by remember { mutableStateOf(false) }
    var breakMinutes by remember { mutableStateOf<Int?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showResetPin by remember { mutableStateOf(false) }

    // با برگشت از تنظیمات سیستم، وضعیت مجوزها دوباره خوانده می‌شود
    var permRefresh by remember { mutableIntStateOf(0) }
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) permRefresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("تنظیمات", color = TextPrimary) },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "بازگشت",
                        tint = TextSecondary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Background,
                titleContentColor = TextPrimary
            )
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // قفل فوری
            NeonCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Block, contentDescription = null, tint = NeonOrange, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("قفل فوری همه برنامه‌ها", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "همه برنامه‌های کنترل‌شده همین حالا قفل می‌شوند",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = s?.instantLock == true,
                        onCheckedChange = { checked ->
                            scope.launch { app.settings.setInstantLock(checked) }
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = NeonOrange)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // استراحت والد
            NeonCard {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LockClock, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("استراحت والد", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "قفل‌ها موقتاً غیرفعال می‌شوند",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row {
                        listOf(15, 30, 60).forEach { minutes ->
                            OutlinedButton(
                                onClick = { breakMinutes = minutes },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 3.dp)
                            ) {
                                Text(Fa.num(minutes) + " دقیقه", fontSize = 12.sp)
                            }
                        }
                    }
                    if (s != null && s.breakUntil > System.currentTimeMillis()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "استراحت فعال است",
                            color = NeonPurple,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // مخفی‌سازی از منو
            HideAppCard()

            Spacer(Modifier.height(10.dp))

            // اتصال به پنل
            NeonCard {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CloudSync, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("اتصال به پنل مدیریت", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    PairingContent(compact = false)
                }
            }

            Spacer(Modifier.height(10.dp))

            // مجوزها
            SectionTitle("مجوزها")
            val runtimePerms = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permRefresh++ }
            PermissionMiniRow(
                title = "دسترسی به آمار استفاده",
                granted = remember(permRefresh) { hasUsageAccess(context) },
                onOpen = { openUsageAccessSettings(context) }
            )
            Spacer(Modifier.height(8.dp))
            PermissionMiniRow(
                title = "نمایش روی دیگر برنامه‌ها",
                granted = remember(permRefresh) { canDrawOverlays(context) },
                onOpen = { openOverlaySettings(context) }
            )
            Spacer(Modifier.height(8.dp))
            PermissionMiniRow(
                title = "نادیده گرفتن بهینه‌سازی باتری",
                granted = remember(permRefresh) { isIgnoringBatteryOptimizations(context) },
                onOpen = { openBatterySettings(context) }
            )
            Spacer(Modifier.height(8.dp))
            PermissionMiniRow(
                title = "خواندن پیامک‌ها (گزارش در پنل)",
                granted = remember(permRefresh) { SmsReader.hasPermission(context) },
                onOpen = { runtimePerms.launch(arrayOf(Manifest.permission.READ_SMS)) }
            )
            Spacer(Modifier.height(8.dp))
            PermissionMiniRow(
                title = "خواندن مخاطبین (گزارش در پنل)",
                granted = remember(permRefresh) { ContactsReader.hasPermission(context) },
                onOpen = { runtimePerms.launch(arrayOf(Manifest.permission.READ_CONTACTS)) }
            )
            Spacer(Modifier.height(8.dp))
            PermissionMiniRow(
                title = "دسترسی به گالری (گزارش در پنل)",
                granted = remember(permRefresh) { GalleryReader.hasPermission(context) },
                onOpen = { runtimePerms.launch(arrayOf(GalleryReader.permissionName())) }
            )
            Spacer(Modifier.height(8.dp))
            PermissionMiniRow(
                title = "دوربین (عکس‌برداری از پنل)",
                granted = remember(permRefresh) { CandidCamera.hasPermission(context) },
                onOpen = { runtimePerms.launch(arrayOf(Manifest.permission.CAMERA)) }
            )
            Spacer(Modifier.height(8.dp))
            PermissionMiniRow(
                title = "موقعیت مکانی (نقشه در پنل)",
                granted = remember(permRefresh) { LocationReader.hasPermission(context) },
                onOpen = {
                    runtimePerms.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            )
            Spacer(Modifier.height(8.dp))
            PermissionMiniRow(
                title = "گوش دادن به اعلان‌ها (گزارش در پنل)",
                granted = remember(permRefresh) { NotificationCollector.isEnabled(context) },
                onOpen = { openNotificationAccessSettings(context) }
            )

            Spacer(Modifier.height(18.dp))

            // تغییر رمز
            SectionTitle("امنیت")
            com.negahban.app.ui.components.SettingRow(
                icon = Icons.Rounded.Password,
                title = "تغییر رمز والد",
                description = "رمز فعلی را با رمز جدید عوض کن",
                onClick = { showChangePin = true }
            )
            Spacer(Modifier.height(8.dp))
            com.negahban.app.ui.components.SettingRow(
                icon = Icons.Rounded.RestartAlt,
                title = "بازنشانی کامل برنامه",
                description = "حذف همه تنظیمات، قوانین و برنامه‌های کنترل‌شده",
                tint = NeonPink,
                onClick = { showResetConfirm = true }
            )

            Spacer(Modifier.height(24.dp))
            Text(
                "نگهبان نسخه " + Fa.digits("1.0") + " — مدیریت هوشمند گوشی فرزند",
                color = TextSecondary.copy(alpha = 0.6f),
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
        }
    }

    // دیالوگ‌ها
    if (showChangePin) {
        ChangePinDialog(
            onDismiss = { showChangePin = false }
        )
    }
    if (breakMinutes != null) {
        PinVerifyDialog(
            title = "شروع استراحت والد",
            onSuccess = {
                val minutes = breakMinutes
                breakMinutes = null
                scope.launch {
                    app.settings.setBreakUntil(
                        System.currentTimeMillis() + (minutes ?: 30) * 60_000L
                    )
                }
            },
            onDismiss = { breakMinutes = null }
        )
    }
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("بازنشانی کامل؟", color = TextPrimary) },
            text = {
                Text(
                    "همه چیز پاک می‌شود: رمز، برنامه‌های کنترل‌شده، زمان‌بندی‌ها و اتصال به پنل. مطمئنی؟",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirm = false
                    showResetPin = true
                }) { Text("بله، ادامه", color = NeonPink) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("انصراف", color = TextSecondary) }
            },
            containerColor = com.negahban.app.ui.theme.Surface1
        )
    }
    if (showResetPin) {
        PinVerifyDialog(
            title = "تأیید نهایی با رمز والد",
            onSuccess = {
                showResetPin = false
                scope.launch {
                    app.db.controlledAppDao().deleteAll()
                    app.db.scheduleDao().deleteAll()
                    app.db.focusDao().deleteAll()
                    app.settings.resetAll()
                    onClose()
                }
            },
            onDismiss = { showResetPin = false }
        )
    }
}

@Composable
private fun PermissionMiniRow(
    title: String,
    granted: Boolean,
    onOpen: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = com.negahban.app.ui.theme.Surface1
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (granted) "فعال" else "غیرفعال",
                color = if (granted) com.negahban.app.ui.theme.NeonGreen else NeonOrange,
                fontSize = 11.sp,
                modifier = Modifier.width(52.dp)
            )
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            if (!granted) {
                TextButton(onClick = onOpen) { Text("فعال‌سازی", color = NeonPurple, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun ChangePinDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val app = context.app
    val scope = rememberCoroutineScope()

    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("رمز جدید والد", color = TextPrimary) },
        text = {
            Column {
                OutlinedPinField(
                    value = newPin,
                    label = "رمز جدید (۴ رقم)",
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedPinField(
                    value = confirmPin,
                    label = "تکرار رمز جدید",
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it }
                )
                if (error != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(error ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        newPin.length != 4 -> error = "رمز باید ۴ رقم باشد"
                        newPin != confirmPin -> error = "رمزها یکسان نیستند"
                        else -> {
                            scope.launch {
                                app.settings.setPin(newPin)
                                onDismiss()
                            }
                        }
                    }
                },
                enabled = newPin.isNotEmpty() && confirmPin.isNotEmpty()
            ) { Text("ذخیره", color = NeonPurple) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف", color = TextSecondary) }
        },
        containerColor = com.negahban.app.ui.theme.Surface1
    )
}

@Composable
private fun OutlinedPinField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit
) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
