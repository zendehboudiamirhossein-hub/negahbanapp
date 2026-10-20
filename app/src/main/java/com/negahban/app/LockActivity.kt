package com.negahban.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.negahban.app.data.BlockReason
import com.negahban.app.ui.components.NeonCard
import com.negahban.app.ui.components.PackageIcon
import com.negahban.app.ui.components.PinVerifyDialog
import com.negahban.app.ui.theme.Background
import com.negahban.app.ui.theme.BorderColor
import com.negahban.app.ui.theme.NeonGreen
import com.negahban.app.ui.theme.NeonPink
import com.negahban.app.ui.theme.NeonPurple
import com.negahban.app.ui.theme.NegahbanTheme
import com.negahban.app.ui.theme.Surface2
import com.negahban.app.ui.theme.TextPrimary
import com.negahban.app.ui.theme.TextSecondary
import com.negahban.app.util.app
import com.negahban.app.util.formatCountdown
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * صفحه‌ای که روی برنامه‌های قفل‌شده نمایش داده می‌شود.
 * تا وقتی دلیل قفل برقرار است، کاربر نمی‌تواند از برنامه استفاده کند.
 */
class LockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pkg = intent.getStringExtra(EXTRA_PACKAGE)
        if (pkg == null) {
            finish()
            return
        }
        shownPackage = pkg
        isShowing = true

        setContent {
            NegahbanTheme {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl
                ) {
                    LockScreen(pkg = pkg, onHome = { goHome() }, onFinish = { finish() })
                }
            }
        }
    }

    private fun goHome() {
        val home = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(home)
        } catch (e: Exception) {
        }
        finish()
    }

    override fun onDestroy() {
        isShowing = false
        shownPackage = null
        super.onDestroy()
    }

    companion object {
        var isShowing = false
            private set
        var shownPackage: String? = null
            private set

        private const val EXTRA_PACKAGE = "pkg"
        const val BREAK_MINUTES = 5L

        fun launch(context: Context, pkg: String) {
            val intent = Intent(context, LockActivity::class.java)
                .putExtra(EXTRA_PACKAGE, pkg)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
            }
        }
    }
}

@Composable
private fun LockScreen(
    pkg: String,
    onHome: () -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val app = context.app
    val scope = rememberCoroutineScope()
    var reason by remember { mutableStateOf<BlockReason?>(null) }
    var showPin by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    // ساعت جاری برای شمارش معکوس زنده
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    // هر ثانیه دلیل قفل بررسی می‌شود؛ اگر قفل برداشته شد، صفحه بسته می‌شود
    LaunchedEffect(pkg) {
        while (true) {
            val r = app.blocker.currentBlockReason(pkg)
            if (r == null) {
                onFinish()
                break
            }
            reason = r
            delay(1000)
        }
    }

    BackHandler { onHome() }

    Column(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF141A2E), Background)
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // آیکون قفل درخشان
        Box(
            Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(NeonPurple.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = NeonPurple,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "دسترسی مسدود شد",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = app.apps.appLabel(pkg),
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(Modifier.height(24.dp))

        // کارت دلیل قفل
        NeonCard(borderColor = NeonPink.copy(alpha = 0.5f)) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val r = reason
                when (r) {
                    is BlockReason.ScheduleLock -> {
                        Text("بر اساس زمان‌بندی", fontSize = 12.sp, color = TextSecondary, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "«" + (r as BlockReason.ScheduleLock).ruleName + "»",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonPink,
                            textAlign = TextAlign.Center
                        )
                        Text("فعال است و این برنامه الان قفل شده", fontSize = 12.sp, color = TextSecondary, textAlign = TextAlign.Center)
                    }

                    is BlockReason.FocusLock -> {
                        val remaining = (r as BlockReason.FocusLock).endMillis - now
                        Text("حالت تمرکز فعال است", fontSize = 12.sp, color = TextSecondary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            formatCountdown(remaining),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen
                        )
                        Text("تا پایان تمرکز، برنامه‌ها در دسترس نیستند", fontSize = 12.sp, color = TextSecondary, textAlign = TextAlign.Center)
                    }

                    is BlockReason.InstantLock -> {
                        Text("قفل فوری", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeonPink)
                        Text("والد همه‌ی برنامه‌های کنترل‌شده را موقتاً قفل کرده است", fontSize = 12.sp, color = TextSecondary, textAlign = TextAlign.Center)
                    }

                    null -> {}
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // آیکون برنامه
        Row(verticalAlignment = Alignment.CenterVertically) {
            PackageIcon(pkg = pkg, size = 36.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                text = app.apps.appLabel(pkg),
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onHome,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple, contentColor = Background),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("برگرد به صفحه‌ی اصلی", fontSize = 15.sp, modifier = Modifier.padding(vertical = 6.dp))
        }

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = { showPin = true }) {
            Text("ورود والد — " + com.negahban.app.util.Fa.num(LockActivity.BREAK_MINUTES) + " دقیقه استراحت", color = TextSecondary, fontSize = 13.sp)
        }
    }

    if (showPin) {
        PinVerifyDialog(
            title = "رمز والد را وارد کنید",
            onSuccess = {
                showPin = false
                scope.launch {
                    app.settings.setBreakUntil(
                        System.currentTimeMillis() + LockActivity.BREAK_MINUTES * 60_000L
                    )
                }
                onFinish()
            },
            onDismiss = { showPin = false }
        )
    }
}
