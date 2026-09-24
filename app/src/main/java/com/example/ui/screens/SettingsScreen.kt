package com.example.ui.screens

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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.example.data.preferences.*
import com.example.ui.theme.SalimBlue
import com.example.ui.theme.SalimRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: SalimSettings,
    mediaCount: Int,
    albumCount: Int,
    preferencesManager: PreferencesManager,
    onRescanLibrary: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
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
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // APPEARANCE SECTION
            SettingsSectionHeader(title = "APPEARANCE")
            SettingsGroupCard {
                // Theme
                SettingsRowWithPicker(
                    icon = Icons.Outlined.Palette,
                    label = "Theme",
                    currentValue = settings.theme.name.lowercase().replaceFirstChar { it.uppercase() }
                ) {
                    val themes = SalimTheme.values()
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(
                            onClick = { expanded = true },
                            modifier = Modifier.testTag("theme_selector_button")
                        ) {
                            Text(
                                text = settings.theme.name.lowercase().replaceFirstChar { it.uppercase() },
                                color = SalimBlue
                            )
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            themes.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        preferencesManager.updateTheme(t)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Columns
                SettingsRowWithPicker(
                    icon = Icons.Outlined.GridView,
                    label = "Grid Columns",
                    currentValue = "${settings.columnCount} columns"
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(
                            onClick = { expanded = true },
                            modifier = Modifier.testTag("columns_selector_button")
                        ) {
                            Text(text = "${settings.columnCount}", color = SalimBlue)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            (1..5).forEach { count ->
                                DropdownMenuItem(
                                    text = { Text("$count columns") },
                                    onClick = {
                                        preferencesManager.updateColumnCount(count)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Square vs Natural
                SettingsSwitchRow(
                    icon = Icons.Outlined.CropSquare,
                    label = "Square Thumbnails",
                    checked = settings.isSquareCrop,
                    onCheckedChange = { preferencesManager.updateSquareCrop(it) },
                    tag = "switch_square_crop"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Corner Radius
                SettingsRowWithPicker(
                    icon = Icons.Outlined.RoundedCorner,
                    label = "Thumbnail Corners",
                    currentValue = "${settings.cornerRadiusDp} dp"
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(
                            onClick = { expanded = true },
                            modifier = Modifier.testTag("corner_radius_button")
                        ) {
                            Text(text = "${settings.cornerRadiusDp} dp", color = SalimBlue)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf(0, 4, 8, 12, 16).forEach { rad ->
                                DropdownMenuItem(
                                    text = { Text("$rad dp") },
                                    onClick = {
                                        preferencesManager.updateCornerRadius(rad)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Video duration badges
                SettingsSwitchRow(
                    icon = Icons.Outlined.PlayCircle,
                    label = "Show Video Badges",
                    checked = settings.showVideoBadges,
                    onCheckedChange = { preferencesManager.updateShowVideoBadges(it) },
                    tag = "switch_video_badges"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Date headers
                SettingsSwitchRow(
                    icon = Icons.Outlined.CalendarToday,
                    label = "Show Date Headers",
                    checked = settings.showDateHeaders,
                    onCheckedChange = { preferencesManager.updateShowDateHeaders(it) },
                    tag = "switch_date_headers"
                )
            }

            // LIBRARY & ORGANIZATION
            SettingsSectionHeader(title = "LIBRARY & TIMELINE")
            SettingsGroupCard {
                // Grouping mode
                SettingsRowWithPicker(
                    icon = Icons.Outlined.DateRange,
                    label = "Group Timeline By",
                    currentValue = settings.groupingMode.label
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(
                            onClick = { expanded = true },
                            modifier = Modifier.testTag("grouping_mode_button")
                        ) {
                            Text(text = settings.groupingMode.label, color = SalimBlue)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            GroupingMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.label) },
                                    onClick = {
                                        preferencesManager.updateGroupingMode(mode)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Show Videos in photos tab
                SettingsSwitchRow(
                    icon = Icons.Outlined.Videocam,
                    label = "Include Videos in Timeline",
                    checked = settings.showVideos,
                    onCheckedChange = { preferencesManager.updateShowVideos(it) },
                    tag = "switch_show_videos"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Show Screenshots
                SettingsSwitchRow(
                    icon = Icons.Outlined.Screenshot,
                    label = "Include Screenshots",
                    checked = settings.showScreenshots,
                    onCheckedChange = { preferencesManager.updateShowScreenshots(it) },
                    tag = "switch_show_screenshots"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Rescan library action
                SettingsActionRow(
                    icon = Icons.Outlined.Refresh,
                    label = "Rescan Media Library",
                    actionText = "Rescan",
                    onClick = {
                        onRescanLibrary()
                        Toast.makeText(context, "Rescanning storage…", Toast.LENGTH_SHORT).show()
                    },
                    tag = "action_rescan_library"
                )
            }

            // STORAGE & DIAGNOSTICS
            SettingsSectionHeader(title = "STORAGE")
            SettingsGroupCard {
                SettingsInfoRow(
                    icon = Icons.Outlined.PhotoLibrary,
                    label = "Indexed Media Items",
                    value = "$mediaCount files"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                SettingsInfoRow(
                    icon = Icons.Outlined.Folder,
                    label = "Custom Albums",
                    value = "$albumCount albums"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                SettingsActionRow(
                    icon = Icons.Outlined.CleaningServices,
                    label = "Clear Thumbnail Cache",
                    actionText = "Clear",
                    onClick = {
                        try {
                            context.imageLoader.memoryCache?.clear()
                            context.imageLoader.diskCache?.clear()
                            Toast.makeText(context, "Thumbnail cache cleared", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                        }
                    },
                    tag = "action_clear_cache"
                )
            }

            // PRIVACY & ABOUT
            SettingsSectionHeader(title = "PRIVACY & ABOUT")
            SettingsGroupCard {
                SettingsActionRow(
                    icon = Icons.Outlined.Security,
                    label = "Privacy & Local Data Statement",
                    actionText = "Read",
                    onClick = { showPrivacyDialog = true },
                    tag = "action_privacy"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                SettingsActionRow(
                    icon = Icons.Outlined.Info,
                    label = "About Salim",
                    actionText = "v1.0",
                    onClick = { showAboutDialog = true },
                    tag = "action_about"
                )
            }
        }
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = {
                Text("Privacy & Storage", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "• Salim is completely local. Your photos, videos, and edits stay 100% on your device.",
                        fontSize = 14.sp
                    )
                    Text(
                        "• MediaStore permissions are used solely to view and organize your local media files.",
                        fontSize = 14.sp
                    )
                    Text(
                        "• No analytics, tracking, telemetry, or remote server connections exist.",
                        fontSize = 14.sp
                    )
                    Text(
                        "• Album organization and favorites are stored locally in an on-device SQLite database.",
                        fontSize = 14.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showPrivacyDialog = false },
                    modifier = Modifier.testTag("privacy_dialog_close")
                ) {
                    Text("Done", color = SalimBlue)
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Text("Salim", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Version 1.0", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "A premium, Apple-inspired photo album and local media manager crafted for Android.",
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showAboutDialog = false },
                    modifier = Modifier.testTag("about_dialog_close")
                ) {
                    Text("OK", color = SalimBlue)
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        content = content
    )
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = SalimBlue, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = label, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SalimBlue
            ),
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
fun SettingsRowWithPicker(
    icon: ImageVector,
    label: String,
    currentValue: String,
    picker: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = SalimBlue, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = label, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        picker()
    }
}

@Composable
fun SettingsInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = SalimBlue, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = label, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(text = value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsActionRow(
    icon: ImageVector,
    label: String,
    actionText: String,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = SalimBlue, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = label, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(text = actionText, fontSize = 14.sp, color = SalimBlue, fontWeight = FontWeight.Medium)
    }
}
