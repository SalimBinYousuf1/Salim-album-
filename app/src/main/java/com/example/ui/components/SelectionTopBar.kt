package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SalimBlue
import com.example.ui.theme.SalimRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    selectedCount: Int,
    totalCount: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onShare: () -> Unit,
    onAddToAlbum: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = if (selectedCount == 0) "Select Items" else "$selectedCount Selected",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onClose,
                modifier = Modifier.testTag("selection_close_button")
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close Selection")
            }
        },
        actions = {
            TextButton(
                onClick = onSelectAll,
                modifier = Modifier.testTag("selection_toggle_all_button")
            ) {
                Text(
                    text = if (selectedCount == totalCount) "Deselect" else "Select All",
                    color = SalimBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(
                onClick = onShare,
                enabled = selectedCount > 0,
                modifier = Modifier.testTag("selection_share_button")
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "Share Selected")
            }

            IconButton(
                onClick = onAddToAlbum,
                enabled = selectedCount > 0,
                modifier = Modifier.testTag("selection_album_button")
            ) {
                Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = "Add to Album")
            }

            IconButton(
                onClick = onFavorite,
                enabled = selectedCount > 0,
                modifier = Modifier.testTag("selection_favorite_button")
            ) {
                Icon(imageVector = Icons.Default.FavoriteBorder, contentDescription = "Favorite Selected")
            }

            IconButton(
                onClick = onDelete,
                enabled = selectedCount > 0,
                modifier = Modifier.testTag("selection_delete_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete Selected",
                    tint = if (selectedCount > 0) SalimRed else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
