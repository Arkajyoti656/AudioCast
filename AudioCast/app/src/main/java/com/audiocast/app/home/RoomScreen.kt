package com.audiocast.app.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com. google. firebase. database. ChildEventListener
import androidx.compose.ui.Alignment
import android.media.projection.MediaProjectionManager
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.database.ValueEventListener
import androidx.navigation.NavController
import com.audiocast.app.audio.WebRTCManager
import com.audiocast.app.MainActivity
import com.audiocast.app.audio.AudioCaptureService
import com.audiocast.app.settings.AudioFocusHelper
import com.audiocast.app.settings.RoomViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.jvm.java


@Composable
fun RoomScreen(
    navController: NavController,
    onRequestMediaProjection: () -> Unit,
    roomCode: String,
    userName: String,
    isHost: Boolean,
    mediaProjectionPermissionIntent: Intent?
) {
    val context = LocalContext.current
    val participants = remember { mutableStateListOf<String>() }
    val participantsRef = FirebaseDatabase.getInstance()
        .getReference("rooms/$roomCode/participants")
    val previousParticipants = remember { mutableStateListOf<String>() }

    val audioFocusHelper = remember {
        AudioFocusHelper(context) {
            Toast.makeText(
                context,
                "🎵 Stream paused because another media app started.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val mediaProjectionManager = remember {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }
    val firebaseDatabase = FirebaseDatabase.getInstance()
    val webRtcManager = remember {
        WebRTCManager(
            context = context,
            roomCode = roomCode,
            isHost = isHost,
            firebaseDatabase = firebaseDatabase,
            mediaProjectionManager = mediaProjectionManager
        )
    }

    val viewModel: RoomViewModel = viewModel()
    val synced = viewModel.isSynced

    val audioTrackRef = remember { mutableStateOf<AudioTrack?>(null) }
    val audioBufferQueue = remember { LinkedBlockingQueue<ByteArray>() }

    // Participant-side buffered audio receiving
    if (!isHost) {
        LaunchedEffect(Unit) {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(44100)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(
                    AudioTrack.getMinBufferSize(
                        44100,
                        AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT
                    )
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build().apply { play() }

            audioTrackRef.value = audioTrack

            val audioRef = FirebaseDatabase.getInstance()
                .getReference("rooms/$roomCode/audioChunks")

            audioRef.addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val base64Data = snapshot.getValue(String::class.java)
                    val pcmData = Base64.decode(base64Data, Base64.NO_WRAP)
                    audioBufferQueue.offer(pcmData)
                }

                override fun onCancelled(error: DatabaseError) {}
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            })

            // Continuous playback from buffer
            withContext(Dispatchers.IO) {
                while (true) {
                    val chunk = audioBufferQueue.poll(100, TimeUnit.MILLISECONDS)
                    chunk?.let { audioTrack.write(it, 0, it.size) }
                }
            }
        }
    }

    // Participant list updates
    LaunchedEffect(roomCode) {
        participantsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newList = snapshot.children.mapNotNull { it.getValue(String::class.java) }
                val left = previousParticipants.filterNot { it in newList }
                if (left.isNotEmpty()) {
                    left.forEach {
                        Toast.makeText(context, "❌ $it left the room", Toast.LENGTH_SHORT).show()
                    }
                }
                previousParticipants.clear()
                previousParticipants.addAll(newList)
                participants.clear()
                participants.addAll(newList)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // Media projection request for host
    LaunchedEffect(Unit) {
        if (!isHost) audioFocusHelper.requestFocus()
        if (isHost && mediaProjectionPermissionIntent == null) onRequestMediaProjection()
    }

    LaunchedEffect(isHost, mediaProjectionPermissionIntent) {
        if (isHost && mediaProjectionPermissionIntent == null) {
            onRequestMediaProjection()
        }
    }

    // Start AudioCaptureService and WebRTC on permission
    LaunchedEffect(mediaProjectionPermissionIntent) {
        if (mediaProjectionPermissionIntent != null || !isHost) {
            webRtcManager.initConnection()
            if (isHost) {
                val serviceIntent = Intent(context, AudioCaptureService::class.java).apply {
                    putExtra("resultCode", Activity.RESULT_OK)
                    putExtra("data", mediaProjectionPermissionIntent)
                    putExtra("roomCode", roomCode)
                }
                ContextCompat.startForegroundService(context, serviceIntent)

                (context as? MainActivity)?.mediaProjectionResultIntent = null
                Toast.makeText(context, "🎧 Now broadcasting", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Release on exit
    DisposableEffect(Unit) {
        onDispose {
            webRtcManager.release()
            if (!isHost) {
                audioTrackRef.value?.stop()
                audioTrackRef.value?.release()
                audioFocusHelper.releaseFocus()
            }
        }
    }

    // UI
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFDF6F3))) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (!isHost) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (synced.value) Color(0xFFE57373) else Color(0xFFFFC107),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                if (!synced.value) {
                                    webRtcManager.release()
                                    webRtcManager.initConnection()
                                    Toast.makeText(context, "🔄 Syncing to live stream...", Toast.LENGTH_SHORT).show()
                                    synced.value = true
                                    viewModel.setSynced(true)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (synced.value) "LIVE" else "SYNC",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "Room Code: $roomCode",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFB35C5C),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                TextButton(onClick = { navController.navigate("home") }) {
                    Text("Exit", color = Color.Red)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "You: $userName",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Participants:", fontWeight = FontWeight.Bold)
            Column(modifier = Modifier.padding(start = 8.dp)) {
                participants.forEach { name ->
                    Text("• $name", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    navController.navigate("chat/$roomCode/$userName")
                },
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEDC9C4))
            ) {
                Text("Open Chat")
            }
        }
    }
}
