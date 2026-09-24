package com.example.editor

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import kotlin.math.*

enum class AspectRatioPreset(val label: String, val ratio: Float?) {
    ORIGINAL("Original", null),
    FREE("Free", null),
    SQUARE("1:1", 1.0f),
    RATIO_4_3("4:3", 4f / 3f),
    RATIO_3_4("3:4", 3f / 4f),
    RATIO_16_9("16:9", 16f / 9f),
    RATIO_9_16("9:16", 9f / 16f)
}

data class EditAdjustments(
    val brightness: Float = 0f,    // -100 to 100, default 0
    val contrast: Float = 1f,      // 0.5 to 2.0, default 1.0
    val saturation: Float = 1f,    // 0.0 to 2.0, default 1.0
    val warmth: Float = 0f,        // -50 to 50, default 0
    val vignette: Float = 0f,      // 0 to 100, default 0
    val rotationAngle: Int = 0,    // 0, 90, 180, 270
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val cropRectNormalized: RectF? = null // Left, Top, Right, Bottom [0..1]
)

object BitmapProcessor {

    suspend fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun decodeFullBitmapFromUri(
        context: Context,
        uri: Uri
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return max(1, inSampleSize)
    }

    fun applyEdits(source: Bitmap, adjustments: EditAdjustments): Bitmap {
        // 1. Transformations: Rotation & Flip
        val matrix = Matrix()
        if (adjustments.flipHorizontal) {
            matrix.postScale(-1f, 1f)
        }
        if (adjustments.flipVertical) {
            matrix.postScale(1f, -1f)
        }
        if (adjustments.rotationAngle != 0) {
            matrix.postRotate(adjustments.rotationAngle.toFloat())
        }

        var transformed = if (matrix.isIdentity) {
            source
        } else {
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        }

        // 2. Crop
        adjustments.cropRectNormalized?.let { rect ->
            val cropL = (rect.left * transformed.width).toInt().coerceIn(0, transformed.width - 1)
            val cropT = (rect.top * transformed.height).toInt().coerceIn(0, transformed.height - 1)
            val cropR = (rect.right * transformed.width).toInt().coerceIn(cropL + 1, transformed.width)
            val cropB = (rect.bottom * transformed.height).toInt().coerceIn(cropT + 1, transformed.height)
            val w = cropR - cropL
            val h = cropB - cropT
            if (w > 10 && h > 10) {
                transformed = Bitmap.createBitmap(transformed, cropL, cropT, w, h)
            }
        }

        // 3. Color Filter Adjustments
        val result = Bitmap.createBitmap(transformed.width, transformed.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // ColorMatrix: Brightness, Contrast, Saturation, Warmth
        val cm = ColorMatrix()

        // Saturation
        val satMatrix = ColorMatrix().apply { setSaturation(adjustments.saturation) }
        cm.postConcat(satMatrix)

        // Contrast & Brightness
        // formula: v' = contrast * (v - 128) + 128 + brightness
        val c = adjustments.contrast
        val b = adjustments.brightness
        val translate = (-0.5f * c + 0.5f) * 255f + b

        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, translate,
                0f, c, 0f, 0f, translate,
                0f, 0f, c, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.postConcat(contrastMatrix)

        // Warmth: Warm boosts Red/Yellow, Cool boosts Blue
        if (adjustments.warmth != 0f) {
            val w = adjustments.warmth / 50f // -1 to 1
            val rBoost = if (w > 0) w * 25f else 0f
            val bBoost = if (w < 0) -w * 25f else 0f
            val warmthMatrix = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, rBoost,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1f, 0f, bBoost,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.postConcat(warmthMatrix)
        }

        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(transformed, 0f, 0f, paint)

        // 4. Vignette
        if (adjustments.vignette > 0f) {
            val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                val radius = max(result.width, result.height) * 0.75f
                val cx = result.width / 2f
                val cy = result.height / 2f
                val intensity = (adjustments.vignette / 100f * 0.7f).coerceIn(0f, 1f)
                val darkColor = Color.argb((intensity * 255).toInt(), 0, 0, 0)

                shader = RadialGradient(
                    cx, cy, radius,
                    intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, darkColor),
                    floatArrayOf(0.0f, 0.45f, 1.0f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, result.width.toFloat(), result.height.toFloat(), vignettePaint)
        }

        return result
    }

    suspend fun saveEditedBitmap(
        context: Context,
        sourceUri: Uri,
        adjustments: EditAdjustments,
        baseName: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            // Load full resolution
            val fullBitmap = decodeFullBitmapFromUri(context, sourceUri)
                ?: return@withContext Result.failure(Exception("Failed to load original full-resolution image"))

            val processed = applyEdits(fullBitmap, adjustments)

            val cleanName = baseName.substringBeforeLast('.')
            val fileName = "${cleanName}_edited_${System.currentTimeMillis()}.jpg"

            val contentResolver = context.contentResolver
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

            val resultUri = contentResolver.insert(targetCollection, values)
                ?: return@withContext Result.failure(Exception("Failed to create destination image in MediaStore"))

            contentResolver.openOutputStream(resultUri)?.use { out ->
                processed.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(resultUri, values, null, null)
            }

            Result.success(resultUri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
