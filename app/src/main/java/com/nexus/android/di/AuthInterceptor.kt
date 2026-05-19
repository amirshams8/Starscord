package com.nexus.android.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    @ApplicationContext private val context: Context
) : Interceptor {

    val prefs: SharedPreferences by lazy {
        val mk = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "nexus_secure_prefs",
            mk,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // Access token — attached as Bearer on every request
    var accessToken: String?
        get()  = prefs.getString("access_token", null)
        set(v) = if (v == null) prefs.edit().remove("access_token").apply()
                 else           prefs.edit().putString("access_token", v).apply()

    // Refresh token — stored at login time, sent as Cookie on /auth/refresh
    var refreshToken: String?
        get()  = prefs.getString("refresh_token", null)
        set(v) = if (v == null) prefs.edit().remove("refresh_token").apply()
                 else           prefs.edit().putString("refresh_token", v).apply()

    // Clear both tokens (called on logout)
    fun clearTokens() {
        prefs.edit().remove("access_token").remove("refresh_token").apply()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = accessToken ?: return chain.proceed(chain.request())
        return chain.proceed(
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        )
    }
}
