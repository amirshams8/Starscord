package com.nexus.android.data.api

import com.nexus.android.data.api.models.*
import retrofit2.Response
import retrofit2.http.*

interface NexusApi {
    @POST("auth/register")  suspend fun register(@Body body: RegisterRequest): Response<UserResponse>
    @POST("auth/login")     suspend fun login(@Body body: LoginRequest): Response<LoginResponse>
    @POST("auth/logout")    suspend fun logout(): Response<Unit>
    @POST("auth/refresh")   suspend fun refreshToken(): Response<RefreshResponse>
    @POST("auth/mfa/enable") suspend fun enableMfa(): Response<MfaEnableResponse>
    @POST("auth/mfa/verify") suspend fun verifyMfa(@Body body: MfaVerifyRequest): Response<SuccessResponse>

    @GET("users/@me")       suspend fun getMe(): Response<UserResponse>
    @PATCH("users/@me")     suspend fun updateMe(@Body body: UpdateUserRequest): Response<UserResponse>
    @GET("users/{id}")      suspend fun getUser(@Path("id") id: String): Response<UserResponse>
    @GET("users/@me/guilds") suspend fun getMyGuilds(): Response<List<GuildResponse>>

    @POST("guilds")         suspend fun createGuild(@Body body: CreateGuildRequest): Response<GuildResponse>
    @GET("guilds/{id}")     suspend fun getGuild(@Path("id") id: String): Response<GuildResponse>
    @PATCH("guilds/{id}")   suspend fun updateGuild(@Path("id") id: String, @Body body: Map<String, Any>): Response<GuildResponse>
    @DELETE("guilds/{id}")  suspend fun deleteGuild(@Path("id") id: String): Response<Unit>
    @GET("guilds/{id}/members") suspend fun getGuildMembers(@Path("id") id: String, @Query("limit") limit: Int = 100): Response<List<MemberResponse>>
    @PUT("guilds/{id}/members/@me")    suspend fun joinGuild(@Path("id") id: String): Response<MemberResponse>
    @DELETE("guilds/{id}/members/@me") suspend fun leaveGuild(@Path("id") id: String): Response<Unit>

    // NEW: create channel inside a guild
    @POST("guilds/{id}/channels") suspend fun createChannel(@Path("id") guildId: String, @Body body: CreateChannelRequest): Response<ChannelResponse>

    // NEW: generate an invite for a channel
    @POST("channels/{id}/invites") suspend fun createInvite(@Path("id") channelId: String, @Body body: CreateInviteRequest = CreateInviteRequest()): Response<CreatedInviteResponse>

    @GET("channels/{id}")   suspend fun getChannel(@Path("id") id: String): Response<ChannelResponse>
    @PATCH("channels/{id}") suspend fun updateChannel(@Path("id") id: String, @Body body: Map<String, Any>): Response<ChannelResponse>
    @DELETE("channels/{id}") suspend fun deleteChannel(@Path("id") id: String): Response<Unit>

    @GET("channels/{channelId}/messages")
    suspend fun getMessages(@Path("channelId") channelId: String, @Query("limit") limit: Int = 50, @Query("before") before: String? = null, @Query("after") after: String? = null): Response<List<MessageResponse>>

    @POST("channels/{channelId}/messages")
    suspend fun sendMessage(@Path("channelId") channelId: String, @Body body: SendMessageRequest): Response<MessageResponse>

    @PATCH("channels/{channelId}/messages/{messageId}")
    suspend fun editMessage(@Path("channelId") channelId: String, @Path("messageId") messageId: String, @Body body: EditMessageRequest): Response<MessageResponse>

    @DELETE("channels/{channelId}/messages/{messageId}")
    suspend fun deleteMessage(@Path("channelId") channelId: String, @Path("messageId") messageId: String): Response<Unit>

    @PUT("channels/{channelId}/messages/{messageId}/reactions/{emoji}/@me")
    suspend fun addReaction(@Path("channelId") channelId: String, @Path("messageId") messageId: String, @Path("emoji") emoji: String): Response<Unit>

    @DELETE("channels/{channelId}/messages/{messageId}/reactions/{emoji}/@me")
    suspend fun removeReaction(@Path("channelId") channelId: String, @Path("messageId") messageId: String, @Path("emoji") emoji: String): Response<Unit>

    @POST("channels/{channelId}/typing")
    suspend fun sendTyping(@Path("channelId") channelId: String): Response<Unit>

    @POST("voice/token")    suspend fun getVoiceToken(@Body body: VoiceTokenRequest): Response<VoiceTokenResponse>
    @DELETE("voice/leave")  suspend fun leaveVoice(@Body body: VoiceLeaveRequest): Response<Unit>

    @GET("nitro/plans")     suspend fun getNitroPlans(): Response<NitroPlansResponse>
    @POST("nitro/subscribe") suspend fun subscribeNitro(@Body body: NitroSubscribeRequest): Response<SuccessResponse>
    @POST("nitro/boost/{guildId}") suspend fun boostServer(@Path("guildId") guildId: String): Response<BoostResponse>

    @GET("invites/{code}")  suspend fun getInvite(@Path("code") code: String): Response<InviteResponse>
    @POST("invites/{code}/use") suspend fun useInvite(@Path("code") code: String): Response<GuildResponse>
    @DELETE("invites/{code}") suspend fun deleteInvite(@Path("code") code: String): Response<Unit>
}
