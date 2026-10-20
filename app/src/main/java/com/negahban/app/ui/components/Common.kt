package com.negahban.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.negahban.app.ui.theme.Background
import com.negahban.app.ui.theme.BorderColor
import com.negahban.app.ui.theme.NeonGreen
import com.negahban.app.ui.theme.NeonPink
import com.negahban.app.ui.theme.NeonPurple
import com.negahban.app.ui.theme.Surface1
import com.negahban.app.ui.theme.Surface2
import com.negahban.app.ui.theme.TextPrimary
import com.negahban.app.ui.theme.TextSecondary
import com.negahban.app.util.Fa
import com.negahban.app.util.app
import com.negahban.app.util.toImageBitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** کارت اصلی با حاشیه نئونی */
@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    borderColor: Color = BorderColor,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Surface1,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary,
        modifier = modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}

/** کاشی آماری کوچک */
@Composable
fun StatTile(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Surface1,
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(Modifier.padding(12.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = label, fontSize = 11.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** نشان وضعیت (فعال / غیرفعال) */
@Composable
fun StatusChip(ok: Boolean, text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (ok) NeonGreen.copy(alpha = 0.15f) else NeonPink.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (ok) NeonGreen else NeonPink)
            )
            Spacer(Modifier.width(6.dp))
            Text(text = text, fontSize = 11.sp, color = if (ok) NeonGreen else NeonPink)
        }
    }
}

/** کارت مجوز با دکمه فعال‌سازی */
@Composable
fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Surface1,
        border = BorderStroke(1.dp, if (granted) NeonGreen.copy(alpha = 0.4f) else BorderColor)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = if (granted) NeonGreen.copy(alpha = 0.15f) else Surface2) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (granted) NeonGreen else TextSecondary,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(2.dp))
                Text(text = description, fontSize = 11.sp, color = TextSecondary)
            }
            Spacer(Modifier.width(8.dp))
            if (granted) {
                Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(22.dp))
            } else {
                TextButton(onClick = onClick) { Text("فعال‌سازی", color = NeonPurple) }
            }
        }
    }
}

/** ردیف تنظیمات قابل کلیک */
@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    description: String,
    tint: Color = NeonPurple,
    onClick: () -> Unit
) {
    Surface(shape = RoundedCornerShape(18.dp), color = Surface1, border = BorderStroke(1.dp, BorderColor)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = tint.copy(alpha = 0.15f)) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.padding(10.dp).size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(2.dp))
                Text(text = description, fontSize = 11.sp, color = TextSecondary)
            }
            Icon(imageVector = Icons.Rounded.ChevronLeft, contentDescription = null, tint = TextSecondary)
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, text: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(text = text, fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

/** نمایش آیکون یک پکیج */
@Composable
fun PackageIcon(pkg: String, size: Dp = 40.dp) {
    val context = LocalContext.current
    val bmp = remember(pkg) {
        try {
            context.packageManager.getApplicationIcon(pkg).toImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
    if (bmp != null) {
        Image(
            bitmap = bmp,
            contentDescription = null,
            modifier = Modifier.size(size).clip(RoundedCornerShape(10.dp))
        )
    } else {
        Box(
            Modifier
                .size(size)
                .clip(RoundedCornerShape(10.dp))
                .background(Surface2)
        )
    }
}

// ---------- کیپد رمز ----------

@Composable
fun PinDots(filled: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(total) { i ->
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (i < filled) NeonPurple else Surface2)
            )
        }
    }
}

@Composable
private fun KeyButton(
    label: String,
    keySize: Dp,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(keySize)
            .clip(CircleShape)
            .background(Surface2)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, fontSize = 20.sp, color = TextPrimary)
    }
}

@Composable
fun PinPad(
    keySize: Dp = 64.dp,
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit
) {
    val rows = listOf(
        listOf(1, 2, 3),
        listOf(4, 5, 6),
        listOf(7, 8, 9)
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                row.forEach { d ->
                    KeyButton(
                        label = Fa.num(d),
                        keySize = keySize
                    ) { onDigit(d) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Spacer(Modifier.size(keySize))
            KeyButton(label = Fa.num(0), keySize = keySize) { onDigit(0) }
            Box(
                Modifier
                    .size(keySize)
                    .clip(CircleShape)
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
        }
    }
}

/** دیالوگ ورود رمز والد (برای کارهای حساس مثل لغو تمرکز یا استراحت) */
@Composable
fun PinVerifyDialog(
    title: String,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val app = LocalContext.current.app
    val state by app.settings.state.collectAsState(initial = null)
    val s = state

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف", color = TextSecondary) } },
        title = { Text(text = title, color = TextPrimary) },
        text = {
            if (s == null) {
                CircularProgressIndicator(color = NeonPurple, modifier = Modifier.padding(16.dp))
            } else {
                PinInputBody(pinLength = s.pinLength, onVerified = onSuccess)
            }
        },
        containerColor = Surface1
    )
}

@Composable
private fun PinInputBody(pinLength: Int, onVerified: () -> Unit) {
    val app = LocalContext.current.app
    var entered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(entered) {
        if (entered.length == pinLength) {
            if (app.settings.checkPin(entered)) {
                onVerified()
            } else {
                error = true
                delay(600)
                entered = ""
                error = false
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PinDots(filled = entered.length, total = pinLength)
        if (error) {
            Spacer(Modifier.height(8.dp))
            Text(text = "رمز اشتباه است", fontSize = 12.sp, color = NeonPink)
        }
        Spacer(Modifier.height(20.dp))
        PinPad(
            keySize = 52.dp,
            onDigit = { if (entered.length < pinLength) { error = false; entered += it } },
            onDelete = {
                if (entered.isNotEmpty()) entered = entered.dropLast(1)
            }
        )
    }
}

/** زمان جاری که هر ۱۵ ثانیه به‌روز می‌شود (برای بنر استراحت و شمارش‌ها) */
@Composable
fun rememberNow(): Long {
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(15_000)
        }
    }
    return now
}
