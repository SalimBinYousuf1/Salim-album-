package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val mediaId: Long,
    val dateFavorited: Long = System.currentTimeMillis()
)

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val customCoverUri: String? = null
)

@Entity(tableName = "album_media", primaryKeys = ["albumId", "mediaId"])
data class AlbumMediaCrossRef(
    val albumId: Long,
    val mediaId: Long,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_views")
data class RecentViewEntity(
    @PrimaryKey val mediaId: Long,
    val viewedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "trash")
data class TrashEntity(
    @PrimaryKey val mediaId: Long,
    val trashedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "hidden")
data class HiddenEntity(
    @PrimaryKey val mediaId: Long,
    val hiddenAt: Long = System.currentTimeMillis()
)
