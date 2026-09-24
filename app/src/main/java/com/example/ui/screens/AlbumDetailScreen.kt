package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlbumType
import com.example.data.model.MediaItem
import com.example.data.model.SalimAlbum
import com.example.data.preferences.SalimSettings
import com.example.ui.components.MediaThumbnail
import com.example.ui.theme.SalimBlue
import com.example.ui.theme.SalimRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    album: SalimAlbum,
    media: List<MediaItem>,
    settings: SalimSettings,
    isSelectionMode: Boolean,
    selectedIds: Set<Long>,
    onBack: () -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    onMediaLongClick: (MediaItem) -> Unit,
    onStartSelection: () -> Unit,
    onRenameAlbum: (() -> Unit)?,
    onDeleteAlbum: (() -> Unit)?
) {
    var showMoreMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (!isSelectionMode) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = album.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${media.size} items",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("album_detail_back_button")
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (media.isNotEmpty()) {
                            TextButton(
                                onClick = onStartSelection,
                                modifier = Modifier.testTag("album_select_button")
                            ) {
                                Text(
                                    text = "Select",
                                    color = SalimBlue,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (album.type == AlbumType.USER_CREATED) {
                            Box {
                                IconButton(
                                    onClick = { showMoreMenu = true },
                                    modifier = Modifier.testTag("album_more_options_button")
                                ) {
                                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Album Options")
                                }
                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false }
                                ) {
                                    if (onRenameAlbum != null) {
                                        DropdownMenuItem(
                                            text = { Text("Rename Album") },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                            onClick = {
                                                showMoreMenu = false
                                                onRenameAlbum()
                                            }
                                        )
                                    }
                                    if (onDeleteAlbum != null) {
                                        DropdownMenuItem(
                                            text = { Text("Delete Album", color = SalimRed) },
                                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = SalimRed) },
                                            onClick = {
                                                showMoreMenu = false
                                                onDeleteAlbum()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (media.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "This Album is Empty",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Add photos or videos to see them here",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(settings.columnCount),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .testTag("album_detail_grid"),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
            ) {
                items(
                    items = media,
                    key = { it.id }
                ) { item ->
                    MediaThumbnail(
                        item = item,
                        isSelected = selectedIds.contains(item.id),
                        isSelectionMode = isSelectionMode,
                        isSquare = settings.isSquareCrop,
                        cornerRadius = settings.cornerRadiusDp,
                        showVideoBadge = settings.showVideoBadges,
                        onClick = { onMediaClick(item) },
                        onLongClick = { onMediaLongClick(item) }
                    )
                }
            }
        }
    }
}
