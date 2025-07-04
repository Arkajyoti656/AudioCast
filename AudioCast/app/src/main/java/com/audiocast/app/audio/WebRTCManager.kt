package com.audiocast.app.audio

import android.content.Context
import android.media.projection.MediaProjectionManager
import android.util.Log
import org.webrtc.*
import com.audiocast.app.utils.SdpObserverAdapter
import com.google.firebase.database.*


class WebRTCManager(
    private val context: Context,
    private val roomCode: String,
    private val isHost: Boolean,
    private val firebaseDatabase: FirebaseDatabase,
    private val mediaProjectionManager: MediaProjectionManager
) {

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var signalingRef: DatabaseReference? = null

    fun initConnection() {
        initializePeerConnectionFactory()
        createPeerConnection()

        signalingRef = firebaseDatabase.getReference("rooms/$roomCode/signaling")

        if (isHost) {
            createAndSendOffer()
        } else {
            listenForOfferAndAnswer()
        }

        listenForIceCandidates()
    }

    private fun initializePeerConnectionFactory() {
        val initializationOptions = PeerConnectionFactory.InitializationOptions.builder(context)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(initializationOptions)

        val options = PeerConnectionFactory.Options()
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .createPeerConnectionFactory()
    }

    private fun createPeerConnection() {
        val rtcConfig = PeerConnection.RTCConfiguration(emptyList())
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN

        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    signalingRef?.child("candidates")?.push()?.setValue(
                        mapOf(
                            "sdpMid" to it.sdpMid,
                            "sdpMLineIndex" to it.sdpMLineIndex,
                            "candidate" to it.sdp
                        )
                    )
                }
            }

            override fun onAddStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {}
            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {}
            override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {}
            override fun onSignalingChange(newState: PeerConnection.SignalingState?) {}
            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {}
            override fun onStandardizedIceConnectionChange(newState: PeerConnection.IceConnectionState?) {}
            override fun onSelectedCandidatePairChanged(event: CandidatePairChangeEvent?) {}
            override fun onTrack(transceiver: RtpTransceiver?) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onRenegotiationNeeded() {
                Log.d("WebRTC", "Renegotiation needed")
            }
            override fun onRemoveStream(stream: MediaStream?) {}
        })

        val audioConstraints = MediaConstraints()
        audioSource = peerConnectionFactory?.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory?.createAudioTrack("101", audioSource)

        localAudioTrack?.let { track ->
            peerConnection?.addTrack(track)
        }
    }

    private fun createAndSendOffer() {
        val mediaConstraints = MediaConstraints()
        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))

        peerConnection?.createOffer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserverAdapter() {
                    override fun onSetSuccess() {
                        Log.d("WebRTC", "Offer set locally")
                    }

                    override fun onSetFailure(p0: String?) {
                        Log.e("WebRTC", "Failed to set local offer: $p0")
                    }
                }, sessionDescription)

                val offer = mapOf(
                    "sdp" to sessionDescription?.description,
                    "type" to sessionDescription?.type?.canonicalForm()
                )
                signalingRef?.child("offer")?.setValue(offer)
            }

            override fun onCreateFailure(p0: String?) {
                Log.e("WebRTC", "Offer creation failed: $p0")
            }
        }, mediaConstraints)
    }

    private fun listenForOfferAndAnswer() {
        signalingRef?.child("offer")?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val offerMap = snapshot.value as? Map<*, *> ?: return
                val sdp = offerMap["sdp"] as? String ?: return
                val type = offerMap["type"] as? String ?: return
                val offer = SessionDescription(SessionDescription.Type.fromCanonicalForm(type), sdp)

                peerConnection?.setRemoteDescription(object : SdpObserverAdapter() {
                    override fun onSetSuccess() {
                        Log.d("WebRTC", "Remote offer set. Creating answer...")
                        createAndSendAnswer()
                    }

                    override fun onSetFailure(p0: String?) {
                        Log.e("WebRTC", "Failed to set remote offer: $p0")
                    }
                }, offer)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("WebRTC", "Offer listener cancelled: ${error.message}")
            }
        })
    }

    private fun createAndSendAnswer() {
        val mediaConstraints = MediaConstraints()
        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))

        peerConnection?.createAnswer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserverAdapter() {
                    override fun onSetSuccess() {
                        Log.d("WebRTC", "Answer set locally")
                    }

                    override fun onSetFailure(p0: String?) {
                        Log.e("WebRTC", "Failed to set local answer: $p0")
                    }
                }, sessionDescription)

                val answer = mapOf(
                    "sdp" to sessionDescription?.description,
                    "type" to sessionDescription?.type?.canonicalForm()
                )
                signalingRef?.child("answer")?.setValue(answer)
            }

            override fun onCreateFailure(p0: String?) {
                Log.e("WebRTC", "Answer creation failed: $p0")
            }
        }, mediaConstraints)
    }

    private fun listenForIceCandidates() {
        signalingRef?.child("candidates")?.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val map = snapshot.value as? Map<*, *> ?: return
                val sdpMid = map["sdpMid"] as? String ?: return
                val sdpMLineIndex = (map["sdpMLineIndex"] as? Long)?.toInt() ?: return
                val candidate = map["candidate"] as? String ?: return
                peerConnection?.addIceCandidate(IceCandidate(sdpMid, sdpMLineIndex, candidate))
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("WebRTC", "ICE candidate listener cancelled: ${error.message}")
            }
        })
    }

    fun release() {
        peerConnection?.close()
        audioSource?.dispose()
        localAudioTrack?.dispose()
        peerConnectionFactory?.dispose()
    }
}

