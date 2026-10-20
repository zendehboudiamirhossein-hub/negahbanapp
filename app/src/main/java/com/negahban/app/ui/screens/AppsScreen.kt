package com.negahban.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.negahban.app.data.AppsRepository
import com.negahban.app.data.ControlledAppEntity
import com.negahban.app.ui.components.EmptyState
import com.negahban.app.ui.components.PackageIcon
import com.negahban.app.ui.components.SectionTitle
import com.negahban.app.ui.theme.BorderColor
import com.negahban.app.ui.theme.NeonPurple
import com.negahban.app.ui.theme.Surface1
import com.negahban.app.ui.theme.TextPrimary
import com.negahban.app.ui.theme.TextSecondary
import com.negahban.app.util.Fa
import com.negahban.app.util.app
import kotlinx.coroutines.launch

/** مدیریت برنامه‌های تحت کنترل */
@Composable
fun AppsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.app
    val scope = rememberCoroutineScope()

    val controlled by app.db.controlledAppDao().observeAll()
        .collectAsState(initial = emptyList())
    val controlledSet = remember(controlled) {
        controlled.map { it.packageName }.toSet()
    }

    val apps by produceState(initial = null as List<AppsRepository.AppInfo>?) {
        value = app.apps.launchableApps()
    }

    var query by remember { mutableStateOf("") }

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        SectionTitle("برنامه‌های تحت کنترل")
        Text(
            "تیک هر برنامه را بزن تا هنگام تمرکز یا زمان‌بندی قفل شود. " +
                Fa.num(controlledSet.size) + " برنامه انتخاب شده است.",
            color = TextSecondary,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("جستجوی برنامه...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = TextSecondary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        val list = apps
        if (list == null) {
            Text(
                "در حال بارگذاری برنامه‌ها...",
                color = TextSecondary,
                fontSize = 13.sp
            )
        } else {

        val filtered = list.filter {
            query.isBlank() || it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
        }

        if (filtered.isEmpty()) {
            EmptyState(icon = Icons.Rounded.Search, text = "برنامه‌ای یافت نشد")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.packageName }) { info ->
                    val isControlled = info.packageName in controlledSet
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Surface1,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isControlled) NeonPurple.copy(alpha = 0.5f) else BorderColor
                        ),
                        modifier = Modifier.clickable {
                            scope.launch {
                                if (isControlled) {
                                    app.db.controlledAppDao().delete(info.packageName)
                                } else {
                                    app.db.controlledAppDao()
                                        .insert(ControlledAppEntity(info.packageName))
                                }
                            }
                        }
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PackageIcon(pkg = info.packageName, size = 40.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = info.label,
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = info.packageName,
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                            Checkbox(
                                checked = isControlled,
                                onCheckedChange = { checked ->
                                    scope.launch {
                                        if (checked) {
                                            app.db.controlledAppDao()
                                                .insert(ControlledAppEntity(info.packageName))
                                        } else {
                                            app.db.controlledAppDao().delete(info.packageName)
                                        }
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = NeonPurple,
                                    uncheckedColor = TextSecondary
                                )
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
