package com.audiocast.app.auth

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.NavHostController
import com.google.firebase.auth.userProfileChangeRequest

@Composable
fun RegisterScreen(navController: NavHostController) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to AudioCast",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFDAA5A4),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        TextField(value = displayName, onValueChange = { displayName = it }, label = { Text("User Name") })
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation())
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    val profileUpdates = userProfileChangeRequest {
                        setDisplayName(displayName)
                    }
                    FirebaseAuth.getInstance().currentUser?.updateProfile(profileUpdates)
                        ?.addOnSuccessListener {
                            navController.navigate("loading")
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Registration Failed", Toast.LENGTH_SHORT).show()
                }
        }) {
            Text("Register")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Already have an account? Login",
            color = Color.Gray,
            modifier = Modifier.clickable { navController.navigate("login") }
        )
    }
}