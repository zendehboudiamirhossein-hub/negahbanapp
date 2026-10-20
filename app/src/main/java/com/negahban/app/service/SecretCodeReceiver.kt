package com.negahban.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.negahban.app.MainActivity

/**
 * باز کردن برنامه با شماره‌گیری مخفی *#*#5678#*#*
 * (وقتی آیکون از منو مخفی شده است، راه اصلی باز کردن برنامه همین است؛
 * راه دوم لمس اعلان دائمی «نگهبان» در بخش اعلان‌هاست)
 */
class SecretCodeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // ثابت ACTION_SECRET_CODE در SDK عمومی نیست؛ رشته خام مقایسه می‌شود
        if (intent.action != "android.intent.action.SECRET_CODE") return

        try {
            context.startActivity(
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
        }
    }
}
