package com.example.editor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

object VideoProcessor {

    suspend fun extractFrame(
        context: Context,
        videoUri: Uri,
        timeMs: Long,
        baseName: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val timeUs = timeMs * 1000L
            val frame: Bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(timeUs)
                ?: return@withContext Result.failure(Exception("Could not extract frame at ${timeMs}ms"))

            val cleanName = baseName.substringBeforeLast('.')
            val fileName = "${cleanName}_frame_${timeMs}ms.jpg"

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Salim")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val targetCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val resultUri = context.contentResolver.insert(targetCollection, values)
                ?: return@withContext Result.failure(Exception("Failed to create destination image in MediaStore"))

            context.contentResolver.openOutputStream(resultUri)?.use { out ->
                frame.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(resultUri, values, null, null)
            }

            Result.success(resultUri)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                retriever.release()
            } catch (ignored: Exception) {}
        }
    }

    suspend fun trimVideo(
        context: Context,
        videoUri: Uri,
        startMs: Long,
        endMs: Long,
        isMuted: Boolean,
        baseName: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        val tempOutputFile = File(context.cacheDir, "trimmed_${System.currentTimeMillis()}.mp4")
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            extractor.setDataSource(context, videoUri, null)
            val trackCount = extractor.trackCount
            muxer = MediaMuxer(tempOutputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val trackMap = mutableMapOf<Int, Int>()
            var bufferSize = 1024 * 1024

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""

                if (isMuted && mime.startsWith("audio/")) {
                    // Skip audio track if muted
                    continue
                }

                if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                    val newSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                    if (newSize > bufferSize) bufferSize = newSize
                }

                val muxerTrackIndex = muxer.addTrack(format)
                trackMap[i] = muxerTrackIndex
            }

            muxer.start()

            val startUs = startMs * 1000L
            val endUs = endMs * 1000L

            val buffer = ByteBuffer.allocate(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            for ((extractorTrackIndex, muxerTrackIndex) in trackMap) {
                extractor.selectTrack(extractorTrackIndex)
                extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

                while (true) {
                    bufferInfo.size = extractor.readSampleData(buffer, 0)
                    if (bufferInfo.size < 0) break

                    bufferInfo.presentationTimeUs = extractor.sampleTime
                    if (bufferInfo.presentationTimeUs > endUs) {
                        break
                    }

                    if (bufferInfo.presentationTimeUs >= startUs) {
                        bufferInfo.flags = extractor.sampleFlags
                        bufferInfo.offset = 0
                        muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                    }

                    extractor.advance()
                }
                extractor.unselectTrack(extractorTrackIndex)
            }

            muxer.stop()
            muxer.release()
            muxer = null
            extractor.release()

            // Now insert trimmed file into MediaStore Movies/Salim
            val cleanName = baseName.substringBeforeLast('.')
            val fileName = "${cleanName}_trimmed_${System.currentTimeMillis()}.mp4"

            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Salim")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val targetCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val resultUri = context.contentResolver.insert(targetCollection, values)
                ?: return@withContext Result.failure(Exception("Failed to allocate destination video in MediaStore"))

            tempOutputFile.inputStream().use { input ->
                context.contentResolver.openOutputStream(resultUri)?.use { output ->
                    input.copyTo(output)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                context.contentResolver.update(resultUri, values, null, null)
            }

            tempOutputFile.delete()
            Result.success(resultUri)
        } catch (e: Exception) {
            tempOutputFile.delete()
            Result.failure(e)
        } finally {
            try {
                muxer?.release()
            } catch (ignored: Exception) {}
            try {
                extractor.release()
            } catch (ignored: Exception) {}
        }
    }
}
