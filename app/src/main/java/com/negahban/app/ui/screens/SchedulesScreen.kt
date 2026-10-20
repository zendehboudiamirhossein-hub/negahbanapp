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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.negahban.app.data.ScheduleRuleEntity
import com.negahban.app.ui.components.EmptyState
import com.negahban.app.ui.components.NeonCard
import com.negahban.app.ui.components.SectionTitle
import com.negahban.app.ui.theme.Background
import com.negahban.app.ui.theme.BorderColor
import com.negahban.app.ui.theme.NeonPurple
import com.negahban.app.ui.theme.Surface1
import com.negahban.app.ui.theme.TextPrimary
import com.negahban.app.ui.theme.TextSecondary
import com.negahban.app.util.Fa
import com.negahban.app.util.app
import com.negahban.app.util.dayBit
import com.negahban.app.util.formatMinuteOfDay
import java.util.Calendar
import kotlinx.coroutines.launch

/** زمان‌بندی قفل خودکار (مثلاً هر شب ۲۲ تا ۶) */
@Composable
fun SchedulesScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.app
    val scope = rememberCoroutineScope()

    val rules by app.db.scheduleDao().observeAll()
        .collectAsState(initial = emptyList())
    var showAdd by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        SectionTitle("زمان‌بندی قفل خودکار")
        Text(
            "در بازه‌های زمانی مشخص، همه برنامه‌های کنترل‌شده قفل می‌شوند. مثلاً ساعت خواب یا زمان درس.",
            color = TextSecondary,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(16.dp))

        if (rules.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Schedule,
                text = "هنوز زمان‌بندی‌ای نساخته‌ای.\nمثلاً: «ساعت خواب» هر شب ۲۲:۰۰ تا ۰۶:۰۰"
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(rules, key = { it.id }) { rule ->
                    RuleCard(
                        rule = rule,
                        onToggle = { enabled ->
                            scope.launch {
                                app.db.scheduleDao().update(rule.copy(enabled = enabled))
                            }
                        },
                        onDelete = {
                            scope.launch { app.db.scheduleDao().delete(rule) }
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { showAdd = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple, contentColor = Background),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("زمان‌بندی جدید", modifier = Modifier.padding(vertical = 6.dp))
        }
        Spacer(Modifier.height(16.dp))
    }

    if (showAdd) {
        AddRuleDialog(
            onDismiss = { showAdd = false },
            onSave = { name, start, end, days ->
                scope.launch {
                    app.db.scheduleDao().insert(
                        ScheduleRuleEntity(
                            name = name,
                            startMinute = start,
                            endMinute = end,
                            daysMask = days
                        )
                    )
                    showAdd = false
                }
            }
        )
    }
}

@Composable
private fun RuleCard(
    rule: ScheduleRuleEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val dayNames = listOf(
        Calendar.SATURDAY to "ش", Calendar.SUNDAY to "ی", Calendar.MONDAY to "د",
        Calendar.TUESDAY to "س", Calendar.WEDNESDAY to "چ",
        Calendar.THURSDAY to "پ", Calendar.FRIDAY to "ج"
    )
    NeonCard(borderColor = if (rule.enabled) NeonPurple.copy(alpha = 0.45f) else BorderColor) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = rule.name,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatMinuteOfDay(rule.startMinute) + " تا " + formatMinuteOfDay(rule.endMinute),
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    dayNames.forEach { (day, label) ->
                        val active = (rule.daysMask and dayBit(day)) != 0
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (active) NeonPurple.copy(alpha = 0.25f) else Surface1
                        ) {
                            Text(
                                text = label,
                                color = if (active) NeonPurple else TextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(
                    checked = rule.enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(checkedTrackColor = NeonPurple)
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "حذف",
                        tint = TextSecondary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRuleDialog(
    onDismiss: () -> Unit,
    onSave: (String, Int, Int, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var days by remember { mutableStateOf(0b1111111) } // همه روزها
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var startMinute by remember { mutableStateOf(22 * 60) }
    var endMinute by remember { mutableStateOf(6 * 60) }

    val startState = rememberTimePickerState(initialHour = 22, initialMinute = 0, is24Hour = true)
    val endState = rememberTimePickerState(initialHour = 6, initialMinute = 0, is24Hour = true)

    val daysList = listOf(
        Calendar.SATURDAY to "شنبه",
        Calendar.SUNDAY to "یکشنبه",
        Calendar.MONDAY to "دوشنبه",
        Calendar.TUESDAY to "سه‌شنبه",
        Calendar.WEDNESDAY to "چهارشنبه",
        Calendar.THURSDAY to "پنجشنبه",
        Calendar.FRIDAY to "جمعه"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), startMinute, endMinute, days)
                    }
                },
                enabled = name.isNotBlank()
            ) { Text("ذخیره", color = NeonPurple) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف", color = TextSecondary) }
        },
        title = { Text("زمان‌بندی جدید", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام (مثلاً ساعت خواب)") },
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Surface1,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("شروع", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                formatMinuteOfDay(startMinute),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Surface1,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("پایان", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                formatMinuteOfDay(endMinute),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }

                // دکمه‌های تغییر ساعت
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showStartPicker = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple, contentColor = Background)
                    ) { Text("تعیین شروع") }
                    Button(
                        onClick = { showEndPicker = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple, contentColor = Background)
                    ) { Text("تعیین پایان") }
                }

                Text("روزهای هفته:", fontSize = 12.sp, color = TextSecondary)
                // انتخاب روزها
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    daysList.forEach { (day, label) ->
                        val active = (days and dayBit(day)) != 0
                        FilterChip(
                            selected = active,
                            onClick = { days = days xor dayBit(day) },
                            label = { Text(Fa.weekdayShort(day), fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonPurple.copy(alpha = 0.3f),
                                selectedLabelColor = NeonPurple
                            )
                        )
                    }
                }
            }
        },
        containerColor = Surface1
    )

    if (showStartPicker) {
        AlertDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startMinute = startState.hour * 60 + startState.minute
                    showStartPicker = false
                }) { Text("تأیید", color = NeonPurple) }
            },
            title = { Text("ساعت شروع", color = TextPrimary) },
            text = { TimePicker(state = startState) },
            containerColor = Surface1
        )
    }
    if (showEndPicker) {
        AlertDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endMinute = endState.hour * 60 + endState.minute
                    showEndPicker = false
                }) { Text("تأیید", color = NeonPurple) }
            },
            title = { Text("ساعت پایان", color = TextPrimary) },
            text = { TimePicker(state = endState) },
            containerColor = Surface1
        )
    }
}
