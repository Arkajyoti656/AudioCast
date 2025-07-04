package com.audiocast.app.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.audiocast.app.ui.theme.CalmTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    navController: NavController,
    roomCode: String,
    userName: String,
    messages: List<Message>,
    onSendMessage: (String) -> Unit
) {
    var newMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDF6F3))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "← Back",
                color = Color.Blue,
                modifier = Modifier
                    .clickable { navController.popBackStack() }
                    .padding(horizontal = 8.dp)
            )

            Text(
                text = "Chat Room: $roomCode",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFFB35C5C),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp),
            reverseLayout = true
        ) {
            items(messages.reversed()) { msg ->
                val isCurrentUser = msg.user == userName
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isCurrentUser) Color(0xFFD6E4FF) else Color(0xFFFFE4E1),
                        modifier = Modifier.widthIn(0.dp, 280.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = msg.user,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CalmTextField(
                value = newMessage,
                onValueChange = { newMessage = it },
                label = "Type your message",
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (newMessage.isNotBlank()) {
                        onSendMessage(newMessage.trim())
                        newMessage = ""
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEDC9C4))
            ) {
                Text("Send")
            }
        }
    }
}
