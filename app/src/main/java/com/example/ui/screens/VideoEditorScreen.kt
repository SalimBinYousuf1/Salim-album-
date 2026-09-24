package com.example.ui.screens

import android.net.Uri
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.MediaItem
import com.example.editor.VideoProcessor
import com.example.ui.theme.SalimBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditorScreen(
    item: MediaItem,
    onCancel: () -> Unit,
    onSaveSuccess: (Uri) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val totalDurationMs = if (item.duration > 0) item.duration else 10000L

    var startMs by remember { mutableLongStateOf(0L) }
    var endMs by remember { mutableLongStateOf(totalDurationMs) }
    var isMuted by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Edit Video",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.testTag("video_editor_cancel_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    // Capture still frame button
                    IconButton(
                        onClick = {
                            val currentPos = videoViewRef?.currentPosition?.toLong() ?: startMs
                            isProcessing = true
                            coroutineScope.launch {
                                val result = VideoProcessor.extractFrame(context, item.uri, currentPos, item.displayName)
                                isProcessing = false
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Frame saved to Photos!", Toast.LENGTH_SHORT).show()
                                    onSaveSuccess(result.getOrThrow())
                                } else {
                                    Toast.makeText(context, "Could not capture frame: ${result.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isProcessing,
                        modifier = Modifier.testTag("capture_frame_button")
                    ) {
                        Icon(imageVector = Icons.Outlined.CameraAlt, contentDescription = "Capture Frame")
                    }

                    // Save Trimmed Video
                    Button(
                        onClick = {
                            isProcessing = true
                            coroutineScope.launch {
                                val result = VideoProcessor.trimVideo(
                                    context = context,
                                    videoUri = item.uri,
                                    startMs = startMs,
                                    endMs = endMs,
                                    isMuted = isMuted,
                                    baseName = item.displayName
                                )
                                isProcessing = false
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Trimmed video saved!", Toast.LENGTH_SHORT).show()
                                    onSaveSuccess(result.getOrThrow())
                                } else {
                                    Toast.makeText(context, "Error saving video: ${result.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = SalimBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("video_save_button")
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Save", fontWeight = FontWeight.SemiBold)
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
            // Video Player Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoURI(item.uri)
                            val controller = MediaController(ctx)
                            controller.setAnchorView(this)
                            setMediaController(controller)
                            setOnPreparedListener { mp ->
                                mp.isLooping = true
                                start()
                            }
                            videoViewRef = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Trimming & Audio Panel
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Start time and End time indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Start: ${formatMs(startMs)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val trimmedDuration = (endMs - startMs).coerceAtLeast(0)
                        Text(
                            text = "Length: ${formatMs(trimmedDuration)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SalimBlue
                        )
                        Text(
                            text = "End: ${formatMs(endMs)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Trim Start Slider
                    Column {
                        Text("Trim Start", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(
                            value = startMs.toFloat(),
                            onValueChange = {
                                startMs = it.toLong().coerceAtMost(endMs - 500L)
                                videoViewRef?.seekTo(startMs.toInt())
                            },
                            valueRange = 0f..totalDurationMs.toFloat(),
                            colors = SliderDefaults.colors(thumbColor = SalimBlue, activeTrackColor = SalimBlue),
                            modifier = Modifier.testTag("trim_start_slider")
                        )
                    }

                    // Trim End Slider
                    Column {
                        Text("Trim End", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(
                            value = endMs.toFloat(),
                            onValueChange = {
                                endMs = it.toLong().coerceAtLeast(startMs + 500L)
                                videoViewRef?.seekTo(endMs.toInt())
                            },
                            valueRange = 0f..totalDurationMs.toFloat(),
                            colors = SliderDefaults.colors(thumbColor = SalimBlue, activeTrackColor = SalimBlue),
                            modifier = Modifier.testTag("trim_end_slider")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    // Audio Mute switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = SalimBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mute Video Audio", fontSize = 14.sp)
                        }
                        Switch(
                            checked = isMuted,
                            onCheckedChange = { isMuted = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SalimBlue),
                            modifier = Modifier.testTag("mute_audio_switch")
                        )
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = (ms % 1000) / 100
    return String.format("%d:%02d.%d", minutes, seconds, millis)
}
