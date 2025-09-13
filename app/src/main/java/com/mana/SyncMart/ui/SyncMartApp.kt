package com.mana.SyncMart.ui

import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mana.SyncMart.ui.auth.LoginScreen
import com.mana.SyncMart.ui.auth.SignupScreen
import com.mana.SyncMart.ui.auth.ForgotPasswordScreen
import com.mana.SyncMart.ui.home.HomeScreen
import com.mana.SyncMart.ui.home.ListDetailScreen
import com.mana.SyncMart.ui.friends.FriendsScreen
import com.mana.SyncMart.ui.friends.SharedListScreen
import com.mana.SyncMart.viewmodel.AuthViewModel

@Composable
fun SyncMartApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    // Determine start destination based on auth state
    val startDestination = if (authViewModel.isLoggedIn) "home" else "login"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 🔑 Authentication Routes
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToSignup = { navController.navigate("signup") },
                onForgotPassword = { navController.navigate("forgotPassword") },
                authViewModel = authViewModel
            )
        }

        composable("signup") {
            SignupScreen(
                onSignupSuccess = {
                    navController.navigate("home") {
                        popUpTo("signup") { inclusive = true }
                    }
                },
                onBackToLogin = { navController.popBackStack() },
                authViewModel = authViewModel
            )
        }

        composable("forgotPassword") {
            ForgotPasswordScreen(
                onPasswordReset = { navController.popBackStack() },
                authViewModel = authViewModel
            )
        }

        // 🏠 Main App Routes
        composable("home") {
            HomeScreen(
                onNavigateToListDetail = { listId ->
                    navController.navigate("listDetail/$listId")
                },
                onNavigateToFriends = {
                    navController.navigate("friends")
                },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // 📋 List details (with listId param)
        composable(
            route = "listDetail/{listId}",
            arguments = listOf(
                navArgument("listId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getString("listId") ?: ""
            ListDetailScreen(
                listId = listId,
                onBack = { navController.popBackStack() }
            )
        }

        // 👥 Friends Routes
        composable("friends") {
            FriendsScreen(
                onSharedListClick = { navController.navigate("sharedLists") },
                onBack = { navController.popBackStack() }
            )
        }

        composable("sharedLists") {
            SharedListScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}