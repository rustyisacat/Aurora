package com.rusty.aurora.notifications

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import java.io.ByteArrayOutputStream

/** Reads an installed app's launcher icon for the dashboard's notification
 *  list - same "dumb backend, serves bytes" role Aurora already plays for
 *  sound/photos, nothing rendered on-device. */
interface AppIconProvider {
    /** Null if [packageName] isn't installed (e.g. it was uninstalled after
     *  posting its last notification). */
    fun getIconPng(packageName: String): ByteArray?
}

class AppIconProviderImpl(private val context: Context) : AppIconProvider {

    override fun getIconPng(packageName: String): ByteArray? = runCatching {
        val drawable = context.packageManager.getApplicationIcon(packageName)
        drawable.toPngBytes()
    }.getOrNull()

    /** Manual conversion rather than androidx.core's Drawable.toBitmap() -
     *  avoids taking on that dependency just for this one call site. */
    private fun Drawable.toPngBytes(): ByteArray {
        val bitmap = (this as? BitmapDrawable)?.bitmap ?: drawToBitmap()
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    private fun Drawable.drawToBitmap(): Bitmap {
        val width = intrinsicWidth.coerceAtLeast(1)
        val height = intrinsicHeight.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        return bitmap
    }
}
