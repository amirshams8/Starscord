package com.nexus.android.data.local.dao

import androidx.room.*
import com.nexus.android.data.local.entities.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE channelId = :channelId ORDER BY createdAt DESC LIMIT :limit")
    fun getMessages(channelId: String, limit: Int = 100): Flow<List<MessageEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(messages: List<MessageEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(message: MessageEntity)
    @Delete suspend fun delete(message: MessageEntity)
    @Query("DELETE FROM messages WHERE channelId = :channelId") suspend fun deleteByChannel(channelId: String)
}
