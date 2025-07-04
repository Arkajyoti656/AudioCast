package com.audiocast.app.home


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.audiocast.app.MainActivity
import com.audiocast.app.ui.theme.CalmTextField
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@Composable
fun JoinRoomScreen(navController: NavController) {
    var roomCode by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    val displayName = user?.displayName ?: user?.email?.substringBefore("@") ?: "Anonymous"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Join Room", fontSize = 24.sp, color = Color(0xFF444444))
        CalmTextField(value = roomCode, onValueChange = { roomCode = it }, label = "Enter Room Code")

        if (error.isNotEmpty()) {
            Text(error, color = Color.Red, fontSize = 12.sp)
        }

        Button(
            onClick = {
                val ref = FirebaseDatabase.getInstance().getReference("rooms/${roomCode.uppercase()}")

                // Set pending host false in MainActivity
                (context.applicationContext as? MainActivity)?.let {
                    it.isPendingHost = false
                }

                ref.get().addOnSuccessListener {
                    if (it.exists()) {
                        // Add participant after room exists
                        ref.child("participants").child(user?.uid ?: "unknown").setValue(displayName)

                        navController.navigate("room/${roomCode.uppercase()}")
                    } else {
                        error = "Room not found!"
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEDC9C4)),
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
        ) {
            Text("Join", color = Color.White)
        }
    }
}

