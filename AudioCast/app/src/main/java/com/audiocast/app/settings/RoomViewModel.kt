package com.audiocast.app.settings

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.database.*

class RoomViewModel : ViewModel() {

    val isSynced = mutableStateOf(false)

    private val _messages = mutableStateListOf<Message>()
    val messages: List<Message> = _messages

    private var messagesListener: ValueEventListener? = null

    fun setSynced(state: Boolean) {
        isSynced.value = state
    }

    fun startListening(roomCode: String) {
        val messagesRef = FirebaseDatabase.getInstance()
            .getReference("rooms/$roomCode/messages")

        messagesListener = messagesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _messages.clear()
                snapshot.children.mapNotNullTo(_messages) {
                    it.getValue(Message::class.java)
                }
                _messages.sortBy { it.timestamp }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error if needed
            }
        })
    }

    fun sendMessage(roomCode: String, sender: String, text: String) {
        val messagesRef = FirebaseDatabase.getInstance()
            .getReference("rooms/$roomCode/messages")

        val message = Message(user = sender, text = text, timestamp = System.currentTimeMillis())
        messagesRef.push().setValue(message)
    }

    fun stopListening(roomCode: String) {
        val messagesRef = FirebaseDatabase.getInstance()
            .getReference("rooms/$roomCode/messages")

        messagesListener?.let {
            messagesRef.removeEventListener(it)
        }
    }
}
