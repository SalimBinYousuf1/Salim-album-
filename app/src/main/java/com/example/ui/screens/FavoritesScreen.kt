package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaItem
import com.example.data.preferences.SalimSettings
import com.example.ui.components.MediaThumbnail
import com.example.ui.theme.SalimBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    favorites: List<MediaItem>,
    settings: SalimSettings,
    isSelectionMode: Boolean,
    selectedIds: Set<Long>,
    onMediaClick: (MediaItem) -> Unit,
    onMediaLongClick: (MediaItem) -> Unit,
    onStartSelection: () -> Unit
) {
    Scaffold(
        topBar = {
            if (!isSelectionMode) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Favorites",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                            if (favorites.isNotEmpty()) {
                                Text(
                                    text = "${favorites.size} items",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        if (favorites.isNotEmpty()) {
                            TextButton(
                                onClick = onStartSelection,
                                modifier = Modifier.testTag("favorites_select_button")
                            ) {
                                Text(
                                    text = "Select",
                                    color = SalimBlue,
                                    fontWeight = FontWeight.SemiBold
                                )
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
        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Favorites Yet",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap the heart icon on any photo or video to add it to your favorites.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(settings.columnCount),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .testTag("favorites_grid"),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
            ) {
                items(
                    items = favorites,
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
