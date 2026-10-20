package com.negahban.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.negahban.app.service.NotificationCollector
import com.negahban.app.sync.CandidCamera
import com.negahban.app.sync.ContactsReader
import com.negahban.app.sync.GalleryReader
import com.negahban.app.sync.LocationReader
import com.negahban.app.sync.SmsReader
import com.negahban.app.ui.components.PermissionCard
import com.negahban.app.ui.components.PinDots
import com.negahban.app.ui.components.PinPad
import com.negahban.app.ui.theme.NeonGreen
import com.negahban.app.ui.theme.NeonPurple
import com.negahban.app.ui.theme.TextPrimary
import com.negahban.app.ui.theme.TextSecondary
import com.negahban.app.util.app
import com.negahban.app.util.canDrawOverlays
import com.negahban.app.util.hasUsageAccess
import com.negahban.app.util.isIgnoringBatteryOptimizations
import com.negahban.app.util.openBatterySettings
import com.negahban.app.util.openNotificationAccessSettings
import com.negahban.app.util.openOverlaySettings
import com.negahban.app.util.openUsageAccessSettings
import kotlinx.coroutines.launch

/**
 * راه‌اندازی اولیه: ساخت رمز والد ← مجوزها ← اتصال به پنل (اختیاری)
 */
@Composable
fun SetupScreen(onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }

    when (step) {
        0 -> WelcomeStep(onStart = { step = 1 })
        1 -> PinStep(onBack = { step = 0 }, onNext = { step = 2 })
        2 -> PermissionsStep(onBack = { step = 1 }, onNext = { step = 3 })
        3 -> PairStep(onFinish = onDone)
    }
}

@Composable
private fun SetupFrame(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        Text(
            text = "نگهبان",
            style = MaterialTheme.typography.titleLarge,
            color = NeonPurple
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        content()
    }
}

@Composable
private fun WelcomeStep(onStart: () -> Unit) {
    SetupFrame(
        title = "کنترل والدین هوشمند",
        subtitle = "با نگهبان، زمان استفاده از برنامه‌ها را مدیریت کن، حالت تمرکز بساز و همه‌چیز را در پنل والدین ببین."
    ) {
        val features = listOf(
            "قفل برنامه‌های حواس‌پرت‌کن",
            "زمان‌بندی قفل خودکار (مثلاً شب‌ها ۱۱ تا ۷)",
            "حالت تمرکز عمیق",
            "آمار مصرف با نمودار",
            "گزارش لحظه‌ای در پنل وب والدین"
        )
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            features.forEach { text ->
                androidx.compose.material3.Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "•  $text",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        Spacer(Modifier.height(30.dp))
        androidx.compose.material3.Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = NeonPurple,
                contentColor = androidx.compose.ui.graphics.Color(0xFF0A0E17)
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
        ) {
            Text("شروع راه‌اندازی", modifier = Modifier.padding(vertical = 6.dp))
        }
    }
}

