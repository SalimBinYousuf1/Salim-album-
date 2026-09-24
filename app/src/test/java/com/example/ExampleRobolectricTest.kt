package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.MediaItem
import com.example.data.preferences.GroupingMode
import com.example.data.preferences.PreferencesManager
import com.example.data.preferences.SalimTheme
import com.example.data.preferences.SortOrder
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context verifies app name Salim`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Salim", appName)
    }

    @Test
    fun `test preferences manager defaults and updates`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefManager = PreferencesManager(context)
        val initial = prefManager.settings.value

        assertNotNull(initial)
        assertEquals(SalimTheme.LIGHT, initial.theme)

        prefManager.updateTheme(SalimTheme.DARK)
        assertEquals(SalimTheme.DARK, prefManager.settings.value.theme)

        prefManager.updateColumnCount(4)
        assertEquals(4, prefManager.settings.value.columnCount)

        prefManager.updateSortOrder(SortOrder.NAME_ASC)
        assertEquals(SortOrder.NAME_ASC, prefManager.settings.value.sortOrder)

        prefManager.updateGroupingMode(GroupingMode.MONTH)
        assertEquals(GroupingMode.MONTH, prefManager.settings.value.groupingMode)
    }

    @Test
    fun `test media item formatting properties`() {
        val testItem = MediaItem(
            id = 101L,
            uri = android.net.Uri.parse("content://media/external/images/media/101"),
            displayName = "Vacation_Panorama.jpg",
            dateTaken = 1700000000000L,
            dateModified = 1700000000000L,
            mimeType = "image/jpeg",
            size = 30 * 1024 * 1024L, // 30MB
            width = 4000,
            height = 1200,
            orientation = 0,
            duration = 0L,
            isVideo = false,
            bucketId = "123",
            bucketDisplayName = "Vacation"
        )

        assertTrue(testItem.isPanorama)
        assertTrue(testItem.isLargeFile)
        assertFalse(testItem.isVideo)
        assertEquals("30.0 MB", testItem.formattedSize)
    }
}
