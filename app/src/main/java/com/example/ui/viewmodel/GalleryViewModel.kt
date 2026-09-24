package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AlbumEntity
import com.example.data.model.*
import com.example.data.preferences.*
import com.example.data.repository.MediaStoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaStoreRepository(application)
    val preferencesManager = PreferencesManager(application)
    val settings = preferencesManager.settings

    private val _allMedia = MutableStateFlow<List<MediaItem>>(emptyList())
    val allMedia: StateFlow<List<MediaItem>> = _allMedia.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _hasPermission = MutableStateFlow(true)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    // Selection mode state
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    // Search query & results
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchFilterType = MutableStateFlow("all") // all, photos, videos, favorites
    val searchFilterType: StateFlow<String> = _searchFilterType.asStateFlow()

    // User albums from Room
    val userAlbums: StateFlow<List<AlbumEntity>> = repository.userAlbums
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Favorites IDs from Room
    val favoriteIds: StateFlow<List<Long>> = repository.favoriteIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Trash and Hidden from Room
    val trashItems = repository.trashItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hiddenIds: StateFlow<List<Long>> = repository.hiddenIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Trashed Media
    val trashedMedia: StateFlow<List<MediaItem>> = combine(
        _allMedia,
        trashItems
    ) { media, trashList ->
        val trashMap = trashList.associateBy({ it.mediaId }, { it.trashedAt })
        media.filter { trashMap.containsKey(it.id) }.map {
            it.copy(trashedAt = trashMap[it.id])
        }.sortedByDescending { it.trashedAt ?: 0L }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Hidden Media
    val hiddenMedia: StateFlow<List<MediaItem>> = combine(
        _allMedia,
        hiddenIds
    ) { media, hiddenList ->
        val hiddenSet = hiddenList.toSet()
        media.filter { hiddenSet.contains(it.id) }.map {
            it.copy(isHidden = true)
        }.sortedByDescending { it.dateTaken }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var searchJob: Job? = null

    init {
        // Observe system MediaStore for changes
        viewModelScope.launch {
            repository.observeMediaStore().collect {
                refreshLibrary()
            }
        }
    }

    fun setPermissionGranted(granted: Boolean) {
        _hasPermission.value = granted
        if (granted) {
            refreshLibrary()
        }
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val media = repository.fetchAllMedia()
                _allMedia.value = media
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isScanning.value = false
            }
        }
    }

    // Filtered and sorted media based on settings
    val filteredMedia: StateFlow<List<MediaItem>> = combine(
        _allMedia,
        settings,
        favoriteIds,
        trashItems,
        hiddenIds
    ) { media, currentSettings, favIds, trashList, hiddenList ->
        val favSet = favIds.toSet()
        val trashSet = trashList.map { it.mediaId }.toSet()
        val hiddenSet = hiddenList.toSet()

        var list = media.map { item ->
            item.copy(
                isFavorite = favSet.contains(item.id),
                isHidden = hiddenSet.contains(item.id)
            )
        }

        // Exclude trashed and hidden items from main library timeline
        list = list.filter { !trashSet.contains(it.id) && !hiddenSet.contains(it.id) }

        if (!currentSettings.showVideos) {
            list = list.filter { !it.isVideo }
        }

        if (!currentSettings.showScreenshots) {
            list = list.filter { !it.isScreenshot }
        }

        when (currentSettings.sortOrder) {
            SortOrder.DATE_NEWEST -> list.sortedByDescending { it.dateTaken }
            SortOrder.DATE_OLDEST -> list.sortedBy { it.dateTaken }
            SortOrder.NAME_ASC -> list.sortedBy { it.displayName.lowercase() }
            SortOrder.NAME_DESC -> list.sortedByDescending { it.displayName.lowercase() }
            SortOrder.SIZE_DESC -> list.sortedByDescending { it.size }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Date-grouped sections for the main timeline
    val mediaSections: StateFlow<List<MediaSection>> = combine(
        filteredMedia,
        settings
    ) { media, currentSettings ->
        groupMediaBySetting(media, currentSettings.groupingMode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun groupMediaBySetting(media: List<MediaItem>, mode: GroupingMode): List<MediaSection> {
        if (media.isEmpty()) return emptyList()
        if (mode == GroupingMode.NONE) {
            return listOf(MediaSection(title = "All Media", items = media))
        }

        val pattern = when (mode) {
            GroupingMode.DAY -> "EEEE, MMMM d, yyyy"
            GroupingMode.MONTH -> "MMMM yyyy"
            GroupingMode.YEAR -> "yyyy"
            GroupingMode.NONE -> ""
        }

        val formatter = SimpleDateFormat(pattern, Locale.getDefault())
        val groupedMap = LinkedHashMap<String, MutableList<MediaItem>>()

        for (item in media) {
            val dateKey = formatter.format(Date(item.dateTaken))
            groupedMap.getOrPut(dateKey) { mutableListOf() }.add(item)
        }

        return groupedMap.map { (key, items) ->
            MediaSection(title = key, items = items)
        }
    }

    // Cleanup suggestions
    val cleanupSuggestions: StateFlow<CleanupCandidates> = filteredMedia.map { media ->
        val dupes = media.groupBy { "${it.size}_${it.width}_${it.height}" }
            .filter { it.value.size > 1 }
            .flatMap { it.value.drop(1) }

        val large = media.filter { it.size > 50 * 1024 * 1024L }
        val screenshots = media.filter { it.isScreenshot }
        val totalBytes = dupes.sumOf { it.size } + large.sumOf { it.size }

        CleanupCandidates(
            duplicates = dupes,
            largeFiles = large,
            oldScreenshots = screenshots,
            totalCleanableBytes = totalBytes
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CleanupCandidates())

    // Built-in Albums & Smart Folders
    val albumsList: StateFlow<List<SalimAlbum>> = combine(
        filteredMedia,
        userAlbums,
        trashedMedia,
        hiddenMedia,
        settings
    ) { media, customAlbums, trashed, hidden, curSettings ->
        val result = mutableListOf<SalimAlbum>()

        // 1. All Photos & Videos
        result.add(
            SalimAlbum(
                id = "all",
                title = "Recents",
                count = media.size,
                coverUri = media.firstOrNull()?.uri,
                type = AlbumType.ALL
            )
        )

        // 2. Favorites
        val favCount = media.count { it.isFavorite }
        val favCover = media.firstOrNull { it.isFavorite }?.uri
        result.add(
            SalimAlbum(
                id = "favorites",
                title = "Favorites",
                count = favCount,
                coverUri = favCover,
                type = AlbumType.FAVORITES
            )
        )

        // 3. Videos
        val videoItems = media.filter { it.isVideo }
        if (videoItems.isNotEmpty()) {
            result.add(
                SalimAlbum(
                    id = "videos",
                    title = "Videos",
                    count = videoItems.size,
                    coverUri = videoItems.firstOrNull()?.uri,
                    type = AlbumType.VIDEOS
                )
            )
        }

        // 4. Screenshots
        val screenshotItems = media.filter { it.isScreenshot }
        if (screenshotItems.isNotEmpty()) {
            result.add(
                SalimAlbum(
                    id = "screenshots",
                    title = "Screenshots",
                    count = screenshotItems.size,
                    coverUri = screenshotItems.firstOrNull()?.uri,
                    type = AlbumType.SCREENSHOTS
                )
            )
        }

        // 5. Intelligent classification: Selfies
        if (curSettings.intelligentClassification) {
            val selfieItems = media.filter { it.isSelfie }
            if (selfieItems.isNotEmpty()) {
                result.add(
                    SalimAlbum(
                        id = "selfies",
                        title = "Selfies",
                        count = selfieItems.size,
                        coverUri = selfieItems.firstOrNull()?.uri,
                        type = AlbumType.SELFIES
                    )
                )
            }
        }

        // 6. Camera
        val cameraItems = media.filter { it.bucketDisplayName.contains("camera", ignoreCase = true) || it.bucketDisplayName.contains("dcim", ignoreCase = true) }
        if (cameraItems.isNotEmpty()) {
            result.add(
                SalimAlbum(
                    id = "camera",
                    title = "Camera",
                    count = cameraItems.size,
                    coverUri = cameraItems.firstOrNull()?.uri,
                    type = AlbumType.CAMERA
                )
            )
        }

        // 7. Downloads
        val downloadItems = media.filter { it.bucketDisplayName.contains("download", ignoreCase = true) }
        if (downloadItems.isNotEmpty()) {
            result.add(
                SalimAlbum(
                    id = "downloads",
                    title = "Downloads",
                    count = downloadItems.size,
                    coverUri = downloadItems.firstOrNull()?.uri,
                    type = AlbumType.DOWNLOADS
                )
            )
        }

        // 8. Panoramas
        val panoramaItems = media.filter { it.isPanorama }
        if (panoramaItems.isNotEmpty()) {
            result.add(
                SalimAlbum(
                    id = "panoramas",
                    title = "Panoramas",
                    count = panoramaItems.size,
                    coverUri = panoramaItems.firstOrNull()?.uri,
                    type = AlbumType.PANORAMAS
                )
            )
        }

        // 9. Large Files (>25MB)
        val largeFiles = media.filter { it.isLargeFile }
        if (largeFiles.isNotEmpty()) {
            result.add(
                SalimAlbum(
                    id = "large_files",
                    title = "Large Files",
                    count = largeFiles.size,
                    coverUri = largeFiles.firstOrNull()?.uri,
                    type = AlbumType.LARGE_FILES
                )
            )
        }

        // 10. RAW Photos
        val rawItems = media.filter { it.isRaw }
        if (rawItems.isNotEmpty()) {
            result.add(
                SalimAlbum(
                    id = "raw_photos",
                    title = "RAW",
                    count = rawItems.size,
                    coverUri = rawItems.firstOrNull()?.uri,
                    type = AlbumType.RAW
                )
            )
        }

        // 11. Hidden Photos
        if (curSettings.showHiddenPhotos) {
            result.add(
                SalimAlbum(
                    id = "hidden",
                    title = "Hidden",
                    count = hidden.size,
                    coverUri = hidden.firstOrNull()?.uri,
                    type = AlbumType.HIDDEN
                )
            )
        }

        // 12. Recently Deleted (Trash)
        result.add(
            SalimAlbum(
                id = "trash",
                title = "Recently Deleted",
                count = trashed.size,
                coverUri = trashed.firstOrNull()?.uri,
                type = AlbumType.RECENTLY_DELETED
            )
        )

        // 13. Distinct Folders (Buckets)
        val bucketMap = media.groupBy { it.bucketDisplayName }
        for ((bucketName, bucketList) in bucketMap) {
            if (bucketName.isNotEmpty() &&
                !bucketName.equals("camera", ignoreCase = true) &&
                !bucketName.equals("dcim", ignoreCase = true) &&
                !bucketName.equals("screenshots", ignoreCase = true) &&
                !bucketName.equals("download", ignoreCase = true) &&
                !bucketName.equals("downloads", ignoreCase = true)
            ) {
                result.add(
                    SalimAlbum(
                        id = "folder_${bucketList.first().bucketId}",
                        title = bucketName,
                        count = bucketList.size,
                        coverUri = bucketList.firstOrNull()?.uri,
                        type = AlbumType.FOLDER,
                        bucketId = bucketList.first().bucketId
                    )
                )
            }
        }

        // 14. User Created Albums from Room
        for (custom in customAlbums) {
            val albumMediaIds = repository.getMediaIdsForAlbum(custom.id).toSet()
            val albumMedia = media.filter { albumMediaIds.contains(it.id) }
            result.add(
                SalimAlbum(
                    id = "user_${custom.id}",
                    title = custom.name,
                    count = albumMedia.size,
                    coverUri = custom.customCoverUri?.let { Uri.parse(it) } ?: albumMedia.firstOrNull()?.uri,
                    type = AlbumType.USER_CREATED,
                    rawId = custom.id
                )
            )
        }

        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selection management
    fun startSelection(initialId: Long? = null) {
        _isSelectionMode.value = true
        _selectedIds.value = if (initialId != null) setOf(initialId) else emptySet()
    }

    fun toggleSelection(id: Long) {
        val current = _selectedIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
            if (current.isEmpty()) {
                _isSelectionMode.value = false
            }
        } else {
            current.add(id)
        }
        _selectedIds.value = current
    }

    fun selectAll(items: List<MediaItem>) {
        _isSelectionMode.value = true
        _selectedIds.value = items.map { it.id }.toSet()
    }

    fun clearSelection() {
        _isSelectionMode.value = false
        _selectedIds.value = emptySet()
    }

    fun selectSection(sectionItems: List<MediaItem>) {
        val current = _selectedIds.value.toMutableSet()
        val sectionIds = sectionItems.map { it.id }
        if (current.containsAll(sectionIds)) {
            current.removeAll(sectionIds.toSet())
            if (current.isEmpty()) _isSelectionMode.value = false
        } else {
            _isSelectionMode.value = true
            current.addAll(sectionIds)
        }
        _selectedIds.value = current
    }

    // Media actions
    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch {
            repository.toggleFavorite(item.id)
            // Update local memory list optimistically
            _allMedia.value = _allMedia.value.map {
                if (it.id == item.id) it.copy(isFavorite = !it.isFavorite) else it
            }
        }
    }

    fun batchFavorite(favorite: Boolean) {
        val selected = _selectedIds.value
        viewModelScope.launch {
            selected.forEach { id ->
                repository.setFavorite(id, favorite)
            }
            _allMedia.value = _allMedia.value.map {
                if (selected.contains(it.id)) it.copy(isFavorite = favorite) else it
            }
            clearSelection()
        }
    }

    fun moveToTrash(items: List<MediaItem>) {
        viewModelScope.launch {
            items.forEach { repository.moveToTrash(it.id) }
            clearSelection()
        }
    }

    fun restoreFromTrash(items: List<MediaItem>) {
        viewModelScope.launch {
            items.forEach { repository.restoreFromTrash(it.id) }
            clearSelection()
        }
    }

    fun permanentlyDelete(items: List<MediaItem>, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteMedia(items)
            if (result.isSuccess) {
                val deletedIds = items.map { it.id }.toSet()
                _allMedia.value = _allMedia.value.filter { !deletedIds.contains(it.id) }
                clearSelection()
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }

    fun emptyTrash(onComplete: (Boolean) -> Unit) {
        val items = trashedMedia.value
        permanentlyDelete(items, onComplete)
    }

    fun hideMedia(items: List<MediaItem>) {
        viewModelScope.launch {
            items.forEach { repository.hideMedia(it.id) }
            clearSelection()
        }
    }

    fun unhideMedia(items: List<MediaItem>) {
        viewModelScope.launch {
            items.forEach { repository.unhideMedia(it.id) }
            clearSelection()
        }
    }

    fun deleteMedia(items: List<MediaItem>, onComplete: (Boolean) -> Unit) {
        moveToTrash(items)
        onComplete(true)
    }

    fun renameMedia(item: MediaItem, newName: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.renameMedia(item.uri, item.isVideo, newName)
            if (result.isSuccess) {
                _allMedia.value = _allMedia.value.map {
                    if (it.id == item.id) it.copy(displayName = newName) else it
                }
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun copyMedia(item: MediaItem, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.saveMediaCopy(item.uri, item.displayName, item.isVideo)
            if (result.isSuccess) {
                refreshLibrary()
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun recordView(item: MediaItem) {
        viewModelScope.launch {
            repository.recordRecentView(item.id)
        }
    }

    suspend fun getExif(item: MediaItem): ExifData {
        return repository.getExifData(item)
    }

    // User Albums CRUD
    fun createAlbum(name: String, initialMediaIds: List<Long> = emptyList()) {
        viewModelScope.launch {
            val albumId = repository.createAlbum(name)
            if (initialMediaIds.isNotEmpty()) {
                repository.addMediaToAlbum(albumId, initialMediaIds)
            }
            clearSelection()
        }
    }

    fun renameAlbum(albumId: Long, newName: String) {
        viewModelScope.launch {
            repository.renameAlbum(albumId, newName)
        }
    }

    fun deleteAlbum(albumId: Long) {
        viewModelScope.launch {
            repository.deleteAlbum(albumId)
        }
    }

    fun addSelectedToAlbum(albumId: Long) {
        val selected = _selectedIds.value.toList()
        viewModelScope.launch {
            repository.addMediaToAlbum(albumId, selected)
            clearSelection()
        }
    }

    fun removeMediaFromAlbum(albumId: Long, mediaId: Long) {
        viewModelScope.launch {
            repository.removeMediaFromAlbum(albumId, mediaId)
        }
    }

    suspend fun getMediaForUserAlbum(albumId: Long): List<MediaItem> {
        val ids = repository.getMediaIdsForAlbum(albumId).toSet()
        return _allMedia.value.filter { ids.contains(it.id) }
    }

    // Search operations
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchFilterType(type: String) {
        _searchFilterType.value = type
    }

    val searchResults: StateFlow<List<MediaItem>> = combine(
        filteredMedia,
        _searchQuery,
        _searchFilterType
    ) { media, query, filter ->
        if (query.isBlank() && filter == "all") {
            return@combine emptyList()
        }

        val q = query.trim().lowercase()

        media.filter { item ->
            val matchesFilter = when (filter) {
                "photos" -> !item.isVideo
                "videos" -> item.isVideo
                "favorites" -> item.isFavorite
                else -> true
            }

            if (!matchesFilter) return@filter false
            if (q.isBlank()) return@filter true

            val nameMatch = item.displayName.lowercase().contains(q)
            val folderMatch = item.bucketDisplayName.lowercase().contains(q)
            val formatMatch = item.mimeType.lowercase().contains(q)
            val dateTakenStr = SimpleDateFormat("yyyy MMMM d", Locale.getDefault()).format(Date(item.dateTaken)).lowercase()
            val dateMatch = dateTakenStr.contains(q)

            nameMatch || folderMatch || formatMatch || dateMatch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

data class CleanupCandidates(
    val duplicates: List<MediaItem> = emptyList(),
    val largeFiles: List<MediaItem> = emptyList(),
    val oldScreenshots: List<MediaItem> = emptyList(),
    val totalCleanableBytes: Long = 0L
)
