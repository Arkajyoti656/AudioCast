package com.audiocast.app.settings


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@Composable
fun LoadingScreen(navController: NavController) {
    val warmPink = Color(0xFFFFC1CC)

    LaunchedEffect(Unit) {
        delay(1000L)
        navController.navigate("home") {
            popUpTo("loading") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(warmPink),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Tuning your Vibe 🎧🎧",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )
    }
}
