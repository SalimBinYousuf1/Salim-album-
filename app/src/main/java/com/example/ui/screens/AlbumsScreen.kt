package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.data.model.AlbumType
import com.example.data.model.SalimAlbum
import com.example.ui.theme.SalimBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    albums: List<SalimAlbum>,
    onCreateAlbumClick: () -> Unit,
    onAlbumClick: (SalimAlbum) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Albums",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                },
                actions = {
                    IconButton(
                        onClick = onCreateAlbumClick,
                        modifier = Modifier.testTag("create_album_header_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Album",
                            tint = SalimBlue
                        )
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
        val userAndPrimaryAlbums = albums.filter {
            it.type == AlbumType.ALL || it.type == AlbumType.FAVORITES || it.type == AlbumType.USER_CREATED
        }
        val mediaTypeAlbums = albums.filter {
            it.type == AlbumType.VIDEOS || it.type == AlbumType.SCREENSHOTS || it.type == AlbumType.PANORAMAS ||
                    it.type == AlbumType.SELFIES || it.type == AlbumType.BURSTS || it.type == AlbumType.RAW
        }
        val utilityAlbums = albums.filter {
            it.type == AlbumType.RECENTLY_DELETED || it.type == AlbumType.HIDDEN
        }
        val folderAlbums = albums.filter {
            it.type == AlbumType.CAMERA || it.type == AlbumType.DOWNLOADS || it.type == AlbumType.LARGE_FILES || it.type == AlbumType.FOLDER
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("albums_grid"),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // My Albums Section
            if (userAndPrimaryAlbums.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = "My Albums",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }

                items(userAndPrimaryAlbums, key = { it.id }) { album ->
                    AlbumCard(album = album, onClick = { onAlbumClick(album) })
                }
            }

            // Media Types Section
            if (mediaTypeAlbums.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = "Media Types",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }

                items(mediaTypeAlbums, key = { it.id }) { album ->
                    AlbumCard(album = album, onClick = { onAlbumClick(album) })
                }
            }

            // Utilities Section (Recently Deleted, Hidden)
            if (utilityAlbums.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = "Utilities",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }

                items(utilityAlbums, key = { it.id }) { album ->
                    AlbumCard(album = album, onClick = { onAlbumClick(album) })
                }
            }

            // Folders Section
            if (folderAlbums.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = "Folders & Storage",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }

                items(folderAlbums, key = { it.id }) { album ->
                    AlbumCard(album = album, onClick = { onAlbumClick(album) })
                }
            }
        }
    }
}

@Composable
fun AlbumCard(
    album: SalimAlbum,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("album_card_${album.id}")
    ) {
        // Album Cover
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (album.coverUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(album.coverUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = album.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = when (album.type) {
                        AlbumType.VIDEOS -> Icons.Outlined.Videocam
                        AlbumType.FAVORITES -> Icons.Outlined.FavoriteBorder
                        AlbumType.RECENTLY_DELETED -> Icons.Outlined.Delete
                        AlbumType.HIDDEN -> Icons.Outlined.Lock
                        AlbumType.SCREENSHOTS -> Icons.Outlined.Screenshot
                        AlbumType.PANORAMAS -> Icons.Outlined.PanoramaHorizontal
                        else -> Icons.Outlined.PhotoAlbum
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Title
        Text(
            text = album.title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )

        // Count
        Text(
            text = "${album.count}",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
