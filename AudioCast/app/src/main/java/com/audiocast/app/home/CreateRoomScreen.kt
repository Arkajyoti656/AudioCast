package com.audiocast.app.home

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.audiocast.app.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

fun generateRoomCode(length: Int = 6): String {
    val chars = ('A'..'Z') + ('0'..'9')
    return (1..length)
        .map { chars.random() }
        .joinToString("")
}

@Composable
fun CreateRoomScreen(navController: NavController) {
    val context = LocalContext.current
    val roomCode = remember { mutableStateOf(generateRoomCode().uppercase()) }
    val user = FirebaseAuth.getInstance().currentUser
    val isCreating = remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val roomRef = FirebaseDatabase.getInstance().getReference("rooms/${roomCode.value}")
        val displayName = user?.displayName ?: user?.email?.substringBefore("@") ?: "Anonymous"

        val roomData = mapOf(
            "host" to displayName,
            "createdAt" to System.currentTimeMillis()
        )

        // Set flag for pending host in MainActivity
        (context.applicationContext as? MainActivity)?.let {
            it.isPendingHost = true
        }

        roomRef.setValue(roomData)
            .addOnSuccessListener {
                // Add host as participant
                val participantsRef = roomRef.child("participants").child(user?.uid ?: "unknown")
                participantsRef.setValue(displayName)

                isCreating.value = false
                navController.navigate("room/${roomCode.value}")
            }
            .addOnFailureListener { exception ->
                isCreating.value = false
                Toast.makeText(context, "Failed to create room: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (isCreating.value) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color(0xFFEDC9C4))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Creating Room...", color = Color.DarkGray)
            }
        }
    }
}