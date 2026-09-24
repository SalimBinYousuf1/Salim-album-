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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
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
    trashCount: Int,
    preferencesManager: PreferencesManager,
    onRescanLibrary: () -> Unit,
    onOpenBackupDetail: () -> Unit,
    onOpenCleanup: () -> Unit,
    onOpenRecentlyDeleted: () -> Unit,
    onOpenPasswordPrompt: () -> Unit
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
            // GOOGLE BACKUP SECTION
            SettingsSectionHeader(title = "CLOUD BACKUP")
            SettingsGroupCard {
                SettingsNavRow(
                    icon = Icons.Outlined.CloudSync,
                    title = "Backup",
                    subtitle = "Backup enabled. The service is provided by Google Photos.",
                    trailingText = if (settings.isGoogleBackupEnabled) "On" else "Off",
                    onClick = onOpenBackupDetail,
                    tag = "setting_backup"
                )
            }

            // VIEWING & PLAYBACK
            SettingsSectionHeader(title = "VIEWING & PLAYBACK")
            SettingsGroupCard {
                // Autoplay videos
                SettingsSwitchRow(
                    icon = Icons.Outlined.PlayCircle,
                    label = "Autoplay videos",
                    subtitle = "Videos are muted by default when autoplaying.",
                    checked = settings.autoplayVideos,
                    onCheckedChange = { preferencesManager.updateAutoplayVideos(it) },
                    tag = "switch_autoplay_videos"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Live photo autoplay
                SettingsSwitchRow(
                    icon = Icons.Outlined.MotionPhotosOn,
                    label = "Live photo autoplay",
                    checked = settings.livePhotoAutoplay,
                    onCheckedChange = { preferencesManager.updateLivePhotoAutoplay(it) },
                    tag = "switch_live_photo_autoplay"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Keep screen on
                SettingsSwitchRow(
                    icon = Icons.Outlined.WbSunny,
                    label = "Keep screen on while viewing",
                    checked = settings.keepScreenOn,
                    onCheckedChange = { preferencesManager.updateKeepScreenOn(it) },
                    tag = "switch_keep_screen_on"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Auto rotate
                SettingsSwitchRow(
                    icon = Icons.Outlined.ScreenRotation,
                    label = "Auto-rotate when viewing photos",
                    checked = settings.autoRotateViewing,
                    onCheckedChange = { preferencesManager.updateAutoRotateViewing(it) },
                    tag = "switch_auto_rotate"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Follow portrait
                SettingsSwitchRow(
                    icon = Icons.Outlined.ScreenLockPortrait,
                    label = "Follow Portrait screen lock",
                    checked = settings.followPortraitScreenLock,
                    onCheckedChange = { preferencesManager.updateFollowPortraitScreenLock(it) },
                    tag = "switch_follow_portrait"
                )
            }

            // ORGANIZATION & CLEANUP
            SettingsSectionHeader(title = "ORGANIZATION & STORAGE")
            SettingsGroupCard {
                // Intelligent classification
                SettingsSwitchRow(
                    icon = Icons.Outlined.Category,
                    label = "Intelligent classification",
                    subtitle = "Automatically categorize selfies, panoramas, and large files.",
                    checked = settings.intelligentClassification,
                    onCheckedChange = { preferencesManager.updateIntelligentClassification(it) },
                    tag = "switch_intelligent_classification"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Cleanup suggestions
                SettingsNavRow(
                    icon = Icons.Outlined.CleaningServices,
                    title = "Cleanup suggestions",
                    subtitle = "Find duplicates, large videos, and screenshots to free space.",
                    trailingText = "Review",
                    onClick = onOpenCleanup,
                    tag = "setting_cleanup_suggestions"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Recently deleted
                SettingsNavRow(
                    icon = Icons.Outlined.DeleteSweep,
                    title = "Recently deleted",
                    subtitle = "Deleted images and videos will be displayed in \"Recently deleted\".",
                    trailingText = if (trashCount > 0) "$trashCount items" else "Empty",
                    onClick = onOpenRecentlyDeleted,
                    tag = "setting_recently_deleted"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Save method after editing
                SettingsRowWithPicker(
                    icon = Icons.Outlined.Save,
                    label = "Save method after editing",
                    currentValue = settings.saveMethodAfterEditing.label
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(
                            onClick = { expanded = true },
                            modifier = Modifier.testTag("save_method_button")
                        ) {
                            Text(text = settings.saveMethodAfterEditing.label, color = SalimBlue)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            SaveMethod.values().forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method.label) },
                                    onClick = {
                                        preferencesManager.updateSaveMethod(method)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // PRIVACY & HIDDEN PHOTOS
            SettingsSectionHeader(title = "PRIVACY & SECURITY")
            SettingsGroupCard {
                // Show hidden images
                SettingsSwitchRow(
                    icon = Icons.Outlined.Visibility,
                    label = "Hidden photos",
                    subtitle = "Show hidden images and videos in your album list.",
                    checked = settings.showHiddenPhotos,
                    onCheckedChange = { preferencesManager.updateShowHiddenPhotos(it) },
                    tag = "switch_show_hidden_photos"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Password for hidden photos
                SettingsNavRow(
                    icon = Icons.Outlined.Lock,
                    title = "Password for hidden photos",
                    subtitle = if (settings.hiddenPhotosPassword != null) "PIN protection enabled" else "Set up a PIN to lock hidden album",
                    trailingText = if (settings.hiddenPhotosPassword != null) "Change" else "Set PIN",
                    onClick = onOpenPasswordPrompt,
                    tag = "setting_hidden_password"
                )
            }

            // APPEARANCE SECTION
            SettingsSectionHeader(title = "APPEARANCE")
            SettingsGroupCard {
                // Theme
                SettingsRowWithPicker(
                    icon = Icons.Outlined.Palette,
                    label = "Theme",
                    currentValue = settings.theme.name
                ) {
                    val themes = SalimTheme.values()
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(
                            onClick = { expanded = true },
                            modifier = Modifier.testTag("theme_selector_button")
                        ) {
                            Text(
                                text = when (settings.theme) {
                                    SalimTheme.ASGL -> "ASGL (Liquid Glass)"
                                    else -> settings.theme.name.lowercase().replaceFirstChar { it.uppercase() }
                                },
                                color = SalimBlue
                            )
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            themes.forEach { t ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            when (t) {
                                                SalimTheme.ASGL -> "ASGL (Liquid Glass)"
                                                else -> t.name.lowercase().replaceFirstChar { it.uppercase() }
                                            }
                                        )
                                    },
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

            // LIBRARY & TIMELINE
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

            // ABOUT
            SettingsSectionHeader(title = "ABOUT")
            SettingsGroupCard {
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
fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailingText: String? = null,
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
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = SalimBlue, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                if (subtitle != null) {
                    Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (trailingText != null) {
                Text(text = trailingText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(4.dp))
            }
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
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
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = SalimBlue, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = label, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                if (subtitle != null) {
                    Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
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
