package com.nexus.android.data.local.dao

import androidx.room.*
import com.nexus.android.data.local.entities.GuildEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GuildDao {
    @Query("SELECT * FROM guilds ORDER BY name ASC") fun getAllGuilds(): Flow<List<GuildEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(guilds: List<GuildEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(guild: GuildEntity)
    @Delete suspend fun delete(guild: GuildEntity)
}
