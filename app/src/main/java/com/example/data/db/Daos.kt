package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GalleryDao {
    // Favorites
    @Query("SELECT mediaId FROM favorites")
    fun getAllFavoriteIds(): Flow<List<Long>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mediaId = :mediaId)")
    suspend fun isFavorite(mediaId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE mediaId = :mediaId")
    suspend fun deleteFavorite(mediaId: Long)

    @Query("DELETE FROM favorites")
    suspend fun clearFavorites()

    // Albums
    @Query("SELECT * FROM albums ORDER BY createdAt DESC")
    fun getAllAlbums(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums WHERE id = :id")
    suspend fun getAlbumById(id: Long): AlbumEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: AlbumEntity): Long

    @Update
    suspend fun updateAlbum(album: AlbumEntity)

    @Query("DELETE FROM albums WHERE id = :albumId")
    suspend fun deleteAlbum(albumId: Long)

    // Album Media CrossRef
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMediaToAlbum(crossRef: AlbumMediaCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMediaBatchToAlbum(crossRefs: List<AlbumMediaCrossRef>)

    @Query("DELETE FROM album_media WHERE albumId = :albumId AND mediaId = :mediaId")
    suspend fun removeMediaFromAlbum(albumId: Long, mediaId: Long)

    @Query("DELETE FROM album_media WHERE albumId = :albumId")
    suspend fun removeAllMediaFromAlbum(albumId: Long)

    @Query("SELECT mediaId FROM album_media WHERE albumId = :albumId ORDER BY addedAt DESC")
    fun getMediaIdsForAlbum(albumId: Long): Flow<List<Long>>

    @Query("SELECT mediaId FROM album_media WHERE albumId = :albumId ORDER BY addedAt DESC")
    suspend fun getMediaIdsForAlbumSync(albumId: Long): List<Long>

    @Query("SELECT COUNT(*) FROM album_media WHERE albumId = :albumId")
    suspend fun getMediaCountForAlbum(albumId: Long): Int

    // Recent Views
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordRecentView(recentView: RecentViewEntity)

    @Query("SELECT mediaId FROM recent_views ORDER BY viewedAt DESC LIMIT 100")
    fun getRecentViewIds(): Flow<List<Long>>

    @Query("DELETE FROM recent_views")
    suspend fun clearRecentViews()
}
