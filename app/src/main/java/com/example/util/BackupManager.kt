package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import com.example.data.model.MediaItem

object BackupManager {

    const val GOOGLE_PHOTOS_PACKAGE = "com.google.android.apps.photos"

    fun isGooglePhotosInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(GOOGLE_PHOTOS_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun openGooglePhotos(context: Context) {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(GOOGLE_PHOTOS_PACKAGE)
            if (launchIntent != null) {
                context.startActivity(launchIntent)
            } else {
                val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$GOOGLE_PHOTOS_PACKAGE"))
                context.startActivity(storeIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open Google Photos", Toast.LENGTH_SHORT).show()
        }
    }

    fun uploadItemToGooglePhotos(context: Context, item: MediaItem) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType
                putExtra(Intent.EXTRA_STREAM, item.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (isGooglePhotosInstalled(context)) {
                    setPackage(GOOGLE_PHOTOS_PACKAGE)
                }
            }
            context.startActivity(Intent.createChooser(intent, "Back up to Google Photos"))
        } catch (e: Exception) {
            Toast.makeText(context, "Backup failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun uploadBatchToGooglePhotos(context: Context, items: List<MediaItem>) {
        if (items.isEmpty()) return
        try {
            val uris = ArrayList<Uri>(items.map { it.uri })
            val mime = if (items.all { !it.isVideo }) "image/*" else if (items.all { it.isVideo }) "video/*" else "*/*"
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mime
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (isGooglePhotosInstalled(context)) {
                    setPackage(GOOGLE_PHOTOS_PACKAGE)
                }
            }
            context.startActivity(Intent.createChooser(intent, "Back up ${items.size} items to Google Photos"))
        } catch (e: Exception) {
            Toast.makeText(context, "Backup failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
