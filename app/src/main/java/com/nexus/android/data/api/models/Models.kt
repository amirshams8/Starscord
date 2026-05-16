package com.nexus.android.data.api.models

import com.google.gson.annotations.SerializedName

data class RegisterRequest(val username: String, val email: String, val password: String)
data class LoginRequest(val email: String, val password: String, @SerializedName("mfaCode") val mfaCode: String? = null)
data class LoginResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Int,
    val user: UserResponse,
)
data class RefreshResponse(@SerializedName("access_token") val accessToken: String)
data class MfaEnableResponse(val secret: String, @SerializedName("qr_code") val qrCode: String)
data class MfaVerifyRequest(val code: String)
data class SuccessResponse(val success: Boolean)

data class UserResponse(
    val id: String,
    val username: String,
    val discriminator: String,
    val email: String? = null,
    val avatar: String? = null,
    @SerializedName("avatar_animated") val avatarAnimated: Boolean = false,
    val banner: String? = null,
    @SerializedName("banner_color") val bannerColor: String? = null,
    val bio: String? = null,
    val pronouns: String? = null,
    val status: String = "online",
    @SerializedName("custom_status") val customStatus: String? = null,
    @SerializedName("nitro_type") val nitroType: String = "none",
    @SerializedName("nitro_since") val nitroSince: String? = null,
    val flags: Long = 0,
    @SerializedName("created_at") val createdAt: String? = null,
)

data class UpdateUserRequest(
    val username: String? = null,
    val bio: String? = null,
    val pronouns: String? = null,
    val avatar: String? = null,
    val banner: String? = null,
    @SerializedName("banner_color") val bannerColor: String? = null,
    @SerializedName("custom_status") val customStatus: String? = null,
    val status: String? = null,
)

data class CreateGuildRequest(val name: String, val icon: String? = null)
data class GuildResponse(
    val id: String,
    val name: String,
    val icon: String? = null,
    @SerializedName("icon_animated") val iconAnimated: Boolean = false,
    val banner: String? = null,
    @SerializedName("owner_id") val ownerId: String,
    val description: String? = null,
    @SerializedName("boost_count") val boostCount: Int = 0,
    @SerializedName("boost_tier") val boostTier: Int = 0,
    @SerializedName("vanity_url") val vanityUrl: String? = null,
    val channels: List<ChannelResponse>? = null,
    val roles: List<RoleResponse>? = null,
    @SerializedName("joined_at") val joinedAt: String? = null,
)

data class CreateChannelRequest(val name: String, val type: String = "text", val parentId: String? = null)

data class ChannelResponse(
    val id: String,
    @SerializedName("guild_id") val guildId: String? = null,
    val type: String,
    val name: String,
    val position: Int = 0,
    val topic: String? = null,
    val nsfw: Boolean = false,
    val slowmode: Int = 0,
    val bitrate: Int = 64000,
    @SerializedName("user_limit") val userLimit: Int = 0,
    @SerializedName("parent_id") val parentId: String? = null,
    @SerializedName("last_message_id") val lastMessageId: String? = null,
)

data class MemberResponse(
    @SerializedName("guild_id") val guildId: String,
    @SerializedName("user_id") val userId: String,
    val nickname: String? = null,
    val roles: List<String> = emptyList(),
    @SerializedName("joined_at") val joinedAt: String,
    val user: UserResponse? = null,
)

data class RoleResponse(
    val id: String,
    @SerializedName("guild_id") val guildId: String,
    val name: String,
    val color: Int = 0,
    val hoist: Boolean = false,
    val position: Int = 0,
    val permissions: String = "0",
    val mentionable: Boolean = false,
    @SerializedName("member_count") val memberCount: Int = 0,
)

data class SendMessageRequest(
    val content: String? = null,
    val tts: Boolean = false,
    val embeds: List<Any>? = null,
    val reference: MessageReferenceRequest? = null,
)
data class EditMessageRequest(val content: String)
data class MessageReferenceRequest(@SerializedName("message_id") val messageId: String)
data class MessageResponse(
    val id: String,
    @SerializedName("channel_id") val channelId: String,
    val author: UserResponse,
    val content: String? = null,
    val type: String = "default",
    val pinned: Boolean = false,
    val tts: Boolean = false,
    val attachments: List<Any>? = null,
    val embeds: List<Any>? = null,
    val reactions: List<ReactionResponse>? = null,
    val reference: MessageResponse? = null,
    @SerializedName("edited_at") val editedAt: String? = null,
    @SerializedName("created_at") val createdAt: String,
)
data class ReactionResponse(
    val emoji: String,
    val count: Int = 1,
    @SerializedName("user_id") val userId: String? = null,
)

data class VoiceTokenRequest(@SerializedName("channelId") val channelId: String)
data class VoiceLeaveRequest(@SerializedName("channelId") val channelId: String)
data class VoiceTokenResponse(
    val token: String,
    @SerializedName("livekit_url") val livekitUrl: String,
    @SerializedName("channel_id") val channelId: String,
)

data class NitroSubscribeRequest(val plan: String)
data class BoostResponse(val success: Boolean, @SerializedName("boost_count") val boostCount: Int, val tier: Int)
data class NitroPlansResponse(val plans: List<NitroPlan>)
data class NitroPlan(val id: String, val name: String, val price: Int, val currency: String)

data class InviteResponse(
    val code: String,
    val guild: GuildResponse,
    val channel: ChannelResponse,
    val uses: Int = 0,
    @SerializedName("max_uses") val maxUses: Int = 0,
    @SerializedName("expires_at") val expiresAt: String? = null,
)
data class CreateInviteRequest(
    @SerializedName("max_uses") val maxUses: Int = 0,
    @SerializedName("max_age") val maxAge: Int = 86400,
)
data class CreatedInviteResponse(
    val code: String,
    @SerializedName("guild_id") val guildId: String,
    @SerializedName("channel_id") val channelId: String,
)
data class GuildInviteResponse(
    val code: String,
    @SerializedName("guild_id") val guildId: String,
    @SerializedName("channel_id") val channelId: String,
    val channel: ChannelResponse? = null,
    val creator: UserResponse? = null,
    val uses: Int = 0,
    @SerializedName("max_uses") val maxUses: Int = 0,
    @SerializedName("max_age") val maxAge: Int = 86400,
    @SerializedName("expires_at") val expiresAt: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
)
