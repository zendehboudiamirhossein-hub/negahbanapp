package com.negahban.app.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.negahban.app.data.AppsRepository
import com.negahban.app.ui.components.NeonCard
import com.negahban.app.ui.components.PackageIcon
import com.negahban.app.ui.components.SectionTitle
import com.negahban.app.ui.theme.BorderColor
import com.negahban.app.ui.theme.NeonCyan
import com.negahban.app.ui.theme.NeonGreen
import com.negahban.app.ui.theme.NeonOrange
import com.negahban.app.ui.theme.NeonPurple
import com.negahban.app.ui.theme.Surface2
import com.negahban.app.ui.theme.TextPrimary
import com.negahban.app.ui.theme.TextSecondary
import com.negahban.app.util.Fa
import com.negahban.app.util.app
import com.negahban.app.util.formatMinutes
import com.negahban.app.util.startOfWeekMillis
import com.negahban.app.util.todayStartMillis

/** آمار مصرف: نمودار هفتگی + پرمصرف‌ترین برنامه‌ها */
@Composable
fun StatsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.app

    val controlled by app.db.controlledAppDao().observeAll()
        .collectAsState(initial = emptyList())
    val controlledSet = remember(controlled) {
        controlled.map { it.packageName }.toSet()
    }

    val weekly by produceState(initialValue = null as List<AppsRepository.DayUsage>?) {
        value = app.apps.dailyUsageMinutes(7)
    }
    val topApps by produceState(initialValue = null as List<AppsRepository.AppUsage>?) {
        value = app.apps.appUsageSince(startOfWeekMillis(), emptySet()).take(10)
    }
    val todayMinutes by produceState(initialValue = 0L) {
        while (true) {
            value = app.apps.totalMinutesSince(todayStartMillis())
            kotlinx.coroutines.delay(30_000)
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        SectionTitle("آمار مصرف")

        // کارکرد امروز
        NeonCard(borderColor = NeonGreen.copy(alpha = 0.4f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.TrendingUp,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "کارکرد امروز گوشی",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        formatMinutes(todayMinutes),
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // نمودار ۷ روزه
        NeonCard {
            Column {
                Text(
                    "۷ روز اخیر",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(14.dp))
                val data = weekly
                if (data == null) {
                    Text(
                        "در حال محاسبه...",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    WeeklyBarChart(
                        data = data,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // پرمصرف‌ترین‌ها
        NeonCard {
            Column {
                Text(
                    "پرمصرف‌ترین برنامه‌های این هفته",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(12.dp))
                val top = topApps
                if (top == null || top.isEmpty()) {
                    Text(
                        "داده‌ای برای نمایش نیست",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                } else {
                    val max = top.first().minutes.coerceAtLeast(1)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        top.take(8).forEachIndexed { index, item ->
                            Column {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PackageIcon(pkg = item.packageName, size = 28.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = item.label,
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = formatMinutes(item.minutes),
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                androidx.compose.foundation.Canvas(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                ) {
                                    val w = size.width * (item.minutes.toFloat() / max)
                                    drawRoundRect(
                                        color = Surface2,
                                        cornerRadius = CornerRadius(4.dp.toPx()),
                                        size = Size(size.width, size.height)
                                    )
                                    drawRoundRect(
                                        color = if (index == 0) NeonGreen
                                        else if (item.packageName in controlledSet) NeonOrange
                                        else NeonPurple,
                                        cornerRadius = CornerRadius(4.dp.toPx()),
                                        size = Size(w.coerceAtLeast(6.dp.toPx()), size.height)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.EmojiEvents,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(16.dp)
            )
            Text(
                "نوار نارنجی یعنی برنامه کنترل‌شده است",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** نمودار ستونی ساده ۷ روز اخیر */
@Composable
private fun WeeklyBarChart(
    data: List<AppsRepository.DayUsage>,
    modifier: Modifier = Modifier
) {
    val maxMinutes = data.maxOfOrNull { it.minutes }?.coerceAtLeast(1) ?: 1
    // قدیمی‌ترین روز سمت راست (راست‌چین)
    val bars = data.asReversed()

    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (bars.isEmpty()) return@Canvas
            val gap = 8.dp.toPx()
            val barWidth = (size.width - gap * (bars.size - 1)) / bars.size
            bars.forEachIndexed { i, day ->
                val h = (day.minutes.toFloat() / maxMinutes) * size.height
                val isToday = i == bars.size - 1
                drawRoundRect(
                    color = if (isToday) NeonGreen else NeonPurple.copy(alpha = 0.7f),
                    topLeft = Offset(i * (barWidth + gap), size.height - h),
                    size = Size(barWidth, h.coerceAtLeast(3.dp.toPx())),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            bars.forEachIndexed { i, day ->
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = Fa.weekdayShortDaysAgo(day.daysAgo),
                        color = if (i == bars.size - 1) NeonGreen else TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
