package com.negahban.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.negahban.app.service.WatchdogService
import com.negahban.app.sync.SyncWorker
import com.negahban.app.ui.components.rememberNow
import com.negahban.app.ui.screens.AppsScreen
import com.negahban.app.ui.screens.DashboardScreen
import com.negahban.app.ui.screens.FocusScreen
import com.negahban.app.ui.screens.PinGateScreen
import com.negahban.app.ui.screens.SchedulesScreen
import com.negahban.app.ui.screens.SetupScreen
import com.negahban.app.ui.screens.StatsScreen
import com.negahban.app.ui.theme.NegahbanTheme
import com.negahban.app.util.app

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NegahbanTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val app = context.app
    val settings by app.settings.state.collectAsState(initial = null)
    val s = settings

    var unlocked by remember { mutableStateOf(false) }

    if (s == null) {
        // در حال بارگذاری تنظیمات
        return
    }

    // بعد از راه‌اندازی یا ورود: روشن کردن سرویس پایش + همگام‌سازی
    LaunchedEffect(s.setupDone, unlocked) {
        if (s.setupDone && unlocked) {
            WatchdogService.start(context)
            SyncWorker.schedule(context)
            SyncWorker.syncNow(context)
        }
    }

    when {
        !s.setupDone -> SetupScreen(onDone = { unlocked = true })
        !unlocked -> PinGateScreen(onUnlock = { unlocked = true })
        else -> MainPanel()
    }
}

private data class NavItem(
    val label: String,
    val icon: ImageVector
)

@Composable
private fun MainPanel() {
    val items = remember {
        listOf(
            NavItem("داشبورد", Icons.Rounded.Home),
            NavItem("برنامه‌ها", Icons.Rounded.Apps),
            NavItem("زمان‌بندی", Icons.Rounded.Schedule),
            NavItem("تمرکز", Icons.Rounded.SelfImprovement),
            NavItem("آمار", Icons.Rounded.BarChart)
        )
    }
    var selected by remember { mutableStateOf(0) }
    val now = rememberNow()

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        val screen = Modifier.padding(padding)
        when (selected) {
            0 -> DashboardScreen(modifier = screen, now = now)
            1 -> AppsScreen(modifier = screen)
            2 -> SchedulesScreen(modifier = screen)
            3 -> FocusScreen(modifier = screen, now = now)
            4 -> StatsScreen(modifier = screen)
        }
    }
}
