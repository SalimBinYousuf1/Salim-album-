package com.example.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.example.data.db.AlbumEntity
import com.example.data.db.AlbumMediaCrossRef
import com.example.data.db.AppDatabase
import com.example.data.db.FavoriteEntity
import com.example.data.db.HiddenEntity
import com.example.data.db.RecentViewEntity
import com.example.data.db.TrashEntity
import com.example.data.model.AlbumType
import com.example.data.model.ExifData
import com.example.data.model.MediaItem
import com.example.data.model.SalimAlbum
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class MediaStoreRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context)
) {
    private val contentResolver: ContentResolver = context.contentResolver
    private val galleryDao = database.galleryDao()

    val favoriteIds: Flow<List<Long>> = galleryDao.getAllFavoriteIds()
    val userAlbums: Flow<List<AlbumEntity>> = galleryDao.getAllAlbums()
    val trashItems: Flow<List<TrashEntity>> = galleryDao.getAllTrash()
    val hiddenIds: Flow<List<Long>> = galleryDao.getAllHiddenIds()

    // Flow that emits when MediaStore changes
    fun observeMediaStore(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                trySend(Unit)
            }
        }

        try {
            contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
            contentResolver.registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Send initial trigger
        trySend(Unit)

        awaitClose {
            try {
                contentResolver.unregisterContentObserver(observer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun fetchAllMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()
        val favoriteSet = galleryDao.getAllFavoriteIds().firstOrNull()?.toSet() ?: emptySet()

        // 1. Fetch Images
        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.ORIENTATION,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATA
        )

        val imageSortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        try {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                imageProjection,
                null,
                null,
                imageSortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                val dateTakenCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                val dateModCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
                val sizeCol = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
                val widthCol = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
                val orientCol = cursor.getColumnIndex(MediaStore.Images.Media.ORIENTATION)
                val bucketIdCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val dataCol = cursor.getColumnIndex(MediaStore.Images.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "IMG_$id.jpg" else "IMG_$id.jpg"
                    val dateTaken = if (dateTakenCol >= 0) cursor.getLong(dateTakenCol) else 0L
                    val dateMod = if (dateModCol >= 0) cursor.getLong(dateModCol) * 1000L else System.currentTimeMillis()
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "image/jpeg" else "image/jpeg"
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val width = if (widthCol >= 0) cursor.getInt(widthCol) else 0
                    val height = if (heightCol >= 0) cursor.getInt(heightCol) else 0
                    val orient = if (orientCol >= 0) cursor.getInt(orientCol) else 0
                    val bucketId = if (bucketIdCol >= 0) cursor.getString(bucketIdCol) ?: "" else ""
                    val bucketName = if (bucketNameCol >= 0) cursor.getString(bucketNameCol) ?: "" else ""
                    val path = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""

                    val effectiveDate = if (dateTaken > 0) dateTaken else dateMod

                    items.add(
                        MediaItem(
                            id = id,
                            uri = contentUri,
                            displayName = name,
                            dateTaken = effectiveDate,
                            dateModified = dateMod,
                            mimeType = mime,
                            size = size,
                            width = width,
                            height = height,
                            orientation = orient,
                            duration = 0L,
                            isVideo = false,
                            bucketId = bucketId,
                            bucketDisplayName = bucketName,
                            path = path,
                            isFavorite = favoriteSet.contains(id)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Fetch Videos
        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATA
        )

        val videoSortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

        try {
            contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection,
                null,
                null,
                videoSortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val dateTakenCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN)
                val dateModCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val durationCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val bucketIdCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "VID_$id.mp4" else "VID_$id.mp4"
                    val dateTaken = if (dateTakenCol >= 0) cursor.getLong(dateTakenCol) else 0L
                    val dateMod = if (dateModCol >= 0) cursor.getLong(dateModCol) * 1000L else System.currentTimeMillis()
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "video/mp4" else "video/mp4"
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val width = if (widthCol >= 0) cursor.getInt(widthCol) else 0
                    val height = if (heightCol >= 0) cursor.getInt(heightCol) else 0
                    val duration = if (durationCol >= 0) cursor.getLong(durationCol) else 0L
                    val bucketId = if (bucketIdCol >= 0) cursor.getString(bucketIdCol) ?: "" else ""
                    val bucketName = if (bucketNameCol >= 0) cursor.getString(bucketNameCol) ?: "" else ""
                    val path = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""

                    val effectiveDate = if (dateTaken > 0) dateTaken else dateMod

                    items.add(
                        MediaItem(
                            id = id,
                            uri = contentUri,
                            displayName = name,
                            dateTaken = effectiveDate,
                            dateModified = dateMod,
                            mimeType = mime,
                            size = size,
                            width = width,
                            height = height,
                            orientation = 0,
                            duration = duration,
                            isVideo = true,
                            bucketId = bucketId,
                            bucketDisplayName = bucketName,
                            path = path,
                            isFavorite = favoriteSet.contains(id)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Sort all by date modified / taken descending by default
        items.sortByDescending { it.dateTaken }
        items
    }

    suspend fun toggleFavorite(mediaId: Long) = withContext(Dispatchers.IO) {
        val isFav = galleryDao.isFavorite(mediaId)
        if (isFav) {
            galleryDao.deleteFavorite(mediaId)
        } else {
            galleryDao.insertFavorite(FavoriteEntity(mediaId = mediaId))
        }
    }

    suspend fun setFavorite(mediaId: Long, favorite: Boolean) = withContext(Dispatchers.IO) {
        if (favorite) {
            galleryDao.insertFavorite(FavoriteEntity(mediaId = mediaId))
        } else {
            galleryDao.deleteFavorite(mediaId)
        }
    }

    suspend fun recordRecentView(mediaId: Long) = withContext(Dispatchers.IO) {
        galleryDao.recordRecentView(RecentViewEntity(mediaId = mediaId))
    }

    suspend fun getRecentViewedIds(): List<Long> = withContext(Dispatchers.IO) {
        galleryDao.getRecentViewIds().firstOrNull() ?: emptyList()
    }

    // User Albums
    suspend fun createAlbum(name: String): Long = withContext(Dispatchers.IO) {
        galleryDao.insertAlbum(AlbumEntity(name = name.trim()))
    }

    suspend fun renameAlbum(albumId: Long, newName: String) = withContext(Dispatchers.IO) {
        val existing = galleryDao.getAlbumById(albumId) ?: return@withContext
        galleryDao.updateAlbum(existing.copy(name = newName.trim()))
    }

    suspend fun deleteAlbum(albumId: Long) = withContext(Dispatchers.IO) {
        galleryDao.removeAllMediaFromAlbum(albumId)
        galleryDao.deleteAlbum(albumId)
    }

    suspend fun addMediaToAlbum(albumId: Long, mediaIds: List<Long>) = withContext(Dispatchers.IO) {
        val entries = mediaIds.map { AlbumMediaCrossRef(albumId = albumId, mediaId = it) }
        galleryDao.addMediaBatchToAlbum(entries)
    }

    suspend fun removeMediaFromAlbum(albumId: Long, mediaId: Long) = withContext(Dispatchers.IO) {
        galleryDao.removeMediaFromAlbum(albumId, mediaId)
    }

    suspend fun getMediaIdsForAlbum(albumId: Long): List<Long> = withContext(Dispatchers.IO) {
        galleryDao.getMediaIdsForAlbumSync(albumId)
    }

    // Trash & Hidden Operations
    suspend fun moveToTrash(mediaId: Long) = withContext(Dispatchers.IO) {
        galleryDao.insertTrash(TrashEntity(mediaId = mediaId))
    }

    suspend fun restoreFromTrash(mediaId: Long) = withContext(Dispatchers.IO) {
        galleryDao.deleteTrash(mediaId)
    }

    suspend fun hideMedia(mediaId: Long) = withContext(Dispatchers.IO) {
        galleryDao.insertHidden(HiddenEntity(mediaId = mediaId))
    }

    suspend fun unhideMedia(mediaId: Long) = withContext(Dispatchers.IO) {
        galleryDao.deleteHidden(mediaId)
    }

    // Real File Operations
    suspend fun deleteMedia(items: List<MediaItem>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            for (item in items) {
                contentResolver.delete(item.uri, null, null)
                galleryDao.deleteTrash(item.id)
                galleryDao.deleteFavorite(item.id)
                galleryDao.deleteHidden(item.id)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMediaUris(uris: List<Uri>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            for (uri in uris) {
                contentResolver.delete(uri, null, null)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renameMedia(uri: Uri, isVideo: Boolean, newDisplayName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, newDisplayName)
            }
            val rows = contentResolver.update(uri, values, null, null)
            if (rows > 0) Result.success(Unit) else Result.failure(Exception("Could not rename file"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveMediaCopy(sourceUri: Uri, originalName: String, isVideo: Boolean): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val extension = originalName.substringAfterLast('.', if (isVideo) "mp4" else "jpg")
            val baseName = originalName.substringBeforeLast('.')
            val newName = "${baseName}_copy_${System.currentTimeMillis()}.$extension"

            val targetCollection = if (isVideo) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, newName)
                put(MediaStore.MediaColumns.MIME_TYPE, if (isVideo) "video/$extension" else "image/$extension")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, if (isVideo) "Movies/Salim" else "Pictures/Salim")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val newUri = contentResolver.insert(targetCollection, values)
                ?: return@withContext Result.failure(Exception("Failed to allocate destination"))

            contentResolver.openInputStream(sourceUri)?.use { input ->
                contentResolver.openOutputStream(newUri)?.use { output ->
                    input.copyTo(output)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                contentResolver.update(newUri, values, null, null)
            }

            Result.success(newUri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Extract EXIF data from Uri
    suspend fun getExifData(item: MediaItem): ExifData = withContext(Dispatchers.IO) {
        var cameraMake = ""
        var cameraModel = ""
        var lens = ""
        var iso = ""
        var aperture = ""
        var shutterSpeed = ""
        var focalLength = ""
        var lat: Double? = null
        var lon: Double? = null

        if (!item.isVideo) {
            try {
                contentResolver.openInputStream(item.uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    cameraMake = exif.getAttribute(ExifInterface.TAG_MAKE) ?: ""
                    cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL) ?: ""
                    lens = exif.getAttribute(ExifInterface.TAG_LENS_MODEL) ?: ""
                    iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY) ?: ""
                    aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { "f/$it" } ?: ""
                    shutterSpeed = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let { "${it}s" } ?: ""
                    focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let { "${it}mm" } ?: ""

                    val latLong = FloatArray(2)
                    if (exif.getLatLong(latLong)) {
                        lat = latLong[0].toDouble()
                        lon = latLong[1].toDouble()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy • h:mm a", Locale.getDefault())
        val dateTakenStr = dateFormat.format(Date(item.dateTaken))
        val dateModStr = dateFormat.format(Date(item.dateModified))

        ExifData(
            fileName = item.displayName,
            folder = item.bucketDisplayName.ifEmpty { "External Storage" },
            dateTaken = dateTakenStr,
            dateModified = dateModStr,
            fileSize = item.formattedSize,
            resolution = if (item.width > 0 && item.height > 0) "${item.width} × ${item.height} (${String.format("%.1f", (item.width * item.height) / 1000000.0)} MP)" else "Unknown",
            orientation = "${item.orientation}°",
            format = item.mimeType.uppercase().substringAfterLast('/'),
            cameraMake = cameraMake,
            cameraModel = cameraModel,
            lens = lens,
            iso = iso,
            aperture = aperture,
            shutterSpeed = shutterSpeed,
            focalLength = focalLength,
            latitude = lat,
            longitude = lon
        )
    }
}
