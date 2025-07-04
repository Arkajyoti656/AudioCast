package com.audiocast.app.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun HomeScreen(navController: NavController) {
    val user = FirebaseAuth.getInstance().currentUser

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("AudioCast", fontSize = 22.sp, color = Color(0xFF444444))
            TextButton(onClick = {
                FirebaseAuth.getInstance().signOut()
                navController.navigate("login") {
                    popUpTo("home") { inclusive = true }
                }

            }) {
                Text("Sign Out", color = Color(0xFFEDC9C4))
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
        Text(user?.displayName ?: "User", fontSize = 26.sp, color = Color(0xFF333333))

        Spacer(modifier = Modifier.height(80.dp))
        Button(
            onClick = { navController.navigate("createRoom") },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEDC9C4)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Room", color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { navController.navigate("joinRoom") },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEDC9C4)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Join Room", color = Color.White)
        }
    }
}

