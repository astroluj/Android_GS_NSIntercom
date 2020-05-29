package com.neosecu.intercom

import android.content.Context
import android.util.Log
import android.view.Surface
import org.freedesktop.gstreamer.GStreamer

class NSIntercom() {
    companion object {
        // so file load
        init {
            System.loadLibrary("gstreamer_rtp")
            System.loadLibrary("gstreamer_android")
            nativeClassInit()
        }

        // static variable
        private var nsIntercom: NSIntercom? = null

        @JvmStatic
        fun getInstance(): NSIntercom {
            if (nsIntercom == null) {
                nsIntercom = NSIntercom()
            }

            return nsIntercom!!
        }

        // gstreamer 클래스 사용
        @JvmStatic
        private external fun nativeClassInit(): Boolean // Initialize native class: cache Method IDs for callbacks
    }

    // 연결
    private external fun nativeInitPlay(sendVideoPipeline: String?, recvVideoPipeline: String?,
                                    sendAudioPipeline: String?, recvAudioPipeline: String?) // Initialize native code, build pipeline, etc
    // 종료
    private external fun nativeFinalize() // Destroy pipeline and shutdown native code
    private external fun nativeResume() // Set pipeline to PLAYING
    private external fun nativePause() // Set pipeline to PAUSED
    // 서페이스 연결
    private external fun nativeSurfaceInit(surface: Any) // A new surface is available
    // 서페이스 종료
    private external fun nativeSurfaceFinalize() // Surface about to be destroyed
    @JvmField var native_custom_data = 0L // Native code will use this to keep private data

    // audio pipeline foramt
    private val audioSenderFormat = "openslessrc " +
            "! audioconvert ! audio/x-raw, channels=1, rate=%d ! rtpL16pay " +
            "! udpsink host=%s port=%d" // [rate, ip, port]
    private var audioSender: String? = null
    private val audioRecvFormat = "udpsrc port=%d " +
            "! application/x-rtp, media=(string)audio, clock-rate=(int)%d, channels=(int)1, payload=(int)96 " +
            "! rtpL16depay ! audioconvert ! autoaudiosink sync=true" // [port, rate]
    private var audioRecver: String? = null

    // video pipeline format
    private val videoSenderFormat = "ahcsrc device=%d " +
            "! video/x-raw, width=%d, height=%d " +
            "! videoconvert ! x264enc tune=zerolatency ! rtph264pay " +
            "! udpsink host=%s port=%d" // [cameraIndex, width, height, ip, port]
    private var videoSender: String? = null
    private val videoRecvFormat = "udpsrc port=%d " +
            "! application/x-rtp !  rtph264depay ! h264parse ! avdec_h264 ! autovideosink sync=true" // [port]
    private var videoRecver: String? = null

    fun setAudioSendPipeline (connectIp: String, connectPort: Int, audioRate: Int) {
        this.audioSender = String.format(audioSenderFormat, audioRate, connectIp, connectPort)
    }

    fun setAudioRecvPipeline (myPort: Int, audioRate: Int) {
        this.audioRecver = String.format(audioRecvFormat, myPort, audioRate)
    }

    fun setVideoSendPipeline (connectIp: String, connectPort: Int, cameraIndex: Int, videoWidth: Int, videoHeight: Int) {
        this.videoSender = String.format(videoSenderFormat, cameraIndex, videoWidth, videoHeight, connectIp, connectPort)
    }

    fun setVideoRecvPipeline (myPort: Int) {
        this.videoRecver = String.format(videoRecvFormat, myPort)
    }

    fun initPlay(context: Context, surface: Surface? = null) {
        // Initialize GStreamer and warn if it fails
        try {
            GStreamer.init(context)
        } catch (e: Exception) {
            setMessage(e.toString())
        }
        surface?.let {
            nativeInitPlay(videoSender, videoRecver, audioSender, audioRecver)
            nativeSurfaceInit(it)
        } ?: run {
            nativeInitPlay(null, null, audioSender, audioRecver)
        }
    }

    fun onResume() {
        // play audio and video
        nativeResume()
    }

    fun onPause() {
        // stop audio and video
        nativePause()
    }

    fun onDestroy() {
        nativeSurfaceFinalize()
        nativeFinalize()
    }

    // Called from native code. This sets the content of the TextView from the UI thread.
    fun setMessage(message: String) {
        Log.d ("NSIntercom", message)
    }

    // Called from native code. Native code calls this once it has created its pipeline and
    // the main loop is running, so it is ready to accept commands.
    fun onGStreamerInitialized() {
        Log.i("GStreamer", "GStreamer initialized:")
    }
}