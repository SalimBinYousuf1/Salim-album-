package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
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
import com.example.data.model.MediaItem
import com.example.data.model.MediaSection
import com.example.data.preferences.SalimSettings
import com.example.data.preferences.SortOrder
import com.example.ui.components.MediaThumbnail
import com.example.ui.theme.SalimBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosScreen(
    sections: List<MediaSection>,
    allFilteredMedia: List<MediaItem>,
    isScanning: Boolean,
    settings: SalimSettings,
    isSelectionMode: Boolean,
    selectedIds: Set<Long>,
    onMediaClick: (MediaItem) -> Unit,
    onMediaLongClick: (MediaItem) -> Unit,
    onToggleSelectAll: () -> Unit,
    onStartSelection: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onRescan: () -> Unit,
    onChangeColumns: (Int) -> Unit,
    onChangeSort: (SortOrder) -> Unit,
    onSelectSection: (List<MediaItem>) -> Unit
) {
    var showDensityMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()

    Scaffold(
        topBar = {
            if (!isSelectionMode) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Salim",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                            if (allFilteredMedia.isNotEmpty()) {
                                Text(
                                    text = "${allFilteredMedia.size} items",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        // Quick column density toggle menu
                        Box {
                            IconButton(
                                onClick = { showDensityMenu = true },
                                modifier = Modifier.testTag("photos_density_button")
                            ) {
                                Icon(imageVector = Icons.Outlined.GridView, contentDescription = "Grid Density")
                            }
                            DropdownMenu(
                                expanded = showDensityMenu,
                                onDismissRequest = { showDensityMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("1 Column (Detail)") },
                                    onClick = { onChangeColumns(1); showDensityMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("2 Columns (Comfortable)") },
                                    onClick = { onChangeColumns(2); showDensityMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("3 Columns (Standard)") },
                                    onClick = { onChangeColumns(3); showDensityMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("4 Columns (Compact)") },
                                    onClick = { onChangeColumns(4); showDensityMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("5 Columns (Mini)") },
                                    onClick = { onChangeColumns(5); showDensityMenu = false }
                                )
                            }
                        }

                        // Sort order menu
                        Box {
                            IconButton(
                                onClick = { showSortMenu = true },
                                modifier = Modifier.testTag("photos_sort_button")
                            ) {
                                Icon(imageVector = Icons.Outlined.Sort, contentDescription = "Sort Media")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                SortOrder.values().forEach { order ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(order.label)
                                                if (settings.sortOrder == order) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = SalimBlue,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            onChangeSort(order)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Select action
                        TextButton(
                            onClick = onStartSelection,
                            modifier = Modifier.testTag("photos_select_button")
                        ) {
                            Text(
                                text = "Select",
                                color = SalimBlue,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isScanning && allFilteredMedia.isEmpty()) {
                // Scanning indicator
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = SalimBlue)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading media from device…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                }
            } else if (allFilteredMedia.isEmpty()) {
                // Real Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Photos or Videos",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Photos and videos on this device will automatically appear here in your timeline.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onRescan,
                        colors = ButtonDefaults.buttonColors(containerColor = SalimBlue),
                        modifier = Modifier.testTag("rescan_empty_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan Library")
                    }
                }
            } else {
                // Adaptive Media Grid with Section Headers
                LazyVerticalGrid(
                    columns = GridCells.Fixed(settings.columnCount),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("photos_grid"),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                ) {
                    for (section in sections) {
                        if (settings.showDateHeaders && section.title.isNotEmpty()) {
                            item(span = { GridItemSpan(settings.columnCount) }) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = section.title,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isSelectionMode) {
                                        TextButton(
                                            onClick = { onSelectSection(section.items) },
                                            modifier = Modifier.testTag("select_section_${section.title.hashCode()}")
                                        ) {
                                            val allSelected = section.items.all { selectedIds.contains(it.id) }
                                            Text(
                                                text = if (allSelected) "Deselect" else "Select",
                                                color = SalimBlue,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        items(
                            items = section.items,
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
    }
}
