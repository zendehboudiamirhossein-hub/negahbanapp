package com.negahban.app.sync

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * خواندن موقعیت مکانی برای نمایش روی نقشه پنل والدین.
 * راهبرد: آخرین موقعیت شناخته‌شده (همیشه بدون محدودیت کار می‌کند)؛
 * اگر نبود، یک ثبت تازه با اتماف مهلت درخواست می‌شود.
 */
object LocationReader {

    data class Fix(
        val lat: Double,
        val lng: Double,
        val accuracy: Float?,
        val provider: String,
        val at: Long
    )

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    private fun providers(): List<String> = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER
    )

    /** بهترین آخرین موقعیت شناخته‌شده از میان همه‌ی پروایدرها */
    fun bestLastKnown(context: Context): Location? = try {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        providers().mapNotNull { p ->
            try {
                if (lm.isProviderEnabled(p)) lm.getLastKnownLocation(p) else null
            } catch (e: Exception) {
                null
            }
        }.maxByOrNull { it.time }
    } catch (e: Exception) {
        null
    }

    /**
     * آخرین موقعیت یا (اگر هیچ‌کدام موجود نبود) یک ثبت تازه.
     * اگر در مهلت داده‌شده ثابتی نرسید null برمی‌گردد.
     */
    suspend fun currentFix(context: Context, timeoutMs: Long = 20_000L): Location? {
        bestLastKnown(context)?.let { return it }
        return withTimeoutOrNull(timeoutMs) { requestOneShot(context) }
    }

    private suspend fun requestOneShot(context: Context): Location? {
        var thread: HandlerThread? = null
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            thread = HandlerThread("loc_fix").also { it.start() }
            val handler = Handler(thread.looper)

            return suspendCancellableCoroutine { cont ->
                var done = false
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (done) return
                        done = true
                        try { lm.removeUpdates(this) } catch (e: Exception) {}
                        cont.resume(location)
                    }

                    override fun onProviderDisabled(provider: String) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                }

                var registered = false
                for (p in providers()) {
                    try {
                        if (lm.isProviderEnabled(p)) {
                            lm.requestLocationUpdates(p, 0L, 0f, listener, handler.looper)
                            registered = true
                        }
                    } catch (e: Exception) {
                    }
                }
                if (!registered) {
                    cont.resume(null)
                    return@suspendCancellableCoroutine
                }
                cont.invokeOnCancellation {
                    try { lm.removeUpdates(listener) } catch (e: Exception) {}
                }
            }
        } catch (e: Exception) {
            return null
        } finally {
            thread?.quitSafely()
        }
    }

    fun toFix(loc: Location): Fix = Fix(
        lat = loc.latitude,
        lng = loc.longitude,
        accuracy = if (loc.hasAccuracy()) loc.accuracy else null,
        provider = loc.provider ?: "unknown",
        at = loc.time
    )
}
