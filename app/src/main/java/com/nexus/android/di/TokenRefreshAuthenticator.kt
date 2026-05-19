package com.nexus.android.di

import android.util.Log
import com.nexus.android.BuildConfig
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Route
import okhttp3.Response
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp Authenticator — fires automatically on any 401 response.
 *
 * Flow:
 *  1. Read the stored refresh token from AuthInterceptor prefs.
 *  2. POST /v1/auth/refresh with Cookie: refresh_token=<value>
 *  3. On success  → save new access token, retry original request once.
 *  4. On failure  → return null (OkHttp cancels the call; app stays on
 *                   whatever screen it was — the ViewModel error handler
 *                   will surface "Not logged in" to the user).
 *
 * The @Singleton scope + `@volatile` flag prevent concurrent refreshes from
 * hammering the backend when multiple parallel calls all get 401 at once.
 */
@Singleton
class TokenRefreshAuthenticator @Inject constructor(
    private val authInterceptor: AuthInterceptor
) : Authenticator {

    @Volatile private var isRefreshing = false

    override fun authenticate(route: Route?, response: Response): Request? {
        // Don't retry refresh calls themselves — prevents infinite loops
        if (response.request.url.encodedPath.contains("/auth/refresh")) return null

        // Only one thread should refresh at a time
        synchronized(this) {
            if (isRefreshing) return null
            isRefreshing = true
        }

        return try {
            val refreshToken = authInterceptor.refreshToken ?: run {
                Log.w("TokenRefresh", "No refresh token stored — user must log in again")
                return null
            }

            val refreshUrl = "${BuildConfig.API_BASE_URL}/auth/refresh"
            Log.d("TokenRefresh", "Access token expired — attempting refresh")

            // Build a plain OkHttp call (NOT Retrofit — avoids dependency cycles with NetworkModule)
            val refreshClient = OkHttpClient()
            val body = "{}".toRequestBody("application/json".toMediaType())
            val refreshRequest = Request.Builder()
                .url(refreshUrl)
                .post(body)
                .header("Cookie", "refresh_token=$refreshToken")
                .build()

            val refreshResponse = refreshClient.newCall(refreshRequest).execute()
            if (!refreshResponse.isSuccessful) {
                Log.w("TokenRefresh", "Refresh failed: ${refreshResponse.code}")
                // Wipe both tokens so NexusNavGraph reroutes to Login on next launch
                authInterceptor.clearTokens()
                return null
            }

            val newToken = refreshResponse.body?.string()?.let {
                runCatching { JSONObject(it).getString("access_token") }.getOrNull()
            }

            if (newToken == null) {
                Log.w("TokenRefresh", "Refresh response had no access_token")
                authInterceptor.clearTokens()
                return null
            }

            Log.d("TokenRefresh", "Token refreshed successfully")
            authInterceptor.accessToken = newToken

            // Retry the original request with the fresh token
            response.request.newBuilder()
                .header("Authorization", "Bearer $newToken")
                .build()

        } catch (e: Exception) {
            Log.e("TokenRefresh", "Exception during token refresh", e)
            null
        } finally {
            isRefreshing = false
        }
    }
}
