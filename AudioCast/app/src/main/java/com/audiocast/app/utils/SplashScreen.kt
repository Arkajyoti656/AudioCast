package com.audiocast.app.utils

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay


@Composable
fun SplashScreen(navController: NavHostController) {
    val currentUser = FirebaseAuth.getInstance().currentUser

    LaunchedEffect(Unit) {
        delay(500L) // optional small delay
        if (currentUser != null) {
            navController.navigate("loading") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("AudioCast", fontSize = 28.sp, color = Color(0xFFDAA5A4))
    }
}
