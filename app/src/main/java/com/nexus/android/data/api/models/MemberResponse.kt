package com.nexus.android.data.api.models

import com.google.gson.annotations.SerializedName

// FIX: joinedAt is now nullable.
// Gson silently deserializes absent/null JSON fields as null even for non-nullable Kotlin types,
// which causes a Kotlin runtime NPE ("lateinit … was null") when the field is touched in Compose
// LazyColumn item composition. Making it nullable? stops the crash with zero logic changes elsewhere.
data class MemberResponse(
    @SerializedName("guild_id")  val guildId: String,
    @SerializedName("user_id")   val userId: String,
    val nickname: String? = null,
    val roles: List<String> = emptyList(),
    @SerializedName("joined_at") val joinedAt: String? = null,
    val user: UserResponse? = null,
)
