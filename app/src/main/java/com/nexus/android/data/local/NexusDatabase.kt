package com.nexus.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nexus.android.data.local.dao.GuildDao
import com.nexus.android.data.local.dao.MessageDao
import com.nexus.android.data.local.entities.ChannelEntity
import com.nexus.android.data.local.entities.GuildEntity
import com.nexus.android.data.local.entities.MessageEntity

@Database(entities = [MessageEntity::class, GuildEntity::class, ChannelEntity::class], version = 1, exportSchema = false)
abstract class NexusDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun guildDao(): GuildDao
}
