package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaItem
import com.example.editor.AspectRatioPreset
import com.example.editor.BitmapProcessor
import com.example.editor.EditAdjustments
import com.example.ui.theme.SalimBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class EditorTab {
    CROP,
    ADJUST,
    ROTATE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(
    item: MediaItem,
    onCancel: () -> Unit,
    onSaveSuccess: (Uri) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Edit state & Undo/Redo History Stack
    var currentAdjustments by remember { mutableStateOf(EditAdjustments()) }
    val historyStack = remember { mutableStateListOf<EditAdjustments>() }
    val redoStack = remember { mutableStateListOf<EditAdjustments>() }

    var activeTab by remember { mutableStateOf(EditorTab.ADJUST) }
    var activeAdjustMode by remember { mutableStateOf("brightness") } // brightness, contrast, saturation, warmth, vignette

    var isComparingOriginal by remember { mutableStateOf(false) }

    // Load initial sampled bitmap for fast editing
    LaunchedEffect(item.uri) {
        isLoading = true
        val bmp = BitmapProcessor.decodeSampledBitmapFromUri(context, item.uri, 1200, 1200)
        originalBitmap = bmp
        previewBitmap = bmp
        isLoading = false
    }

    // Update preview whenever adjustments change
    LaunchedEffect(currentAdjustments, originalBitmap) {
        val orig = originalBitmap ?: return@LaunchedEffect
        withContext(Dispatchers.Default) {
            val edited = BitmapProcessor.applyEdits(orig, currentAdjustments)
            previewBitmap = edited
        }
    }

    fun applyChange(newAdjustments: EditAdjustments) {
        historyStack.add(currentAdjustments)
        redoStack.clear()
        currentAdjustments = newAdjustments
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Edit Photo",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.testTag("editor_cancel_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    // Undo
                    IconButton(
                        onClick = {
                            if (historyStack.isNotEmpty()) {
                                val prev = historyStack.removeLast()
                                redoStack.add(currentAdjustments)
                                currentAdjustments = prev
                            }
                        },
                        enabled = historyStack.isNotEmpty(),
                        modifier = Modifier.testTag("editor_undo_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }

                    // Redo
                    IconButton(
                        onClick = {
                            if (redoStack.isNotEmpty()) {
                                val next = redoStack.removeLast()
                                historyStack.add(currentAdjustments)
                                currentAdjustments = next
                            }
                        },
                        enabled = redoStack.isNotEmpty(),
                        modifier = Modifier.testTag("editor_redo_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                    }

                    // Save as Copy
                    Button(
                        onClick = {
                            isSaving = true
                            coroutineScope.launch {
                                val result = BitmapProcessor.saveEditedBitmap(
                                    context = context,
                                    sourceUri = item.uri,
                                    adjustments = currentAdjustments,
                                    baseName = item.displayName
                                )
                                isSaving = false
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Saved copy to Salim library", Toast.LENGTH_SHORT).show()
                                    onSaveSuccess(result.getOrThrow())
                                } else {
                                    Toast.makeText(context, "Save failed: ${result.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isSaving && !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = SalimBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("editor_save_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Save Copy", fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Preview Canvas Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isComparingOriginal = true
                                tryAwaitRelease()
                                isComparingOriginal = false
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = SalimBlue)
                } else {
                    val bmpToDraw = if (isComparingOriginal) originalBitmap else previewBitmap
                    bmpToDraw?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Edited Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (isComparingOriginal) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("ORIGINAL", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            text = "Press and hold image to compare original",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp)
                        )
                    }
                }
            }

            // Controls Panel
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    // Tool Details Row
                    when (activeTab) {
                        EditorTab.ADJUST -> {
                            // Sub-chips for brightness, contrast, saturation, warmth, vignette
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            ) {
                                val adjustOptions = listOf(
                                    "brightness" to "Brightness",
                                    "contrast" to "Contrast",
                                    "saturation" to "Saturation",
                                    "warmth" to "Warmth",
                                    "vignette" to "Vignette"
                                )
                                items(adjustOptions) { (key, label) ->
                                    FilterChip(
                                        selected = activeAdjustMode == key,
                                        onClick = { activeAdjustMode = key },
                                        label = { Text(label, fontSize = 12.sp) },
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.testTag("adjust_chip_$key")
                                    )
                                }
                            }

                            // Slider for active adjustment
                            when (activeAdjustMode) {
                                "brightness" -> {
                                    SliderControl(
                                        label = "Brightness",
                                        value = currentAdjustments.brightness,
                                        range = -80f..80f,
                                        onValueChange = {
                                            currentAdjustments = currentAdjustments.copy(brightness = it)
                                        },
                                        onValueChangeFinished = {
                                            applyChange(currentAdjustments)
                                        }
                                    )
                                }
                                "contrast" -> {
                                    SliderControl(
                                        label = "Contrast",
                                        value = currentAdjustments.contrast,
                                        range = 0.5f..1.8f,
                                        onValueChange = {
                                            currentAdjustments = currentAdjustments.copy(contrast = it)
                                        },
                                        onValueChangeFinished = {
                                            applyChange(currentAdjustments)
                                        }
                                    )
                                }
                                "saturation" -> {
                                    SliderControl(
                                        label = "Saturation",
                                        value = currentAdjustments.saturation,
                                        range = 0f..2f,
                                        onValueChange = {
                                            currentAdjustments = currentAdjustments.copy(saturation = it)
                                        },
                                        onValueChangeFinished = {
                                            applyChange(currentAdjustments)
                                        }
                                    )
                                }
                                "warmth" -> {
                                    SliderControl(
                                        label = "Warmth",
                                        value = currentAdjustments.warmth,
                                        range = -40f..40f,
                                        onValueChange = {
                                            currentAdjustments = currentAdjustments.copy(warmth = it)
                                        },
                                        onValueChangeFinished = {
                                            applyChange(currentAdjustments)
                                        }
                                    )
                                }
                                "vignette" -> {
                                    SliderControl(
                                        label = "Vignette",
                                        value = currentAdjustments.vignette,
                                        range = 0f..100f,
                                        onValueChange = {
                                            currentAdjustments = currentAdjustments.copy(vignette = it)
                                        },
                                        onValueChangeFinished = {
                                            applyChange(currentAdjustments)
                                        }
                                    )
                                }
                            }
                        }

                        EditorTab.CROP -> {
                            // Aspect Ratio Presets
                            Text(
                                text = "Aspect Ratio",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            ) {
                                items(AspectRatioPreset.values()) { preset ->
                                    Button(
                                        onClick = {
                                            val rect = when (preset) {
                                                AspectRatioPreset.ORIGINAL, AspectRatioPreset.FREE -> null
                                                AspectRatioPreset.SQUARE -> RectF(0.125f, 0.125f, 0.875f, 0.875f)
                                                AspectRatioPreset.RATIO_4_3 -> RectF(0.05f, 0.15f, 0.95f, 0.85f)
                                                AspectRatioPreset.RATIO_3_4 -> RectF(0.15f, 0.05f, 0.85f, 0.95f)
                                                AspectRatioPreset.RATIO_16_9 -> RectF(0.02f, 0.2f, 0.98f, 0.8f)
                                                AspectRatioPreset.RATIO_9_16 -> RectF(0.2f, 0.02f, 0.8f, 0.98f)
                                            }
                                            applyChange(currentAdjustments.copy(cropRectNormalized = rect))
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("crop_preset_${preset.label}")
                                    ) {
                                        Text(preset.label, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        EditorTab.ROTATE -> {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = {
                                        val newRot = (currentAdjustments.rotationAngle - 90 + 360) % 360
                                        applyChange(currentAdjustments.copy(rotationAngle = newRot))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.testTag("rotate_left_button")
                                ) {
                                    Icon(Icons.Default.RotateLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Rotate -90°", color = MaterialTheme.colorScheme.onSurface)
                                }

                                Button(
                                    onClick = {
                                        val newRot = (currentAdjustments.rotationAngle + 90) % 360
                                        applyChange(currentAdjustments.copy(rotationAngle = newRot))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.testTag("rotate_right_button")
                                ) {
                                    Icon(Icons.Default.RotateRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Rotate +90°", color = MaterialTheme.colorScheme.onSurface)
                                }

                                Button(
                                    onClick = {
                                        applyChange(currentAdjustments.copy(flipHorizontal = !currentAdjustments.flipHorizontal))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.testTag("flip_horizontal_button")
                                ) {
                                    Icon(Icons.Default.Flip, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Flip H", color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 6.dp))

                    // Primary Tabs: Crop, Adjust, Rotate
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        EditorTabButton(
                            label = "Adjust",
                            icon = Icons.Default.Tune,
                            isSelected = activeTab == EditorTab.ADJUST,
                            onClick = { activeTab = EditorTab.ADJUST }
                        )

                        EditorTabButton(
                            label = "Crop",
                            icon = Icons.Default.Crop,
                            isSelected = activeTab == EditorTab.CROP,
                            onClick = { activeTab = EditorTab.CROP }
                        )

                        EditorTabButton(
                            label = "Rotate",
                            icon = Icons.Default.Rotate90DegreesCw,
                            isSelected = activeTab == EditorTab.ROTATE,
                            onClick = { activeTab = EditorTab.ROTATE }
                        )

                        EditorTabButton(
                            label = "Reset",
                            icon = Icons.Default.RestartAlt,
                            isSelected = false,
                            onClick = {
                                applyChange(EditAdjustments())
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SliderControl(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(String.format("%.1f", value), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = SalimBlue,
                activeTrackColor = SalimBlue
            ),
            modifier = Modifier.testTag("slider_${label.lowercase()}")
        )
    }
}

@Composable
fun EditorTabButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("editor_tab_${label.lowercase()}")
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) SalimBlue else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (isSelected) SalimBlue else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
