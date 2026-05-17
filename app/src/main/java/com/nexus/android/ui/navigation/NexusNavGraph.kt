package com.nexus.android.ui.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nexus.android.ui.screens.auth.LoginScreen
import com.nexus.android.ui.screens.auth.RegisterScreen
import com.nexus.android.ui.screens.chat.ChatScreen
import com.nexus.android.ui.screens.dm.DmListScreen
import com.nexus.android.ui.screens.home.HomeScreen
import com.nexus.android.ui.screens.profile.ProfileScreen
import com.nexus.android.ui.screens.server.ChannelMembersScreen
import com.nexus.android.ui.screens.server.ServerSettingsScreen
import com.nexus.android.ui.screens.settings.SettingsScreen
import com.nexus.android.ui.screens.voice.VoiceScreen

sealed class Screen(val route: String) {
    object Login          : Screen("login")
    object Register       : Screen("register")
    object Home           : Screen("home")
    object Profile        : Screen("profile")
    object Settings       : Screen("settings")
    object DmList         : Screen("dm_list")
    object DmChat         : Screen("dm_chat/{channelId}") {
        fun createRoute(c: String) = "dm_chat/$c"
    }
    object Chat           : Screen("chat/{channelId}/{guildId}") {
        fun createRoute(c: String, g: String) = "chat/$c/$g"
    }
    object Voice          : Screen("voice/{channelId}/{channelName}") {
        fun createRoute(c: String, n: String) = "voice/$c/${n.replace("/", "-")}"
    }
    object ServerSettings : Screen("server_settings/{guildId}") {
        fun createRoute(g: String) = "server_settings/$g"
    }
    object ChannelMembers : Screen("channel_members/{channelId}/{guildId}") {
        fun createRoute(c: String, g: String) = "channel_members/$c/$g"
    }
}

// FIX: Read stored access token from EncryptedSharedPreferences to decide start destination.
// Token is written by AuthInterceptor under key "access_token" in "nexus_secure_prefs".
private fun hasStoredToken(context: Context): Boolean {
    return try {
        val masterKey = androidx.security.crypto.MasterKey.Builder(context)
            .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
            .build()
        val prefs = androidx.security.crypto.EncryptedSharedPreferences.create(
            context,
            "nexus_secure_prefs",
            masterKey,
            androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
        prefs.getString("access_token", null) != null
    } catch (_: Exception) { false }
}

@Composable
fun NexusNavGraph() {
    val context = LocalContext.current
    // FIX: start at Home if token exists, Login otherwise — no more login prompt on every restart
    val startDestination = if (hasStoredToken(context)) Screen.Home.route else Screen.Login.route
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = startDestination) {

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess       = { nav.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } } },
                onNavigateToRegister = { nav.navigate(Screen.Register.route) },
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = { nav.navigate(Screen.Login.route) { popUpTo(Screen.Register.route) { inclusive = true } } },
                onNavigateToLogin = { nav.popBackStack() },
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onOpenChannel        = { cId, gId -> nav.navigate(Screen.Chat.createRoute(cId, gId)) },
                onOpenVoice          = { cId, name -> nav.navigate(Screen.Voice.createRoute(cId, name)) },
                onOpenProfile        = { nav.navigate(Screen.Profile.route) },
                onOpenSettings       = { nav.navigate(Screen.Settings.route) },
                onOpenServerSettings = { gId -> nav.navigate(Screen.ServerSettings.createRoute(gId)) },
                onOpenDms            = { nav.navigate(Screen.DmList.route) },
            )
        }

        composable(Screen.DmList.route) {
            DmListScreen(
                onOpenDm = { cId -> nav.navigate(Screen.DmChat.createRoute(cId)) },
                onBack   = { nav.popBackStack() },
            )
        }

        composable(
            route     = Screen.DmChat.route,
            arguments = listOf(navArgument("channelId") { type = NavType.StringType }),
        ) { back ->
            ChatScreen(
                channelId     = back.arguments?.getString("channelId") ?: return@composable,
                guildId       = "dm",
                onBack        = { nav.popBackStack() },
                onOpenMembers = { _, _ -> },
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(onBack = { nav.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack   = { nav.popBackStack() },
                onLogout = { nav.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } },
            )
        }

        composable(
            route     = Screen.Chat.route,
            arguments = listOf(
                navArgument("channelId") { type = NavType.StringType },
                navArgument("guildId")   { type = NavType.StringType },
            ),
        ) { back ->
            ChatScreen(
                channelId     = back.arguments?.getString("channelId") ?: return@composable,
                guildId       = back.arguments?.getString("guildId") ?: return@composable,
                onBack        = { nav.popBackStack() },
                onOpenMembers = { cId, gId -> nav.navigate(Screen.ChannelMembers.createRoute(cId, gId)) },
            )
        }

        composable(
            route     = Screen.Voice.route,
            arguments = listOf(
                navArgument("channelId")   { type = NavType.StringType },
                navArgument("channelName") { type = NavType.StringType },
            ),
        ) { back ->
            VoiceScreen(
                channelId   = back.arguments?.getString("channelId") ?: return@composable,
                channelName = back.arguments?.getString("channelName") ?: "Voice",
                onLeave     = { nav.popBackStack() },
            )
        }

        composable(
            route     = Screen.ServerSettings.route,
            arguments = listOf(navArgument("guildId") { type = NavType.StringType }),
        ) { back ->
            ServerSettingsScreen(
                guildId = back.arguments?.getString("guildId") ?: return@composable,
                onBack  = { nav.popBackStack() },
            )
        }

        composable(
            route     = Screen.ChannelMembers.route,
            arguments = listOf(
                navArgument("channelId") { type = NavType.StringType },
                navArgument("guildId")   { type = NavType.StringType },
            ),
        ) { back ->
            ChannelMembersScreen(
                channelId = back.arguments?.getString("channelId") ?: return@composable,
                guildId   = back.arguments?.getString("guildId") ?: return@composable,
                onBack    = { nav.popBackStack() },
            )
        }
    }
}
