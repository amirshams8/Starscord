package com.nexus.android.data.api.models

import com.google.gson.annotations.SerializedName

// FIX: joinedAt is nullable (Gson ignores Kotlin defaults for absent JSON fields).
// FIX: roles is now nullable for the same reason — Gson sets it to null when the
//      JSON field is absent, causing NPE inside LazyColumn item composition in
//      ChannelMembersScreen and ServerSettingsScreen (crashes at 02:33:25-02:33:54).
//      Use .orEmpty() at all call sites.
data class MemberResponse(
    @SerializedName("guild_id")  val guildId: String,
    @SerializedName("user_id")   val userId: String,
    val nickname: String? = null,
    val roles: List<String>? = null,
    @SerializedName("joined_at") val joinedAt: String? = null,
    val user: UserResponse? = null,
)
