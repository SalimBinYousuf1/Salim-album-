package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SalimBlue

enum class SalimTab(val title: String) {
    PHOTOS("Photos"),
    ALBUMS("Albums"),
    FAVORITES("Favorites"),
    VIDEOS("Videos"),
    SEARCH("Search"),
    SETTINGS("Settings")
}

@Composable
fun SalimBottomBar(
    currentTab: SalimTab,
    onTabSelected: (SalimTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.testTag("salim_bottom_navigation_bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        // Photos
        NavigationBarItem(
            selected = currentTab == SalimTab.PHOTOS,
            onClick = { onTabSelected(SalimTab.PHOTOS) },
            icon = {
                Icon(
                    imageVector = if (currentTab == SalimTab.PHOTOS) Icons.Filled.PhotoLibrary else Icons.Outlined.PhotoLibrary,
                    contentDescription = "Photos"
                )
            },
            label = { Text("Photos", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SalimBlue,
                selectedTextColor = SalimBlue,
                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.testTag("tab_photos")
        )

        // Albums
        NavigationBarItem(
            selected = currentTab == SalimTab.ALBUMS,
            onClick = { onTabSelected(SalimTab.ALBUMS) },
            icon = {
                Icon(
                    imageVector = if (currentTab == SalimTab.ALBUMS) Icons.Filled.Folder else Icons.Outlined.Folder,
                    contentDescription = "Albums"
                )
            },
            label = { Text("Albums", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SalimBlue,
                selectedTextColor = SalimBlue,
                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.testTag("tab_albums")
        )

        // Favorites
        NavigationBarItem(
            selected = currentTab == SalimTab.FAVORITES,
            onClick = { onTabSelected(SalimTab.FAVORITES) },
            icon = {
                Icon(
                    imageVector = if (currentTab == SalimTab.FAVORITES) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorites"
                )
            },
            label = { Text("Favorites", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SalimBlue,
                selectedTextColor = SalimBlue,
                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.testTag("tab_favorites")
        )

        // Videos
        NavigationBarItem(
            selected = currentTab == SalimTab.VIDEOS,
            onClick = { onTabSelected(SalimTab.VIDEOS) },
            icon = {
                Icon(
                    imageVector = if (currentTab == SalimTab.VIDEOS) Icons.Filled.Videocam else Icons.Outlined.Videocam,
                    contentDescription = "Videos"
                )
            },
            label = { Text("Videos", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SalimBlue,
                selectedTextColor = SalimBlue,
                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.testTag("tab_videos")
        )

        // Search
        NavigationBarItem(
            selected = currentTab == SalimTab.SEARCH,
            onClick = { onTabSelected(SalimTab.SEARCH) },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search"
                )
            },
            label = { Text("Search", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SalimBlue,
                selectedTextColor = SalimBlue,
                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.testTag("tab_search")
        )

        // Settings
        NavigationBarItem(
            selected = currentTab == SalimTab.SETTINGS,
            onClick = { onTabSelected(SalimTab.SETTINGS) },
            icon = {
                Icon(
                    imageVector = if (currentTab == SalimTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                    contentDescription = "Settings"
                )
            },
            label = { Text("Settings", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SalimBlue,
                selectedTextColor = SalimBlue,
                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.testTag("tab_settings")
        )
    }
}
