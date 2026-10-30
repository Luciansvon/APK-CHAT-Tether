package com.tether.app.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tether.app.ui.chat.ChatScreen
import com.tether.app.ui.chat.ChatViewModel
import com.tether.app.ui.coupons.CouponsScreen
import com.tether.app.ui.pairing.PairingScreen
import com.tether.app.ui.touchpad.TouchpadScreen

@Composable
fun TetherAppNavHost() {
    val navController = rememberNavController()
    val chatViewModel: ChatViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "chat"
    ) {
        composable("chat") {
            ChatScreen(
                viewModel = chatViewModel,
                onNavigateToTouchpad = { navController.navigate("touchpad") },
                onNavigateToCoupons = { navController.navigate("coupons") },
                onNavigateToPairing = { navController.navigate("pairing") }
            )
        }

        composable("touchpad") {
            TouchpadScreen(
                hapticEngine = chatViewModel.hapticEngine,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("coupons") {
            CouponsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("pairing") {
            PairingScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
