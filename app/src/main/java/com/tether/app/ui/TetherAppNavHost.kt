package com.tether.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tether.app.ui.chat.ChatScreen
import com.tether.app.ui.chat.ChatViewModel
import com.tether.app.ui.room.RoomSetupScreen
import com.tether.app.ui.touchpad.TouchpadScreen

@Composable
fun TetherAppNavHost() {
    val navController = rememberNavController()
    val chatViewModel: ChatViewModel = viewModel()
    val isRoomConfigured by chatViewModel.isRoomConfigured.collectAsState()
    val partnerProfile by chatViewModel.partnerProfile.collectAsState()

    val startDestination = if (isRoomConfigured) "chat" else "room_setup"

    LaunchedEffect(isRoomConfigured) {
        if (!isRoomConfigured) {
            navController.navigate("room_setup") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("room_setup") {
            RoomSetupScreen(
                initialMyName = partnerProfile?.myName?.takeIf { it != "Saya" } ?: "",
                initialPartnerName = partnerProfile?.partnerName?.takeIf { it != "Pasangan" } ?: "",
                onJoinRoom = { myName, partnerName, roomCode, passcode ->
                    chatViewModel.setupRoom(myName, partnerName, roomCode, passcode)
                    navController.navigate("chat") {
                        popUpTo("room_setup") { inclusive = true }
                    }
                }
            )
        }

        composable("chat") {
            ChatScreen(
                viewModel = chatViewModel,
                onNavigateToTouchpad = { navController.navigate("touchpad") }
            )
        }

        composable("touchpad") {
            TouchpadScreen(
                partnerName = partnerProfile?.partnerName ?: "Pasangan",
                partnerAvatarUri = partnerProfile?.partnerAvatarUri,
                onTouchPulse = { x, y -> chatViewModel.sendTouchPulse(x, y) },
                onSendTug = { chatViewModel.sendTugSignal(showLocalOverlay = true) },
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
