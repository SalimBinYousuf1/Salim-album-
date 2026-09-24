package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.MediaItem
import com.example.ui.theme.SalimBlue
import com.example.ui.theme.SalimRed
import com.example.ui.viewmodel.CleanupCandidates

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanupScreen(
    candidates: CleanupCandidates,
    onBack: () -> Unit,
    onCleanItems: (List<MediaItem>) -> Unit
) {
    var selectedItems by remember { mutableStateOf(setOf<Long>()) }

    val formattedCleanable = remember(candidates.totalCleanableBytes) {
        val mb = candidates.totalCleanableBytes / (1024.0 * 1024.0)
        val gb = mb / 1024.0
        if (gb >= 1.0) String.format("%.2f GB", gb) else String.format("%.1f MB", mb)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Cleanup Suggestions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("cleanup_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            if (selectedItems.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedItems.size} items selected",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Button(
                            onClick = {
                                val allList = candidates.duplicates + candidates.largeFiles + candidates.oldScreenshots
                                val toDelete = allList.filter { selectedItems.contains(it.id) }.distinctBy { it.id }
                                onCleanItems(toDelete)
                                selectedItems = emptySet()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SalimRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("cleanup_action_button")
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Move to Trash")
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header summary banner
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.CleaningServices,
                                contentDescription = null,
                                tint = SalimBlue,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "$formattedCleanable Available to Clean",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Review duplicate candidates, large video files, and screenshots to free space.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Duplicate Candidates
            if (candidates.duplicates.isNotEmpty()) {
                item {
                    CleanupSection(
                        title = "Duplicate Candidates",
                        count = candidates.duplicates.size,
                        items = candidates.duplicates,
                        selectedIds = selectedItems,
                        onToggle = { id ->
                            selectedItems = if (selectedItems.contains(id)) selectedItems - id else selectedItems + id
                        },
                        onSelectAll = {
                            selectedItems = selectedItems + candidates.duplicates.map { it.id }.toSet()
                        }
                    )
                }
            }

            // Large Files (>50MB)
            if (candidates.largeFiles.isNotEmpty()) {
                item {
                    CleanupSection(
                        title = "Large Videos & Files (>50 MB)",
                        count = candidates.largeFiles.size,
                        items = candidates.largeFiles,
                        selectedIds = selectedItems,
                        onToggle = { id ->
                            selectedItems = if (selectedItems.contains(id)) selectedItems - id else selectedItems + id
                        },
                        onSelectAll = {
                            selectedItems = selectedItems + candidates.largeFiles.map { it.id }.toSet()
                        }
                    )
                }
            }

            // Screenshots
            if (candidates.oldScreenshots.isNotEmpty()) {
                item {
                    CleanupSection(
                        title = "Screenshots",
                        count = candidates.oldScreenshots.size,
                        items = candidates.oldScreenshots,
                        selectedIds = selectedItems,
                        onToggle = { id ->
                            selectedItems = if (selectedItems.contains(id)) selectedItems - id else selectedItems + id
                        },
                        onSelectAll = {
                            selectedItems = selectedItems + candidates.oldScreenshots.map { it.id }.toSet()
                        }
                    )
                }
            }

            if (candidates.duplicates.isEmpty() && candidates.largeFiles.isEmpty() && candidates.oldScreenshots.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SalimBlue,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Library is Optimized",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "No duplicates or unnecessary large files found.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CleanupSection(
    title: String,
    count: Int,
    items: List<MediaItem>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    onSelectAll: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$count items",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(onClick = onSelectAll) {
                Text("Select All", color = SalimBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items, key = { it.id }) { item ->
                val isSelected = selectedIds.contains(item.id)
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onToggle(item.id) }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.uri)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Selected overlay
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f))
                        )
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SalimBlue,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(20.dp)
                        )
                    }

                    // Size badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.formattedSize,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
