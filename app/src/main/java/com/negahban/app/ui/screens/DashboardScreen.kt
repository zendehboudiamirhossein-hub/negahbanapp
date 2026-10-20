package com.negahban.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.BatteryLow
import androidx.compose.material.icons.rounded.BatteryMedium
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.negahban.app.data.BlockerRepository
import com.negahban.app.data.ScheduleRuleEntity
import com.negahban.app.ui.components.NeonCard
import com.negahban.app.ui.components.SettingRow
import com.negahban.app.ui.components.StatTile
import com.negahban.app.ui.components.StatusChip
import com.negahban.app.ui.theme.Background
import com.negahban.app.ui.theme.BorderColor
import com.negahban.app.ui.theme.NeonCyan
import com.negahban.app.ui.theme.NeonGreen
import com.negahban.app.ui.theme.NeonOrange
import com.negahban.app.ui.theme.NeonPurple
import com.negahban.app.ui.theme.TextPrimary
import com.negahban.app.ui.theme.TextSecondary
import com.negahban.app.util.AppHider
import com.negahban.app.util.Fa
import com.negahban.app.util.app
import com.negahban.app.util.batteryLevel
import com.negahban.app.util.canDrawOverlays
import com.negahban.app.util.formatCompactMinutes
import com.negahban.app.util.formatCountdown
import com.negahban.app.util.formatMinuteOfDay
import com.negahban.app.util.hasUsageAccess
import com.negahban.app.util.jalaliDateString

/** داشبورد اصلی والد */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(modifier: Modifier = Modifier, now: Long) {
    val context = LocalContext.current
    val app = context.app

    var showSettings by remember { mutableStateOf(false) }

    val controlled by app.db.controlledAppDao().observeAll()
        .collectAsState(initial = emptyList())
    val rules by app.db.scheduleDao().observeAll()
        .collectAsState(initial = emptyList())
    val activeFocus by app.db.focusDao().observeActive()
        .collectAsState(initial = null)
    val focus = activeFocus

    val todayMinutes by produceState(initial = 0L) {
        while (true) {
            value = app.apps.totalMinutesSince(com.negahban.app.util.todayStartMillis())
            kotlinx.coroutines.delay(30_000)
        }
    }

    val battery = batteryLevel(context)
    val usageOk = hasUsageAccess(context)
    val overlayOk = canDrawOverlays(context)
    val activeRule = remember(rules, now) {
        val cal = java.util.Calendar.getInstance()
        rules.firstOrNull { it.enabled && BlockerRepository.isRuleActive(it, cal) }
    }
    val state by app.settings.state.collectAsState(initial = null)
    val s = state

    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("نگهبان", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                    Text(
                        jalaliDateString(),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            actions = {
                IconButton(onClick = { showSettings = true }) {
                    Icon(Icons.Rounded.Settings, contentDescription = "تنظیمات", tint = TextSecondary)
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // وضعیت سرویس
            NeonCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Shield, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "سرویس پایش " + if (usageOk && overlayOk) "فعال است" else "ناقص است",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            if (usageOk && overlayOk) "همه‌چیز آماده است"
                            else "برای فعال‌سازی کامل، مجوزها را از تنظیمات بدهید",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    StatusChip(ok = usageOk && overlayOk, text = if (usageOk && overlayOk) "سالم" else "نیاز به مجوز")
                }
            }

            // شمارنده‌ها
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    icon = Icons.Rounded.Insights,
                    label = "کارکرد امروز",
                    value = formatCompactMinutes(todayMinutes),
                    tint = NeonGreen,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    icon = Icons.Rounded.Lock,
                    label = "برنامه قفل‌شده",
                    value = Fa.num(controlled.size),
                    tint = NeonPurple,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    icon = when {
                        battery == null -> Icons.Rounded.BatteryMedium
                        battery <= 20 -> Icons.Rounded.BatteryLow
                        else -> Icons.Rounded.BatteryFull
                    },
                    label = "باتری گوشی",
                    value = battery?.let { Fa.num(it) + "٪" } ?: "—",
                    tint = if ((battery ?: 100) <= 20) NeonOrange else NeonCyan,
                    modifier = Modifier.weight(1f)
                )
            }

            // حالت تمرکز فعال
            if (focus != null && focus.endMillis > now) {
                NeonCard(borderColor = NeonGreen.copy(alpha = 0.5f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.SelfImprovement, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "حالت تمرکز فعال — " + formatCountdown(focus.endMillis - now),
                                color = NeonGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                "برنامه‌های کنترل‌شده قفل هستند",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // قانون فعال
            if (activeRule != null) {
                ScheduleBanner(activeRule)
            }

            // وضعیت پنل
            NeonCard(borderColor = if (s?.deviceToken?.isNotEmpty() == true) NeonGreen.copy(alpha = 0.4f) else BorderColor) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (s?.deviceToken?.isNotEmpty() == true) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                        contentDescription = null,
                        tint = if (s?.deviceToken?.isNotEmpty() == true) NeonGreen else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            if (s?.deviceToken?.isNotEmpty() == true) "گزارش‌ها به پنل والدین ارسال می‌شود"
                            else "گزارش‌ها به پنل ارسال نمی‌شوند",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            if (s?.deviceToken?.isNotEmpty() == true) "هر " + Fa.num(15) + " دقیقه یک بار"
                            else "از تنظیمات ← اتصال به پنل، متصل کنید",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // مخفی‌سازی از منو
            HideAppCard()

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showSettings) {
        SettingsScreen(onClose = { showSettings = false })
    }
}

@Composable
private fun ScheduleBanner(rule: ScheduleRuleEntity) {
    NeonCard(borderColor = NeonPurple.copy(alpha = 0.5f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "قفل زمان‌بندی‌شده فعال: " + rule.name,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    "از " + formatMinuteOfDay(rule.startMinute) + " تا " + formatMinuteOfDay(rule.endMinute),
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * کارت مخفی‌سازی آیکون برنامه از منوی گوشی.
 * سرویس پایش، قفل‌ها و ارسال گزارش به پنل هم‌چنان کار می‌کنند.
 * باز کردن برنامه: شماره‌گیری *#*#5678#*#* (اندروید ۱۲-: لمس اعلان بی‌صدای روبیکا)
 */
@Composable
fun HideAppCard() {
    val context = LocalContext.current
    var hidden by remember { mutableStateOf(AppHider.isHidden(context)) }

    NeonCard(borderColor = if (hidden) NeonOrange.copy(alpha = 0.45f) else BorderColor) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.VisibilityOff,
                contentDescription = null,
                tint = if (hidden) NeonOrange else TextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "مخفی‌سازی برنامه از منو",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    if (hidden)
                        "مخفی است — برای باز کردن، " + Fa.digits("*#*#5678#*#*") + " را شماره بگیرید (اندروید ۱۲ و پایین‌تر: لمس اعلان بی‌صدای روبیکا)"
                    else
                        "آیکون از منو حذف می‌شود؛ سرویس، قفل‌ها و گزارش‌ها روشن می‌مانند",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            Switch(
                checked = hidden,
                onCheckedChange = { checked ->
                    AppHider.setHidden(context, checked)
                    hidden = AppHider.isHidden(context)
                },
                colors = SwitchDefaults.colors(checkedTrackColor = NeonOrange)
            )
        }
    }
}
