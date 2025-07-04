package com.audiocast.app.utils

import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

open class SdpObserverAdapter : SdpObserver {
    override fun onCreateSuccess(sessionDescription: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) {}
    override fun onSetFailure(error: String?) {}
}
