package com.audiocast.app.audio

import android.app.*
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Base64
import android.util.Log
import com.audiocast.app.R
import com.google.firebase.database.FirebaseDatabase

class AudioCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null
    private var captureThread: Thread? = null
    private var roomCode: String? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED)
        val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra("data", Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra("data")
        }

        // ✅ GET roomCode from intent
        roomCode = intent?.getStringExtra("roomCode")

        if (resultCode == null || data == null || roomCode.isNullOrEmpty()) {
            Log.e("AudioCaptureService", "Missing resultCode, data, or roomCode in intent.")
            stopSelf()
            return START_NOT_STICKY
        }

        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)

        if (mediaProjection == null) {
            Log.e("AudioCaptureService", "MediaProjection is null.")
            stopSelf()
            return START_NOT_STICKY
        }

        Log.d("AudioCaptureService", "MediaProjection started successfully.")
        startAudioCapture()

        return START_STICKY
    }

    private fun startAudioCapture() {
        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(44100)
            .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
            .build()

        val bufferSize = AudioRecord.getMinBufferSize(
            44100,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord.Builder()
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSize)
            .setAudioPlaybackCaptureConfig(config)
            .build()

        audioRecord?.startRecording()

        val firebaseRef = FirebaseDatabase.getInstance()
            .getReference("rooms/${roomCode}/audioChunks")

        captureThread = Thread {
            val buffer = ByteArray(bufferSize)
            while (!Thread.interrupted()) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    Log.d("AudioCaptureService", "Captured $read bytes")
                    val encoded = Base64.encodeToString(buffer.copyOf(read), Base64.NO_WRAP)
                    firebaseRef.push().setValue(encoded)
                }
            }
        }
        captureThread?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        captureThread?.interrupt()
        audioRecord?.stop()
        audioRecord?.release()
        mediaProjection?.stop()
        Log.d("AudioCaptureService", "Audio capture stopped.")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundService() {
        val notification = Notification.Builder(this, createNotificationChannel())
            .setContentTitle("VibeCast is Live")
            .setContentText("Sharing device audio...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)
    }

    private fun createNotificationChannel(): String {
        val channelId = "VibeCastAudioChannel"
        val channelName = "Audio Streaming"

        val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(chan)

        return channelId
    }
}
