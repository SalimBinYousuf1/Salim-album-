package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SalimBlue

@Composable
fun BackupDetailDialog(
    isEnabled: Boolean,
    isWifiOnly: Boolean,
    totalItemsCount: Int,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleWifiOnly: (Boolean) -> Unit,
    onBackupNow: () -> Unit,
    onOpenGooglePhotos: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CloudDone,
                contentDescription = null,
                tint = SalimBlue,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Google Photos Backup",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Backup enabled. The service is provided by Google Photos.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Automatic Backup", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = onToggleEnabled,
                                colors = SwitchDefaults.colors(checkedThumbColor = androidx.compose.ui.graphics.Color.White, checkedTrackColor = SalimBlue)
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Outlined.Wifi, contentDescription = null, modifier = Modifier.size(18.dp), tint = SalimBlue)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Wi-Fi Only", fontSize = 15.sp)
                            }
                            Switch(
                                checked = isWifiOnly,
                                onCheckedChange = onToggleWifiOnly,
                                colors = SwitchDefaults.colors(checkedThumbColor = androidx.compose.ui.graphics.Color.White, checkedTrackColor = SalimBlue)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenGooglePhotos,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("open_google_photos_button")
                    ) {
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("App", fontSize = 13.sp)
                    }

                    Button(
                        onClick = onBackupNow,
                        colors = ButtonDefaults.buttonColors(containerColor = SalimBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("backup_now_button")
                    ) {
                        Icon(imageVector = Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync Now", fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("backup_dialog_done")
            ) {
                Text("Done", color = SalimBlue)
            }
        }
    )
}
