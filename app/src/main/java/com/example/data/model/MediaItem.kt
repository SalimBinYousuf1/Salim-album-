package com.example.data.model

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateTaken: Long,
    val dateModified: Long,
    val mimeType: String,
    val size: Long,
    val width: Int,
    val height: Int,
    val orientation: Int,
    val duration: Long = 0L,
    val isVideo: Boolean = false,
    val bucketId: String = "",
    val bucketDisplayName: String = "",
    val path: String = "",
    val isFavorite: Boolean = false
) {
    val isPanorama: Boolean
        get() = width > 0 && height > 0 && (width.toFloat() / height.toFloat() >= 2.5f || height.toFloat() / width.toFloat() >= 2.5f)

    val isLargeFile: Boolean
        get() = size > 25 * 1024 * 1024 // > 25MB

    val isRaw: Boolean
        get() = mimeType.contains("dng", ignoreCase = true) ||
                displayName.endsWith(".dng", ignoreCase = true) ||
                displayName.endsWith(".raw", ignoreCase = true) ||
                displayName.endsWith(".cr2", ignoreCase = true) ||
                displayName.endsWith(".nef", ignoreCase = true)

    val isScreenshot: Boolean
        get() = bucketDisplayName.contains("screenshot", ignoreCase = true) ||
                displayName.contains("screenshot", ignoreCase = true) ||
                path.contains("screenshot", ignoreCase = true)

    val formattedDuration: String
        get() {
            if (!isVideo || duration <= 0) return ""
            val totalSeconds = duration / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hours = minutes / 60
            return if (hours > 0) {
                val remMinutes = minutes % 60
                String.format("%d:%02d:%02d", hours, remMinutes, seconds)
            } else {
                String.format("%d:%02d", minutes, seconds)
            }
        }

    val formattedSize: String
        get() {
            val kb = size / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.1f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                kb >= 1.0 -> String.format("%.1f KB", kb)
                else -> "$size B"
            }
        }
}

enum class AlbumType {
    ALL,
    FAVORITES,
    VIDEOS,
    CAMERA,
    SCREENSHOTS,
    DOWNLOADS,
    RECENTLY_ADDED,
    LARGE_FILES,
    PANORAMAS,
    FOLDER,
    USER_CREATED
}

data class SalimAlbum(
    val id: String,
    val title: String,
    val count: Int,
    val coverUri: Uri?,
    val type: AlbumType,
    val rawId: Long? = null,
    val bucketId: String? = null
)

data class MediaSection(
    val title: String,
    val subtitle: String = "",
    val items: List<MediaItem>
)

data class ExifData(
    val fileName: String,
    val folder: String,
    val dateTaken: String,
    val dateModified: String,
    val fileSize: String,
    val resolution: String,
    val orientation: String,
    val format: String,
    val cameraMake: String = "",
    val cameraModel: String = "",
    val lens: String = "",
    val iso: String = "",
    val aperture: String = "",
    val shutterSpeed: String = "",
    val focalLength: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
)
