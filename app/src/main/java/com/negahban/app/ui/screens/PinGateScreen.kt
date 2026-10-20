package com.negahban.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.negahban.app.ui.components.PinDots
import com.negahban.app.ui.components.PinPad
import com.negahban.app.ui.theme.NeonPurple
import com.negahban.app.ui.theme.TextPrimary
import com.negahban.app.ui.theme.TextSecondary
import com.negahban.app.util.app
import kotlinx.coroutines.delay

/** درگاه ورود رمز والد برای باز کردن پنل */
@Composable
fun PinGateScreen(onUnlock: () -> Unit) {
    val context = LocalContext.current
    val app = context.app
    var entered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }

    LaunchedEffect(entered) {
        if (entered.length == 4 && !checking) {
            checking = true
            val ok = app.settings.checkPin(entered)
            if (ok) {
                onUnlock()
            } else {
                error = true
                delay(700)
                entered = ""
                error = false
            }
            checking = false
        }
    }

    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(96.dp))
        Text("نگهبان", style = MaterialTheme.typography.titleLarge, color = NeonPurple)
        Spacer(Modifier.height(6.dp))
        Text(
            "رمز والد را وارد کنید",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(36.dp))
        PinDots(filled = entered.length, total = 4)
        if (error) {
            Spacer(Modifier.height(10.dp))
            Text("رمز اشتباه است", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(36.dp))
        PinPad(
            onDigit = { if (entered.length < 4 && !checking) entered += it },
            onDelete = { if (entered.isNotEmpty()) entered = entered.dropLast(1) }
        )
    }
}
