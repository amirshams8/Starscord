package com.nexus.android.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(@PrimaryKey val id: String, val channelId: String, val authorId: String, val authorUsername: String, val authorAvatar: String?, val content: String?, val createdAt: String, val editedAt: String? = null)

@Entity(tableName = "guilds")
data class GuildEntity(@PrimaryKey val id: String, val name: String, val icon: String?, val iconAnimated: Boolean = false, val boostTier: Int = 0, val boostCount: Int = 0)

@Entity(tableName = "channels")
data class ChannelEntity(@PrimaryKey val id: String, val guildId: String?, val name: String, val type: String, val position: Int, val parentId: String?)
