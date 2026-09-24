package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExifData
import com.example.ui.theme.SalimBlue
import com.example.util.MediaActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailsSheet(
    exif: ExifData,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("media_details_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Info",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = {
                        val text = buildString {
                            appendLine("File: ${exif.fileName}")
                            appendLine("Date: ${exif.dateTaken}")
                            appendLine("Resolution: ${exif.resolution}")
                            appendLine("Size: ${exif.fileSize}")
                            if (exif.cameraModel.isNotEmpty()) appendLine("Camera: ${exif.cameraMake} ${exif.cameraModel}")
                            if (exif.iso.isNotEmpty()) appendLine("ISO: ${exif.iso}")
                            if (exif.aperture.isNotEmpty()) appendLine("Aperture: ${exif.aperture}")
                            if (exif.shutterSpeed.isNotEmpty()) appendLine("Shutter: ${exif.shutterSpeed}")
                            if (exif.focalLength.isNotEmpty()) appendLine("Focal Length: ${exif.focalLength}")
                            if (exif.latitude != null && exif.longitude != null) {
                                appendLine("GPS: ${exif.latitude}, ${exif.longitude}")
                            }
                        }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Media Metadata", text))
                        Toast.makeText(context, "Metadata copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("copy_metadata_button")
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Details", tint = SalimBlue)
                }
            }

            // File Information Card
            MetadataGroupCard(title = "FILE INFORMATION") {
                MetadataRow(icon = Icons.Default.InsertDriveFile, label = "File Name", value = exif.fileName)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                MetadataRow(icon = Icons.Default.Folder, label = "Album / Folder", value = exif.folder)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                MetadataRow(icon = Icons.Default.CalendarToday, label = "Date Taken", value = exif.dateTaken)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                MetadataRow(icon = Icons.Default.AspectRatio, label = "Dimensions", value = exif.resolution)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                MetadataRow(icon = Icons.Default.Storage, label = "File Size", value = exif.fileSize)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                MetadataRow(icon = Icons.Default.Extension, label = "Format", value = exif.format)
            }

            // Camera / Capture Card (if available)
            if (exif.cameraModel.isNotEmpty() || exif.iso.isNotEmpty() || exif.aperture.isNotEmpty()) {
                MetadataGroupCard(title = "CAMERA & EXIF") {
                    if (exif.cameraModel.isNotEmpty()) {
                        MetadataRow(
                            icon = Icons.Default.CameraAlt,
                            label = "Device",
                            value = "${exif.cameraMake} ${exif.cameraModel}".trim()
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                    if (exif.lens.isNotEmpty()) {
                        MetadataRow(icon = Icons.Default.Camera, label = "Lens", value = exif.lens)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                    if (exif.iso.isNotEmpty()) {
                        MetadataRow(icon = Icons.Default.Iso, label = "ISO", value = exif.iso)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                    if (exif.aperture.isNotEmpty()) {
                        MetadataRow(icon = Icons.Default.ShutterSpeed, label = "Aperture", value = exif.aperture)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                    if (exif.shutterSpeed.isNotEmpty()) {
                        MetadataRow(icon = Icons.Default.Timer, label = "Shutter Speed", value = exif.shutterSpeed)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                    if (exif.focalLength.isNotEmpty()) {
                        MetadataRow(icon = Icons.Default.Straighten, label = "Focal Length", value = exif.focalLength)
                    }
                }
            }

            // Location (if GPS coordinates exist)
            if (exif.latitude != null && exif.longitude != null) {
                MetadataGroupCard(title = "LOCATION") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${String.format("%.5f", exif.latitude)}°, ${String.format("%.5f", exif.longitude)}°",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Button(
                            onClick = {
                                MediaActions.openMapLocation(context, exif.latitude, exif.longitude, exif.fileName)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SalimBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("open_map_button")
                        ) {
                            Icon(imageVector = Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open in Maps")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataGroupCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            content = content
        )
    }
}

@Composable
private fun MetadataRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SalimBlue,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
