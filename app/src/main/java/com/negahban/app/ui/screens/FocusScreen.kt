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
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.negahban.app.data.BlockerRepository
import com.negahban.app.data.FocusSessionEntity
import com.negahban.app.ui.components.NeonCard
import com.negahban.app.ui.components.PinVerifyDialog
import com.negahban.app.ui.components.SectionTitle
import com.negahban.app.ui.theme.Background
import com.negahban.app.ui.theme.BorderColor
import com.negahban.app.ui.theme.NeonGreen
import com.negahban.app.ui.theme.NeonPurple
import com.negahban.app.ui.theme.Surface1
import com.negahban.app.ui.theme.TextPrimary
import com.negahban.app.ui.theme.TextSecondary
import com.negahban.app.util.Fa
import com.negahban.app.util.app
import com.negahban.app.util.formatCountdown
import com.negahban.app.util.startOfWeekMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** حالت تمرکز عمیق: قفل موقت همه برنامه‌های کنترل‌شده */
@Composable
fun FocusScreen(modifier: Modifier = Modifier, now: Long) {
    val context = LocalContext.current
    val app = context.app
    val scope = rememberCoroutineScope()

    val active by app.db.focusDao().observeActive()
        .collectAsState(initial = null)
    val session = active
    val activeValid = session != null && session.endMillis > now

    // آمار هفته
    val weekStats by produceState(initialValue = 0 to 0) {
        while (true) {
            val from = startOfWeekMillis()
            val minutes = app.db.focusDao().completedMinutesSince(from)
            val count = app.db.focusDao().completedCountSince(from)
            value = minutes to count
            delay(60_000)
        }
    }

    var showCancelPin by remember { mutableStateOf(false) }

    LaunchedEffect(session) {
        if (session != null && session.endMillis <= System.currentTimeMillis()) {
            app.db.focusDao().update(session.copy(status = BlockerRepository.STATUS_COMPLETED))
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        SectionTitle("حالت تمرکز")

        if (activeValid && session != null) {
            // جلسه فعال
            NeonCard(borderColor = NeonGreen.copy(alpha = 0.5f)) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Rounded.SelfImprovement,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = formatCountdown(session.endMillis - now),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "تا پایان تمرکز، برنامه‌های کنترل‌شده قفل هستند",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(18.dp))
                    OutlinedButton(
                        onClick = { showCancelPin = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("لغو تمرکز (با رمز والد)", color = TextSecondary)
                    }
                }
            }
        } else {
            // شروع جلسه جدید
            NeonCard {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        "یک جلسه تمرکز شروع کن؛ همه برنامه‌های کنترل‌شده تا پایان آن قفل می‌شوند.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    val durations = listOf(15, 25, 45, 60, 90)
                    durations.chunked(3).forEach { rowItems ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { minutes ->
                                Button(
                                    onClick = {
                                        scope.launch {
                                            // جلسه قبلی نیمه‌کاره را ببند
                                            val old = app.db.focusDao().active()
                                            if (old != null) {
                                                app.db.focusDao().update(
                                                    old.copy(status = BlockerRepository.STATUS_CANCELLED)
                                                )
                                            }
                                            app.db.focusDao().insert(
                                                FocusSessionEntity(
                                                    startMillis = System.currentTimeMillis(),
                                                    endMillis = System.currentTimeMillis() + minutes * 60_000L,
                                                    plannedMinutes = minutes,
                                                    status = BlockerRepository.STATUS_ACTIVE
                                                )
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeonPurple,
                                        contentColor = Background
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(
                                        Fa.num(minutes) + " دقیقه",
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            // پر کردن جای خالی ردیف آخر
                            repeat(3 - rowItems.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // آمار هفته
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Surface1
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp)) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        Fa.num(weekStats.second),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonPurple
                    )
                    Text("جلسه این هفته", fontSize = 11.sp, color = TextSecondary)
                }
                androidx.compose.foundation.Canvas(Modifier.width(1.dp).height(34.dp)) {
                    drawRect(color = BorderColor)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        Fa.num(weekStats.first),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen
                    )
                    Text("دقیقه تمرکز این هفته", fontSize = 11.sp, color = TextSecondary)
                }
            }
        }

        Spacer(Modifier.height(18.dp))
    }

    if (showCancelPin && session != null) {
        PinVerifyDialog(
            title = "لغو تمرکز",
            onSuccess = {
                showCancelPin = false
                scope.launch {
                    app.db.focusDao().update(
                        session.copy(status = BlockerRepository.STATUS_CANCELLED)
                    )
                }
            },
            onDismiss = { showCancelPin = false }
        )
    }
}
