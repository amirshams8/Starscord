package com.nexus.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nexus.android.ui.screens.auth.LoginScreen
import com.nexus.android.ui.screens.auth.RegisterScreen
import com.nexus.android.ui.screens.chat.ChatScreen
import com.nexus.android.ui.screens.home.HomeScreen

sealed class Screen(val route: String) {
    object Login    : Screen("login")
    object Register : Screen("register")
    object Home     : Screen("home")
    object Chat     : Screen("chat/{channelId}/{guildId}") {
        fun createRoute(c: String, g: String) = "chat/$c/$g"
    }
}

@Composable
fun NexusNavGraph(startDestination: String = Screen.Login.route) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = startDestination) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess       = { nav.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } } },
                onNavigateToRegister = { nav.navigate(Screen.Register.route) }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = { nav.navigate(Screen.Login.route) { popUpTo(Screen.Register.route) { inclusive = true } } },
                onNavigateToLogin = { nav.popBackStack() }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onOpenChannel = { channelId, guildId -> nav.navigate(Screen.Chat.createRoute(channelId, guildId)) },
                onOpenVoice   = { /* TODO: voice screen */ }
            )
        }
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("channelId") { type = NavType.StringType },
                navArgument("guildId")   { type = NavType.StringType },
            )
        ) { back ->
            ChatScreen(
                channelId = back.arguments?.getString("channelId") ?: return@composable,
                guildId   = back.arguments?.getString("guildId")   ?: return@composable,
                onBack    = { nav.popBackStack() }
            )
        }
    }
}
