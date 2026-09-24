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
import com.example.util.BackupManager
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

    val trashedMedia by viewModel.trashedMedia.collectAsStateWithLifecycle()
    val hiddenMedia by viewModel.hiddenMedia.collectAsStateWithLifecycle()
    val cleanupSuggestions by viewModel.cleanupSuggestions.collectAsStateWithLifecycle()

    // Navigation and screen state
    var currentTab by remember { mutableStateOf(SalimTab.PHOTOS) }
    var selectedAlbum by remember { mutableStateOf<SalimAlbum?>(null) }
    var viewingMediaItem by remember { mutableStateOf<MediaItem?>(null) }
    var editingMediaItem by remember { mutableStateOf<MediaItem?>(null) }
    var editingVideoItem by remember { mutableStateOf<MediaItem?>(null) }
    var showCleanupScreen by remember { mutableStateOf(false) }
    var showBackupDetailDialog by remember { mutableStateOf(false) }
    var showPasswordPrompt by remember { mutableStateOf(false) }
    var pendingHiddenUnlock by remember { mutableStateOf<(() -> Unit)?>(null) }
    var detailsExifData by remember { mutableStateOf<ExifData?>(null) }

    // User album media cache
    var userAlbumMedia by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    LaunchedEffect(selectedAlbum?.id, allFilteredMedia) {
        if (selectedAlbum?.type == AlbumType.USER_CREATED) {
            selectedAlbum?.rawId?.let { rId ->
                userAlbumMedia = viewModel.getMediaForUserAlbum(rId)
            }
        }
    }

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
            enabled = editingMediaItem != null || editingVideoItem != null || showCleanupScreen || viewingMediaItem != null || selectedAlbum != null || isSelectionMode
        ) {
            when {
                editingMediaItem != null -> editingMediaItem = null
                editingVideoItem != null -> editingVideoItem = null
                showCleanupScreen -> showCleanupScreen = false
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
                    AlbumType.USER_CREATED -> userAlbumMedia
                    AlbumType.RECENTLY_DELETED -> trashedMedia
                    AlbumType.HIDDEN -> hiddenMedia
                    AlbumType.SELFIES -> allFilteredMedia.filter { it.isSelfie }
                    AlbumType.BURSTS -> allFilteredMedia.filter { it.displayName.contains("burst", true) }
                    AlbumType.RAW -> allFilteredMedia.filter { it.isRaw }
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
                if (isSelectionMode && viewingMediaItem == null && editingMediaItem == null && editingVideoItem == null && !showCleanupScreen) {
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
                if (viewingMediaItem == null && editingMediaItem == null && editingVideoItem == null && !showCleanupScreen && !isSelectionMode) {
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

                    AlbumDetailScreen(
                        album = album,
                        media = activeMediaList,
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
                        } else null,
                        onRestoreItems = if (album.type == AlbumType.RECENTLY_DELETED) {
                            { items ->
                                viewModel.restoreFromTrash(items)
                                Toast.makeText(context, "Restored ${items.size} item(s)", Toast.LENGTH_SHORT).show()
                            }
                        } else null,
                        onPermanentDeleteItems = if (album.type == AlbumType.RECENTLY_DELETED) {
                            { items ->
                                viewModel.permanentlyDelete(items) {
                                    Toast.makeText(context, "Permanently deleted", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else null,
                        onEmptyTrash = if (album.type == AlbumType.RECENTLY_DELETED) {
                            {
                                viewModel.emptyTrash {
                                    Toast.makeText(context, "Trash emptied", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else null,
                        onUnhideItems = if (album.type == AlbumType.HIDDEN) {
                            { items ->
                                viewModel.unhideMedia(items)
                                Toast.makeText(context, "Unhidden ${items.size} item(s)", Toast.LENGTH_SHORT).show()
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
                                onAlbumClick = { album ->
                                    if (album.type == AlbumType.HIDDEN && settings.hiddenPhotosPassword != null) {
                                        pendingHiddenUnlock = { selectedAlbum = album }
                                        showPasswordPrompt = true
                                    } else {
                                        selectedAlbum = album
                                    }
                                }
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
                                trashCount = trashedMedia.size,
                                preferencesManager = viewModel.preferencesManager,
                                onRescanLibrary = { viewModel.refreshLibrary() },
                                onOpenBackupDetail = { showBackupDetailDialog = true },
                                onOpenCleanup = { showCleanupScreen = true },
                                onOpenRecentlyDeleted = {
                                    selectedAlbum = albumsList.firstOrNull { it.type == AlbumType.RECENTLY_DELETED }
                                },
                                onOpenPasswordPrompt = { showPasswordPrompt = true }
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
                keepScreenOn = settings.keepScreenOn,
                autoplayVideos = settings.autoplayVideos,
                onBack = { viewingMediaItem = null },
                onToggleFavorite = { item -> viewModel.toggleFavorite(item) },
                onShare = { item -> MediaActions.shareSingleMedia(context, item) },
                onEdit = { item -> editingMediaItem = item },
                onEditVideo = { item -> editingVideoItem = item },
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
                onOpenWith = { item -> MediaActions.openWithOtherApp(context, item) },
                onHide = { item ->
                    viewModel.hideMedia(listOf(item))
                    viewingMediaItem = null
                    Toast.makeText(context, "Moved to Hidden album", Toast.LENGTH_SHORT).show()
                },
                onBackupToGoogle = { item ->
                    BackupManager.uploadItemToGooglePhotos(context, item)
                }
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

        // Video Editor Overlay
        if (editingVideoItem != null) {
            VideoEditorScreen(
                item = editingVideoItem!!,
                onCancel = { editingVideoItem = null },
                onSaveSuccess = { _ ->
                    editingVideoItem = null
                    viewModel.refreshLibrary()
                    Toast.makeText(context, "Trimmed video saved", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Cleanup Suggestions Screen Overlay
        if (showCleanupScreen) {
            CleanupScreen(
                candidates = cleanupSuggestions,
                onBack = { showCleanupScreen = false },
                onCleanItems = { items ->
                    viewModel.deleteMedia(items) {
                        Toast.makeText(context, "Cleaned up ${items.size} item(s)", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // Backup Detail Dialog
        if (showBackupDetailDialog) {
            BackupDetailDialog(
                isEnabled = settings.googlePhotosBackupEnabled,
                isWifiOnly = settings.backupWifiOnly,
                totalItemsCount = allFilteredMedia.size,
                onToggleEnabled = { enabled ->
                    coroutineScope.launch { viewModel.preferencesManager.updateGooglePhotosBackup(enabled) }
                },
                onToggleWifiOnly = { wifiOnly ->
                    coroutineScope.launch { viewModel.preferencesManager.updateBackupWifiOnly(wifiOnly) }
                },
                onBackupNow = {
                    BackupManager.uploadBatchToGooglePhotos(context, allFilteredMedia)
                },
                onOpenGooglePhotos = {
                    BackupManager.openGooglePhotos(context)
                },
                onDismiss = { showBackupDetailDialog = false }
            )
        }

        // Hidden Vault Auth Dialog
        if (showPasswordPrompt) {
            HiddenVaultAuthDialog(
                hasExistingPassword = settings.hiddenPhotosPassword != null,
                correctPassword = settings.hiddenPhotosPassword,
                onDismiss = {
                    showPasswordPrompt = false
                    pendingHiddenUnlock = null
                },
                onSuccess = {
                    showPasswordPrompt = false
                    pendingHiddenUnlock?.invoke()
                    pendingHiddenUnlock = null
                },
                onSetNewPassword = { pin ->
                    coroutineScope.launch {
                        viewModel.preferencesManager.setHiddenPassword(pin)
                        Toast.makeText(context, "PIN updated successfully", Toast.LENGTH_SHORT).show()
                    }
                    showPasswordPrompt = false
                    pendingHiddenUnlock?.invoke()
                    pendingHiddenUnlock = null
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
