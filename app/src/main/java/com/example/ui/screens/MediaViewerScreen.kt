package com.example.ui.screens

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.MediaItem
import com.example.ui.theme.SalimBlue
import com.example.ui.theme.SalimRed
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewerScreen(
    mediaList: List<MediaItem>,
    initialIndex: Int,
    onBack: () -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    onShare: (MediaItem) -> Unit,
    onEdit: (MediaItem) -> Unit,
    onDelete: (MediaItem) -> Unit,
    onShowDetails: (MediaItem) -> Unit,
    onSetWallpaper: (MediaItem) -> Unit,
    onSaveCopy: (MediaItem) -> Unit,
    onRename: (MediaItem) -> Unit,
    onAddToAlbum: (MediaItem) -> Unit,
    onOpenWith: (MediaItem) -> Unit
) {
    if (mediaList.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, mediaList.size - 1),
        pageCount = { mediaList.size }
    )

    val currentItem = mediaList.getOrNull(pagerState.currentPage) ?: mediaList.first()

    var showBars by remember { mutableStateOf(true) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showFilmstrip by remember { mutableStateOf(true) }

    val filmstripListState = rememberLazyListState()

    // Keep filmstrip centered on active item
    LaunchedEffect(pagerState.currentPage) {
        filmstripListState.animateScrollToItem(
            (pagerState.currentPage - 2).coerceAtLeast(0)
        )
    }

    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("media_viewer_screen")
    ) {
        // Pager for swipe left/right
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { mediaList[it].id }
        ) { page ->
            val item = mediaList[page]
            if (item.isVideo) {
                VideoPlayerView(
                    item = item,
                    onTap = { showBars = !showBars }
                )
            } else {
                ZoomableImageView(
                    item = item,
                    onTap = { showBars = !showBars }
                )
            }
        }

        // Top App Bar Overlay
        AnimatedVisibility(
            visible = showBars,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("viewer_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = dateFormatter.format(Date(currentItem.dateTaken)),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${pagerState.currentPage + 1} of ${mediaList.size}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }

                    // More Menu
                    Box {
                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier.testTag("viewer_more_options_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            if (!currentItem.isVideo) {
                                DropdownMenuItem(
                                    text = { Text("Set as Wallpaper") },
                                    leadingIcon = { Icon(Icons.Default.Wallpaper, contentDescription = null) },
                                    onClick = {
                                        showMoreMenu = false
                                        onSetWallpaper(currentItem)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Save a Copy") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    onSaveCopy(currentItem)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    onRename(currentItem)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Add to Album") },
                                leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    onAddToAlbum(currentItem)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Open With…") },
                                leadingIcon = { Icon(Icons.Default.OpenInNew, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenWith(currentItem)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Bottom Bar & Filmstrip Overlay
        AnimatedVisibility(
            visible = showBars,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .navigationBarsPadding()
            ) {
                // Bottom Filmstrip Carousel
                if (showFilmstrip && mediaList.size > 1) {
                    LazyRow(
                        state = filmstripListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(mediaList) { idx, item ->
                            val isCurrent = idx == pagerState.currentPage
                            Box(
                                modifier = Modifier
                                    .size(width = 44.dp, height = 44.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(
                                        width = if (isCurrent) 2.dp else 0.dp,
                                        color = if (isCurrent) SalimBlue else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(idx)
                                        }
                                    }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(item.uri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                // Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Share
                    IconButton(
                        onClick = { onShare(currentItem) },
                        modifier = Modifier.testTag("viewer_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White
                        )
                    }

                    // Favorite
                    IconButton(
                        onClick = { onToggleFavorite(currentItem) },
                        modifier = Modifier.testTag("viewer_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (currentItem.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (currentItem.isFavorite) SalimRed else Color.White
                        )
                    }

                    // Edit (only for photos)
                    if (!currentItem.isVideo) {
                        IconButton(
                            onClick = { onEdit(currentItem) },
                            modifier = Modifier.testTag("viewer_edit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Edit",
                                tint = Color.White
                            )
                        }
                    }

                    // Details / EXIF Info
                    IconButton(
                        onClick = { onShowDetails(currentItem) },
                        modifier = Modifier.testTag("viewer_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = Color.White
                        )
                    }

                    // Delete
                    IconButton(
                        onClick = { onDelete(currentItem) },
                        modifier = Modifier.testTag("viewer_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = SalimRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ZoomableImageView(
    item: MediaItem,
    onTap: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        scale = if (scale > 1.2f) 1f else 2.5f
                        offset = androidx.compose.ui.geometry.Offset.Zero
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale > 1f) {
                        offset += pan
                    } else {
                        offset = androidx.compose.ui.geometry.Offset.Zero
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.uri)
                .crossfade(true)
                .build(),
            contentDescription = item.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }
}

@Composable
fun VideoPlayerView(
    item: MediaItem,
    onTap: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoURI(item.uri)
                    val mediaController = MediaController(ctx)
                    mediaController.setAnchorView(this)
                    setMediaController(mediaController)
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
