package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AlbumType
import com.example.data.model.ExifData
import com.example.data.model.MediaItem
import com.example.data.model.SalimAlbum
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.SalimTheme
import com.example.ui.viewmodel.GalleryViewModel
import com.example.util.MediaActions
import kotlinx.coroutines.launch

@Composable
fun SalimApp(
    viewModel: GalleryViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val allFilteredMedia by viewModel.filteredMedia.collectAsStateWithLifecycle()
    val mediaSections by viewModel.mediaSections.collectAsStateWithLifecycle()
    val albumsList by viewModel.albumsList.collectAsStateWithLifecycle()
    val userAlbums by viewModel.userAlbums.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()

    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchFilterType by viewModel.searchFilterType.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()

    // Navigation and screen state
    var currentTab by remember { mutableStateOf(SalimTab.PHOTOS) }
    var selectedAlbum by remember { mutableStateOf<SalimAlbum?>(null) }
    var viewingMediaItem by remember { mutableStateOf<MediaItem?>(null) }
    var editingMediaItem by remember { mutableStateOf<MediaItem?>(null) }
    var detailsExifData by remember { mutableStateOf<ExifData?>(null) }

    // Dialogs state
    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var showAddToAlbumDialog by remember { mutableStateOf(false) }
    var deleteConfirmItems by remember { mutableStateOf<List<MediaItem>?>(null) }
    var renameMediaTarget by remember { mutableStateOf<MediaItem?>(null) }
    var renameAlbumTarget by remember { mutableStateOf<SalimAlbum?>(null) }

    // Permission check
    fun checkPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasImages = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            val hasVideos = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            hasImages || hasVideos
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    var hasStoragePermission by remember { mutableStateOf(checkPermissions()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        hasStoragePermission = granted
        viewModel.setPermissionGranted(granted)
    }

    LaunchedEffect(Unit) {
        if (!hasStoragePermission) {
            val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            permissionLauncher.launch(perms)
        } else {
            viewModel.refreshLibrary()
        }
    }

    SalimTheme(selectedTheme = settings.theme) {
        if (!hasStoragePermission) {
            PermissionExplanationScreen(
                onRequestPermission = {
                    val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
                    } else {
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                    permissionLauncher.launch(perms)
                }
            )
            return@SalimTheme
        }

        // Handle Back Press
        BackHandler(
            enabled = editingMediaItem != null || viewingMediaItem != null || selectedAlbum != null || isSelectionMode
        ) {
            when {
                editingMediaItem != null -> editingMediaItem = null
                viewingMediaItem != null -> viewingMediaItem = null
                selectedAlbum != null -> selectedAlbum = null
                isSelectionMode -> viewModel.clearSelection()
            }
        }

        // Determine currently active media items list based on current context
        val activeMediaList = when {
            selectedAlbum != null -> {
                val album = selectedAlbum!!
                when (album.type) {
                    AlbumType.ALL -> allFilteredMedia
                    AlbumType.FAVORITES -> allFilteredMedia.filter { it.isFavorite }
                    AlbumType.VIDEOS -> allFilteredMedia.filter { it.isVideo }
                    AlbumType.SCREENSHOTS -> allFilteredMedia.filter { it.isScreenshot }
                    AlbumType.CAMERA -> allFilteredMedia.filter { it.bucketDisplayName.contains("camera", true) || it.bucketDisplayName.contains("dcim", true) }
                    AlbumType.DOWNLOADS -> allFilteredMedia.filter { it.bucketDisplayName.contains("download", true) }
                    AlbumType.PANORAMAS -> allFilteredMedia.filter { it.isPanorama }
                    AlbumType.LARGE_FILES -> allFilteredMedia.filter { it.isLargeFile }
                    AlbumType.RECENTLY_ADDED -> allFilteredMedia.take(100)
                    AlbumType.FOLDER -> allFilteredMedia.filter { it.bucketId == album.bucketId }
                    AlbumType.USER_CREATED -> {
                        val albumId = album.rawId ?: 0L
                        // We will filter from user-album media IDs
                        allFilteredMedia.filter { item ->
                            viewModel.selectedIds.value.isEmpty() // reactive list
                        }
                    }
                }
            }
            currentTab == SalimTab.FAVORITES -> allFilteredMedia.filter { it.isFavorite }
            currentTab == SalimTab.VIDEOS -> allFilteredMedia.filter { it.isVideo }
            currentTab == SalimTab.SEARCH -> searchResults
            else -> allFilteredMedia
        }

        val selectedItems = allFilteredMedia.filter { selectedIds.contains(it.id) }

        // Top-level Content
        Scaffold(
            topBar = {
                if (isSelectionMode && viewingMediaItem == null && editingMediaItem == null) {
                    SelectionTopBar(
                        selectedCount = selectedIds.size,
                        totalCount = activeMediaList.size,
                        onClose = { viewModel.clearSelection() },
                        onSelectAll = {
                            if (selectedIds.size == activeMediaList.size) {
                                viewModel.clearSelection()
                            } else {
                                viewModel.selectAll(activeMediaList)
                            }
                        },
                        onShare = {
                            MediaActions.shareMultipleMedia(context, selectedItems)
                        },
                        onAddToAlbum = {
                            showAddToAlbumDialog = true
                        },
                        onFavorite = {
                            val anyUnfav = selectedItems.any { !it.isFavorite }
                            viewModel.batchFavorite(anyUnfav)
                        },
                        onDelete = {
                            deleteConfirmItems = selectedItems
                        }
                    )
                }
            },
            bottomBar = {
                if (viewingMediaItem == null && editingMediaItem == null && !isSelectionMode) {
                    SalimBottomBar(
                        currentTab = currentTab,
                        onTabSelected = { tab ->
                            currentTab = tab
                            selectedAlbum = null
                        }
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
                if (selectedAlbum != null) {
                    // Album Detail Screen
                    val album = selectedAlbum!!
                    val albumMedia = when (album.type) {
                        AlbumType.ALL -> allFilteredMedia
                        AlbumType.FAVORITES -> allFilteredMedia.filter { it.isFavorite }
                        AlbumType.VIDEOS -> allFilteredMedia.filter { it.isVideo }
                        AlbumType.SCREENSHOTS -> allFilteredMedia.filter { it.isScreenshot }
                        AlbumType.CAMERA -> allFilteredMedia.filter { it.bucketDisplayName.contains("camera", true) || it.bucketDisplayName.contains("dcim", true) }
                        AlbumType.DOWNLOADS -> allFilteredMedia.filter { it.bucketDisplayName.contains("download", true) }
                        AlbumType.PANORAMAS -> allFilteredMedia.filter { it.isPanorama }
                        AlbumType.LARGE_FILES -> allFilteredMedia.filter { it.isLargeFile }
                        AlbumType.RECENTLY_ADDED -> allFilteredMedia.take(100)
                        AlbumType.FOLDER -> allFilteredMedia.filter { it.bucketId == album.bucketId }
                        AlbumType.USER_CREATED -> {
                            var items by remember(album.id, allFilteredMedia) { mutableStateOf<List<MediaItem>>(emptyList()) }
                            LaunchedEffect(album.id, allFilteredMedia) {
                                album.rawId?.let { rId ->
                                    items = viewModel.getMediaForUserAlbum(rId)
                                }
                            }
                            items
                        }
                    }

                    AlbumDetailScreen(
                        album = album,
                        media = albumMedia,
                        settings = settings,
                        isSelectionMode = isSelectionMode,
                        selectedIds = selectedIds,
                        onBack = { selectedAlbum = null },
                        onMediaClick = { item ->
                            if (isSelectionMode) {
                                viewModel.toggleSelection(item.id)
                            } else {
                                viewModel.recordView(item)
                                viewingMediaItem = item
                            }
                        },
                        onMediaLongClick = { item ->
                            viewModel.startSelection(item.id)
                        },
                        onStartSelection = { viewModel.startSelection() },
                        onRenameAlbum = if (album.type == AlbumType.USER_CREATED) {
                            { renameAlbumTarget = album }
                        } else null,
                        onDeleteAlbum = if (album.type == AlbumType.USER_CREATED) {
                            {
                                album.rawId?.let { viewModel.deleteAlbum(it) }
                                selectedAlbum = null
                                Toast.makeText(context, "Album deleted", Toast.LENGTH_SHORT).show()
                            }
                        } else null
                    )
                } else {
                    // Primary Tabs
                    when (currentTab) {
                        SalimTab.PHOTOS -> {
                            PhotosScreen(
                                sections = mediaSections,
                                allFilteredMedia = allFilteredMedia,
                                isScanning = isScanning,
                                settings = settings,
                                isSelectionMode = isSelectionMode,
                                selectedIds = selectedIds,
                                onMediaClick = { item ->
                                    if (isSelectionMode) {
                                        viewModel.toggleSelection(item.id)
                                    } else {
                                        viewModel.recordView(item)
                                        viewingMediaItem = item
                                    }
                                },
                                onMediaLongClick = { item ->
                                    viewModel.startSelection(item.id)
                                },
                                onToggleSelectAll = {
                                    if (selectedIds.size == allFilteredMedia.size) {
                                        viewModel.clearSelection()
                                    } else {
                                        viewModel.selectAll(allFilteredMedia)
                                    }
                                },
                                onStartSelection = { viewModel.startSelection() },
                                onOpenSearch = { currentTab = SalimTab.SEARCH },
                                onOpenSettings = { currentTab = SalimTab.SETTINGS },
                                onRescan = { viewModel.refreshLibrary() },
                                onChangeColumns = { viewModel.preferencesManager.updateColumnCount(it) },
                                onChangeSort = { viewModel.preferencesManager.updateSortOrder(it) },
                                onSelectSection = { sectionItems -> viewModel.selectSection(sectionItems) }
                            )
                        }

                        SalimTab.ALBUMS -> {
                            AlbumsScreen(
                                albums = albumsList,
                                onCreateAlbumClick = { showCreateAlbumDialog = true },
                                onAlbumClick = { album -> selectedAlbum = album }
                            )
                        }

                        SalimTab.FAVORITES -> {
                            FavoritesScreen(
                                favorites = allFilteredMedia.filter { it.isFavorite },
                                settings = settings,
                                isSelectionMode = isSelectionMode,
                                selectedIds = selectedIds,
                                onMediaClick = { item ->
                                    if (isSelectionMode) {
                                        viewModel.toggleSelection(item.id)
                                    } else {
                                        viewModel.recordView(item)
                                        viewingMediaItem = item
                                    }
                                },
                                onMediaLongClick = { item -> viewModel.startSelection(item.id) },
                                onStartSelection = { viewModel.startSelection() }
                            )
                        }

                        SalimTab.VIDEOS -> {
                            VideosScreen(
                                videos = allFilteredMedia.filter { it.isVideo },
                                settings = settings,
                                isSelectionMode = isSelectionMode,
                                selectedIds = selectedIds,
                                onMediaClick = { item ->
                                    if (isSelectionMode) {
                                        viewModel.toggleSelection(item.id)
                                    } else {
                                        viewModel.recordView(item)
                                        viewingMediaItem = item
                                    }
                                },
                                onMediaLongClick = { item -> viewModel.startSelection(item.id) },
                                onStartSelection = { viewModel.startSelection() }
                            )
                        }

                        SalimTab.SEARCH -> {
                            SearchScreen(
                                query = searchQuery,
                                filterType = searchFilterType,
                                results = searchResults,
                                settings = settings,
                                onQueryChange = { viewModel.setSearchQuery(it) },
                                onFilterChange = { viewModel.setSearchFilterType(it) },
                                onMediaClick = { item ->
                                    viewModel.recordView(item)
                                    viewingMediaItem = item
                                }
                            )
                        }

                        SalimTab.SETTINGS -> {
                            SettingsScreen(
                                settings = settings,
                                mediaCount = allFilteredMedia.size,
                                albumCount = userAlbums.size,
                                preferencesManager = viewModel.preferencesManager,
                                onRescanLibrary = { viewModel.refreshLibrary() }
                            )
                        }
                    }
                }
            }
        }

        // Full Screen Media Viewer Overlay
        if (viewingMediaItem != null) {
            val fullList = if (activeMediaList.isNotEmpty()) activeMediaList else allFilteredMedia
            val initialIdx = fullList.indexOfFirst { it.id == viewingMediaItem!!.id }.coerceAtLeast(0)

            MediaViewerScreen(
                mediaList = fullList,
                initialIndex = initialIdx,
                onBack = { viewingMediaItem = null },
                onToggleFavorite = { item -> viewModel.toggleFavorite(item) },
                onShare = { item -> MediaActions.shareSingleMedia(context, item) },
                onEdit = { item -> editingMediaItem = item },
                onDelete = { item -> deleteConfirmItems = listOf(item) },
                onShowDetails = { item ->
                    coroutineScope.launch {
                        val exif = viewModel.getExif(item)
                        detailsExifData = exif
                    }
                },
                onSetWallpaper = { item ->
                    coroutineScope.launch {
                        val success = MediaActions.setAsWallpaper(context, item)
                        if (success) {
                            Toast.makeText(context, "Wallpaper updated", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Could not set wallpaper", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onSaveCopy = { item ->
                    viewModel.copyMedia(item) { ok ->
                        if (ok) Toast.makeText(context, "Copy saved to Salim", Toast.LENGTH_SHORT).show()
                    }
                },
                onRename = { item -> renameMediaTarget = item },
                onAddToAlbum = { item ->
                    viewModel.startSelection(item.id)
                    showAddToAlbumDialog = true
                },
                onOpenWith = { item -> MediaActions.openWithOtherApp(context, item) }
            )
        }

        // Photo Editor Overlay
        if (editingMediaItem != null) {
            PhotoEditorScreen(
                item = editingMediaItem!!,
                onCancel = { editingMediaItem = null },
                onSaveSuccess = { _ ->
                    editingMediaItem = null
                    viewModel.refreshLibrary()
                }
            )
        }

        // Media Details Sheet
        if (detailsExifData != null) {
            MediaDetailsSheet(
                exif = detailsExifData!!,
                onDismiss = { detailsExifData = null }
            )
        }

        // Dialogs
        if (showCreateAlbumDialog) {
            CreateAlbumDialog(
                onDismiss = { showCreateAlbumDialog = false },
                onConfirm = { name ->
                    viewModel.createAlbum(name)
                    showCreateAlbumDialog = false
                    Toast.makeText(context, "Album created", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showAddToAlbumDialog) {
            AddToAlbumDialog(
                albums = userAlbums,
                onDismiss = { showAddToAlbumDialog = false },
                onSelectAlbum = { albumId ->
                    viewModel.addSelectedToAlbum(albumId)
                    showAddToAlbumDialog = false
                    Toast.makeText(context, "Added to album", Toast.LENGTH_SHORT).show()
                },
                onCreateNewAlbum = {
                    showAddToAlbumDialog = false
                    showCreateAlbumDialog = true
                }
            )
        }

        if (deleteConfirmItems != null) {
            val itemsToDelete = deleteConfirmItems!!
            DeleteConfirmDialog(
                count = itemsToDelete.size,
                onDismiss = { deleteConfirmItems = null },
                onConfirm = {
                    viewModel.deleteMedia(itemsToDelete) { success ->
                        deleteConfirmItems = null
                        if (viewingMediaItem != null && itemsToDelete.any { it.id == viewingMediaItem!!.id }) {
                            viewingMediaItem = null
                        }
                        if (success) {
                            Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "File delete completed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        if (renameMediaTarget != null) {
            val target = renameMediaTarget!!
            RenameDialog(
                currentName = target.displayName,
                onDismiss = { renameMediaTarget = null },
                onConfirm = { newName ->
                    viewModel.renameMedia(target, newName) { ok ->
                        renameMediaTarget = null
                        if (ok) {
                            Toast.makeText(context, "Renamed to $newName", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Could not rename file", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        if (renameAlbumTarget != null) {
            val target = renameAlbumTarget!!
            RenameDialog(
                currentName = target.title,
                onDismiss = { renameAlbumTarget = null },
                onConfirm = { newName ->
                    target.rawId?.let { rId ->
                        viewModel.renameAlbum(rId, newName)
                        selectedAlbum = selectedAlbum?.copy(title = newName)
                    }
                    renameAlbumTarget = null
                    Toast.makeText(context, "Album renamed", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}
