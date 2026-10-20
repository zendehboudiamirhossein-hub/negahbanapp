package com.negahban.app.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * مخفی/نمایان کردن آیکون برنامه در منوی گوشی (لانچر).
 *
 * آیکون لانچر در واقع یک activity-alias است (LauncherAlias در Manifest).
 * با غیرفعال کردن آن، آیکون از منو حذف می‌شود اما خود برنامه، سرویس پایش
 * و همگام‌سازی با پنل همچنان کار می‌کنند.
 */
object AppHider {

    private fun aliasComponent(context: Context): ComponentName =
        ComponentName(context.packageName, context.packageName + ".LauncherAlias")

    /** آیا آیکون الان مخفی است؟ (DEFAULT = طبق Manifest = نمایان) */
    fun isHidden(context: Context): Boolean = try {
        context.packageManager.getComponentEnabledSetting(aliasComponent(context)) ==
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    } catch (e: Exception) {
        false
    }

    /**
     * مخفی یا نمایان کردن آیکون.
     * DONT_KILL_APP: پروسه و سرویس‌ها کشته نمی‌شوند.
     */
    fun setHidden(context: Context, hidden: Boolean) {
        try {
            context.packageManager.setComponentEnabledSetting(
                aliasComponent(context),
                if (hidden) PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
        }
    }
}
