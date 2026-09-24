package com.example.util

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object MediaActions {

    fun shareSingleMedia(context: Context, item: MediaItem) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType
                putExtra(Intent.EXTRA_STREAM, item.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share media via"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareMultipleMedia(context: Context, items: List<MediaItem>) {
        if (items.isEmpty()) return
        try {
            val uris = ArrayList<Uri>(items.map { it.uri })
            val mime = if (items.all { !it.isVideo }) "image/*" else if (items.all { it.isVideo }) "video/*" else "*/*"
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mime
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share ${items.size} items via"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWithOtherApp(context: Context, item: MediaItem) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(item.uri, item.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: Exception) {
            Toast.makeText(context, "No app available to open this file", Toast.LENGTH_SHORT).show()
        }
    }

    suspend fun setAsWallpaper(context: Context, item: MediaItem): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(item.uri)?.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream) ?: return@withContext false
                val wm = WallpaperManager.getInstance(context)
                wm.setBitmap(bitmap)
                return@withContext true
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun openMapLocation(context: Context, latitude: Double, longitude: Double, label: String = "Photo Location") {
        try {
            val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($label)")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No maps app found on device", Toast.LENGTH_SHORT).show()
        }
    }
}
