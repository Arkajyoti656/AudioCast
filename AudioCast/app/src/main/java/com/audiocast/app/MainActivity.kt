package com.audiocast.app

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.audiocast.app.auth.LoginScreen
import com.audiocast.app.auth.RegisterScreen
import com.audiocast.app.home.CreateRoomScreen
import com.audiocast.app.home.HomeScreen
import com.audiocast.app.home.JoinRoomScreen
import com.audiocast.app.home.RoomScreen
import com.audiocast.app.settings.ChatRoomScreen
import com.audiocast.app.settings.LoadingScreen
import com.audiocast.app.settings.RoomViewModel
import com.audiocast.app.ui.theme.AudioCastTheme
import com.audiocast.app.utils.SplashScreen
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var mediaProjectionLauncher: ActivityResultLauncher<Intent>

    private var pendingRoomCode: String? = null
    internal var isPendingHost: Boolean = false
    internal var mediaProjectionResultIntent: Intent? = null

    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        // Register result launcher for MediaProjection
        mediaProjectionLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK && result.data != null) {
                    mediaProjectionResultIntent = result.data
                    pendingRoomCode?.let { roomCode ->
                        navController?.navigate("room/$roomCode")

                    }
                }
            }

        setContent {
            val navController = rememberNavController()
            this.navController = navController

            AudioCastTheme {
                NavHost(navController, startDestination = "splash") {
                    composable("login") { LoginScreen(navController) }
                    composable("register") { RegisterScreen(navController) }
                    composable("loading") { LoadingScreen(navController) }
                    composable("home") { HomeScreen(navController) }
                    composable("createRoom") { CreateRoomScreen(navController) }
                    composable("splash") { SplashScreen(navController) }
                    composable("joinRoom") { JoinRoomScreen(navController) }

                    composable(
                        "room/{roomCode}",
                        arguments = listOf(navArgument("roomCode") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val roomCode = backStackEntry.arguments?.getString("roomCode") ?: ""
                        val viewModel: RoomViewModel = viewModel()
                        val user = FirebaseAuth.getInstance().currentUser
                        val userName = user?.displayName ?: "Anonymous"

                        LaunchedEffect(roomCode) {
                            viewModel.startListening(roomCode)
                        }

                        DisposableEffect(Unit) {
                            onDispose {
                                viewModel.stopListening(roomCode)
                            }
                        }

                        RoomScreen(
                            navController = navController,
                            roomCode = roomCode,
                            userName = userName,
                            isHost = isPendingHost,
                            mediaProjectionPermissionIntent = mediaProjectionResultIntent,
                            onRequestMediaProjection = {
                                requestMediaProjectionPermission(roomCode, isPendingHost)
                            }
                        )
                    }

                    // Updated ChatRoomScreen route
                    composable(
                        "chat/{roomCode}/{userName}",
                        arguments = listOf(
                            navArgument("roomCode") { type = NavType.StringType },
                            navArgument("userName") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val roomCode = backStackEntry.arguments?.getString("roomCode") ?: ""
                        val userName = backStackEntry.arguments?.getString("userName") ?: "Anonymous"
                        val viewModel: RoomViewModel = viewModel()

                        LaunchedEffect(roomCode) {
                            viewModel.startListening(roomCode)
                        }

                        DisposableEffect(Unit) {
                            onDispose {
                                viewModel.stopListening(roomCode)
                            }
                        }

                        ChatRoomScreen(
                            navController = navController,
                            roomCode = roomCode,
                            userName = userName,
                            messages = viewModel.messages,
                            onSendMessage = { text ->
                                viewModel.sendMessage(roomCode, userName, text)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun requestMediaProjectionPermission(roomCode: String, isHost: Boolean) {
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        isPendingHost = isHost
        pendingRoomCode = roomCode
        mediaProjectionLauncher.launch(intent)
    }
}
