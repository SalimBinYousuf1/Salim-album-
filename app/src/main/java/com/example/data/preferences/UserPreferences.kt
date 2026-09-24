package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SalimTheme {
    SYSTEM,
    LIGHT,
    DARK,
    ASGL
}

enum class SortOrder(val label: String) {
    DATE_NEWEST("Date (Newest first)"),
    DATE_OLDEST("Date (Oldest first)"),
    NAME_ASC("Name (A to Z)"),
    NAME_DESC("Name (Z to A)"),
    SIZE_DESC("Size (Largest first)")
}

enum class GroupingMode(val label: String) {
    DAY("Day"),
    MONTH("Month"),
    YEAR("Year"),
    NONE("None")
}

enum class SaveMethod(val label: String) {
    SAVE_AS_COPY("Save as new copy"),
    OVERWRITE_ORIGINAL("Overwrite original")
}

data class SalimSettings(
    val theme: SalimTheme = SalimTheme.LIGHT,
    val columnCount: Int = 3,
    val isSquareCrop: Boolean = true,
    val cornerRadiusDp: Int = 4,
    val showVideoBadges: Boolean = true,
    val showDateHeaders: Boolean = true,
    val sortOrder: SortOrder = SortOrder.DATE_NEWEST,
    val groupingMode: GroupingMode = GroupingMode.DAY,
    val showVideos: Boolean = true,
    val showScreenshots: Boolean = true,
    val animationsEnabled: Boolean = true,
    val isGoogleBackupEnabled: Boolean = true,
    val backupOverWifiOnly: Boolean = true,
    val autoplayVideos: Boolean = false,
    val livePhotoAutoplay: Boolean = true,
    val intelligentClassification: Boolean = true,
    val cleanupSuggestionsEnabled: Boolean = true,
    val saveMethodAfterEditing: SaveMethod = SaveMethod.SAVE_AS_COPY,
    val keepScreenOn: Boolean = false,
    val autoRotateViewing: Boolean = false,
    val followPortraitScreenLock: Boolean = true,
    val showHiddenPhotos: Boolean = false,
    val hiddenPhotosPassword: String? = null
) {
    val googlePhotosBackupEnabled: Boolean get() = isGoogleBackupEnabled
    val backupWifiOnly: Boolean get() = backupOverWifiOnly
}

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("salim_preferences", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<SalimSettings> = _settings.asStateFlow()

    private fun loadSettings(): SalimSettings {
        val themeStr = prefs.getString("theme", SalimTheme.LIGHT.name) ?: SalimTheme.LIGHT.name
        val theme = try { SalimTheme.valueOf(themeStr) } catch (e: Exception) { SalimTheme.LIGHT }

        val sortStr = prefs.getString("sortOrder", SortOrder.DATE_NEWEST.name) ?: SortOrder.DATE_NEWEST.name
        val sortOrder = try { SortOrder.valueOf(sortStr) } catch (e: Exception) { SortOrder.DATE_NEWEST }

        val groupStr = prefs.getString("groupingMode", GroupingMode.DAY.name) ?: GroupingMode.DAY.name
        val groupingMode = try { GroupingMode.valueOf(groupStr) } catch (e: Exception) { GroupingMode.DAY }

        val saveStr = prefs.getString("saveMethod", SaveMethod.SAVE_AS_COPY.name) ?: SaveMethod.SAVE_AS_COPY.name
        val saveMethod = try { SaveMethod.valueOf(saveStr) } catch (e: Exception) { SaveMethod.SAVE_AS_COPY }

        return SalimSettings(
            theme = theme,
            columnCount = prefs.getInt("columnCount", 3),
            isSquareCrop = prefs.getBoolean("isSquareCrop", true),
            cornerRadiusDp = prefs.getInt("cornerRadiusDp", 4),
            showVideoBadges = prefs.getBoolean("showVideoBadges", true),
            showDateHeaders = prefs.getBoolean("showDateHeaders", true),
            sortOrder = sortOrder,
            groupingMode = groupingMode,
            showVideos = prefs.getBoolean("showVideos", true),
            showScreenshots = prefs.getBoolean("showScreenshots", true),
            animationsEnabled = prefs.getBoolean("animationsEnabled", true),
            isGoogleBackupEnabled = prefs.getBoolean("isGoogleBackupEnabled", true),
            backupOverWifiOnly = prefs.getBoolean("backupOverWifiOnly", true),
            autoplayVideos = prefs.getBoolean("autoplayVideos", false),
            livePhotoAutoplay = prefs.getBoolean("livePhotoAutoplay", true),
            intelligentClassification = prefs.getBoolean("intelligentClassification", true),
            cleanupSuggestionsEnabled = prefs.getBoolean("cleanupSuggestionsEnabled", true),
            saveMethodAfterEditing = saveMethod,
            keepScreenOn = prefs.getBoolean("keepScreenOn", false),
            autoRotateViewing = prefs.getBoolean("autoRotateViewing", false),
            followPortraitScreenLock = prefs.getBoolean("followPortraitScreenLock", true),
            showHiddenPhotos = prefs.getBoolean("showHiddenPhotos", false),
            hiddenPhotosPassword = prefs.getString("hiddenPhotosPassword", null)
        )
    }

    fun updateTheme(theme: SalimTheme) {
        prefs.edit().putString("theme", theme.name).apply()
        _settings.value = _settings.value.copy(theme = theme)
    }

    fun updateColumnCount(count: Int) {
        val clamped = count.coerceIn(1, 5)
        prefs.edit().putInt("columnCount", clamped).apply()
        _settings.value = _settings.value.copy(columnCount = clamped)
    }

    fun updateSquareCrop(isSquare: Boolean) {
        prefs.edit().putBoolean("isSquareCrop", isSquare).apply()
        _settings.value = _settings.value.copy(isSquareCrop = isSquare)
    }

    fun updateCornerRadius(radius: Int) {
        prefs.edit().putInt("cornerRadiusDp", radius).apply()
        _settings.value = _settings.value.copy(cornerRadiusDp = radius)
    }

    fun updateShowVideoBadges(show: Boolean) {
        prefs.edit().putBoolean("showVideoBadges", show).apply()
        _settings.value = _settings.value.copy(showVideoBadges = show)
    }

    fun updateShowDateHeaders(show: Boolean) {
        prefs.edit().putBoolean("showDateHeaders", show).apply()
        _settings.value = _settings.value.copy(showDateHeaders = show)
    }

    fun updateSortOrder(order: SortOrder) {
        prefs.edit().putString("sortOrder", order.name).apply()
        _settings.value = _settings.value.copy(sortOrder = order)
    }

    fun updateGroupingMode(mode: GroupingMode) {
        prefs.edit().putString("groupingMode", mode.name).apply()
        _settings.value = _settings.value.copy(groupingMode = mode)
    }

    fun updateShowVideos(show: Boolean) {
        prefs.edit().putBoolean("showVideos", show).apply()
        _settings.value = _settings.value.copy(showVideos = show)
    }

    fun updateShowScreenshots(show: Boolean) {
        prefs.edit().putBoolean("showScreenshots", show).apply()
        _settings.value = _settings.value.copy(showScreenshots = show)
    }

    fun updateAnimationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("animationsEnabled", enabled).apply()
        _settings.value = _settings.value.copy(animationsEnabled = enabled)
    }

    fun updateGoogleBackupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("isGoogleBackupEnabled", enabled).apply()
        _settings.value = _settings.value.copy(isGoogleBackupEnabled = enabled)
    }

    fun updateBackupOverWifiOnly(wifiOnly: Boolean) {
        prefs.edit().putBoolean("backupOverWifiOnly", wifiOnly).apply()
        _settings.value = _settings.value.copy(backupOverWifiOnly = wifiOnly)
    }

    fun updateAutoplayVideos(autoplay: Boolean) {
        prefs.edit().putBoolean("autoplayVideos", autoplay).apply()
        _settings.value = _settings.value.copy(autoplayVideos = autoplay)
    }

    fun updateLivePhotoAutoplay(enabled: Boolean) {
        prefs.edit().putBoolean("livePhotoAutoplay", enabled).apply()
        _settings.value = _settings.value.copy(livePhotoAutoplay = enabled)
    }

    fun updateIntelligentClassification(enabled: Boolean) {
        prefs.edit().putBoolean("intelligentClassification", enabled).apply()
        _settings.value = _settings.value.copy(intelligentClassification = enabled)
    }

    fun updateCleanupSuggestionsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("cleanupSuggestionsEnabled", enabled).apply()
        _settings.value = _settings.value.copy(cleanupSuggestionsEnabled = enabled)
    }

    fun updateSaveMethod(method: SaveMethod) {
        prefs.edit().putString("saveMethod", method.name).apply()
        _settings.value = _settings.value.copy(saveMethodAfterEditing = method)
    }

    fun updateKeepScreenOn(keepOn: Boolean) {
        prefs.edit().putBoolean("keepScreenOn", keepOn).apply()
        _settings.value = _settings.value.copy(keepScreenOn = keepOn)
    }

    fun updateAutoRotateViewing(autoRotate: Boolean) {
        prefs.edit().putBoolean("autoRotateViewing", autoRotate).apply()
        _settings.value = _settings.value.copy(autoRotateViewing = autoRotate)
    }

    fun updateFollowPortraitScreenLock(follow: Boolean) {
        prefs.edit().putBoolean("followPortraitScreenLock", follow).apply()
        _settings.value = _settings.value.copy(followPortraitScreenLock = follow)
    }

    fun updateShowHiddenPhotos(show: Boolean) {
        prefs.edit().putBoolean("showHiddenPhotos", show).apply()
        _settings.value = _settings.value.copy(showHiddenPhotos = show)
    }

    fun updateHiddenPhotosPassword(password: String?) {
        prefs.edit().putString("hiddenPhotosPassword", password).apply()
        _settings.value = _settings.value.copy(hiddenPhotosPassword = password)
    }

    fun updateGooglePhotosBackup(enabled: Boolean) = updateGoogleBackupEnabled(enabled)
    fun updateBackupWifiOnly(wifiOnly: Boolean) = updateBackupOverWifiOnly(wifiOnly)
    fun setHiddenPassword(password: String?) = updateHiddenPhotosPassword(password)
}
