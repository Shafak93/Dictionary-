package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DictionaryDao {
    @Query("SELECT * FROM cached_words WHERE word = :word LIMIT 1")
    suspend fun getCachedWord(word: String): DictionaryEntity?

    @Query("SELECT * FROM cached_words WHERE word = :word LIMIT 1")
    fun getCachedWordFlow(word: String): Flow<DictionaryEntity?>

    @Query("SELECT * FROM cached_words ORDER BY timestamp DESC")
    fun getAllCachedWordsFlow(): Flow<List<DictionaryEntity>>

    @Query("SELECT * FROM cached_words WHERE isBookmarked = 1 ORDER BY timestamp DESC")
    fun getBookmarkedWordsFlow(): Flow<List<DictionaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedWord(entity: DictionaryEntity)

    @Query("UPDATE cached_words SET isBookmarked = :isBookmarked WHERE word = :word")
    suspend fun updateBookmarkStatus(word: String, isBookmarked: Boolean)

    @Query("DELETE FROM cached_words WHERE word = :word")
    suspend fun deleteCachedWord(word: String)

    // History Queries
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 50")
    fun getHistoryFlow(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entity: HistoryEntity)

    @Query("DELETE FROM search_history WHERE word = :word")
    suspend fun deleteHistoryByWord(word: String)

    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun deleteHistoryItem(id: Int)

    @Query("DELETE FROM search_history")
    suspend fun clearHistory()
}