@Composable
private fun PinStep(onBack: () -> Unit, onNext: () -> Unit) {
    val context = LocalContext.current
    val app = context.app
    val scope = rememberCoroutineScope()

    var firstPin by remember { mutableStateOf<String?>(null) }
    var entered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    val title = if (firstPin == null) "رمز والد بساز" else "تکرار رمز والد"
    val subtitle = if (firstPin == null)
        "این رمز فقط برای والد است؛ با آن می‌توانی قفل‌ها را موقتاً باز کنی. فرزند نباید آن را بداند."
    else
        "همان رمز را یک بار دیگر وارد کن"

    LaunchedEffect(entered) {
        if (entered.length == 4) {
            if (firstPin == null) {
                firstPin = entered
                entered = ""
            } else {
                if (entered == firstPin) {
                    scope.launch {
                        app.settings.setPin(entered)
                        onNext()
                    }
                } else {
                    error = true
                    kotlinx.coroutines.delay(600)
                    entered = ""
                    error = false
                }
            }
        }
    }

    SetupFrame(title = title, subtitle = subtitle) {
        PinDots(filled = entered.length, total = 4)
        if (error) {
            Spacer(Modifier.height(8.dp))
            Text("رمزها یکسان نیستند", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(32.dp))
        PinPad(
            onDigit = { if (entered.length < 4) entered += it },
            onDelete = { if (entered.isNotEmpty()) entered = entered.dropLast(1) }
        )
        Spacer(Modifier.height(20.dp))
        androidx.compose.material3.TextButton(onClick = onBack) {
            Text("بازگشت", color = TextSecondary)
        }
    }
}

@Composable
private fun PermissionsStep(onBack: () -> Unit, onNext: () -> Unit) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }

    // وقتی از تنظیمات سیستم برمی‌گردیم، وضعیت مجوزها دوباره خوانده می‌شود
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // مجوزهای دلخواهِ «داده‌های پنل» با یک بار درخواست runtime گرفته می‌شوند
    val runtimePerms = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refresh++ }

    val usageOk = remember(refresh) { hasUsageAccess(context) }
    val overlayOk = remember(refresh) { canDrawOverlays(context) }
    val batteryOk = remember(refresh) { isIgnoringBatteryOptimizations(context) }
    val smsOk = remember(refresh) { SmsReader.hasPermission(context) }
    val contactsOk = remember(refresh) { ContactsReader.hasPermission(context) }
    val galleryOk = remember(refresh) { GalleryReader.hasPermission(context) }
    val cameraOk = remember(refresh) { CandidCamera.hasPermission(context) }
    val locationOk = remember(refresh) { LocationReader.hasPermission(context) }
    val listenerOk = remember(refresh) { NotificationCollector.isEnabled(context) }

    SetupFrame(
        title = "مجوزهای لازم",
        subtitle = "دو مورد اول برای قفل کردن برنامه‌ها الزامی است. بقیه اختیاری‌اند: بدون هر کدام، فقط همان داده در پنل والدین دیده نمی‌شود."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PermissionCard(
                icon = Icons.Rounded.QueryStats,
                title = "دسترسی به آمار استفاده",
                description = "برای دیدن اینکه کدام برنامه باز است (الزامی)",
                granted = usageOk
            ) { openUsageAccessSettings(context); refresh++ }
            PermissionCard(
                icon = Icons.Rounded.Layers,
                title = "نمایش روی دیگر برنامه‌ها",
                description = "برای نمایش صفحه قفل روی برنامه‌های مسدود (الزامی)",
                granted = overlayOk
            ) { openOverlaySettings(context); refresh++ }
            PermissionCard(
                icon = Icons.Rounded.BatteryChargingFull,
                title = "نادیده گرفتن بهینه‌سازی باتری",
                description = "برای فعال ماندن سرویس پایش",
                granted = batteryOk
            ) { openBatterySettings(context); refresh++ }
            PermissionCard(
                icon = Icons.Rounded.Sms,
                title = "خواندن پیامک‌ها",
                description = "برای نمایش پیامک‌ها در پنل والدین",
                granted = smsOk
            ) { runtimePerms.launch(arrayOf(Manifest.permission.READ_SMS)); refresh++ }
            PermissionCard(
                icon = Icons.Rounded.Contacts,
                title = "خواندن مخاطبین",
                description = "برای نمایش لیست مخاطبین در پنل والدین",
                granted = contactsOk
            ) { runtimePerms.launch(arrayOf(Manifest.permission.READ_CONTACTS)); refresh++ }
            PermissionCard(
                icon = Icons.Rounded.PhotoLibrary,
                title = "دسترسی به گالری",
                description = "برای نمایش عکس‌ها در پنل والدین",
                granted = galleryOk
            ) { runtimePerms.launch(arrayOf(GalleryReader.permissionName())); refresh++ }
            PermissionCard(
                icon = Icons.Rounded.PhotoCamera,
                title = "دوربین",
                description = "برای عکس‌برداری از کاربر گوشی به فرمان پنل والدین",
                granted = cameraOk
            ) { runtimePerms.launch(arrayOf(Manifest.permission.CAMERA)); refresh++ }
            PermissionCard(
                icon = Icons.Rounded.LocationOn,
                title = "موقعیت مکانی",
                description = "برای نمایش موقعیت دقیق روی نقشه در پنل والدین",
                granted = locationOk
            ) {
                runtimePerms.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                ); refresh++
            }
            PermissionCard(
                icon = Icons.Rounded.NotificationsActive,
                title = "گوش دادن به اعلان‌ها",
                description = "برای نمایش اعلان‌های گوشی در پنل والدین",
                granted = listenerOk
            ) { openNotificationAccessSettings(context); refresh++ }
        }
        Spacer(Modifier.height(24.dp))
        if (usageOk && overlayOk) {
            androidx.compose.material3.Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = NeonGreen,
                    contentColor = androidx.compose.ui.graphics.Color(0xFF0A0E17)
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
            ) {
                Text("ادامه", modifier = Modifier.padding(vertical = 6.dp))
            }
        } else {
            val all = listOf(usageOk, overlayOk, batteryOk, smsOk, contactsOk, galleryOk, cameraOk, locationOk, listenerOk)
            LinearProgressIndicator(
                progress = { all.count { it } / all.size.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = NeonPurple
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "بعد از فعال‌سازی هر مجوز، به برنامه برگردید",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Spacer(Modifier.height(8.dp))
        androidx.compose.material3.TextButton(onClick = onBack) {
            Text("بازگشت", color = TextSecondary)
        }
    }
}

@Composable
private fun PairStep(onFinish: () -> Unit) {
    val context = LocalContext.current
    val app = context.app
    val scope = rememberCoroutineScope()

    SetupFrame(
        title = "اتصال به پنل والدین",
        subtitle = "اگر می‌خواهی گزارش کارکرد و لیست برنامه‌ها روی پنل تحت وب دیده شود، از پنل یک کد جفت‌سازی بگیر و اینجا وارد کن."
    ) {
        var connected by remember { mutableStateOf(false) }
        PairingContent(
            compact = false,
            onConnected = { connected = true }
        )
        Spacer(Modifier.height(20.dp))

        // مخفی‌سازی آیکون از منو (اختیاری، هر زمان قابل تغییر است)
        HideAppCard()
        Spacer(Modifier.height(20.dp))
        androidx.compose.material3.Button(
            onClick = {
                scope.launch {
                    app.settings.setSetupDone()
                    onFinish()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = NeonPurple,
                contentColor = androidx.compose.ui.graphics.Color(0xFF0A0E17)
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
        ) {
            Text(
                if (connected) "پایان راه‌اندازی" else "رد کردن و پایان",
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }
    }
}
