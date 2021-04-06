@file:Suppress("unused")

package com.astroluj.intercom

import android.content.Context
import android.util.Log
import android.view.Surface
import org.freedesktop.gstreamer.GStreamer
import kotlin.random.Random

/**
 * GStreamer를 이용한 인터폰 API
 * @author astroluj
 */
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

        /**
         * 인스탄스 클래스 반환
         * @return Instance static
         */
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

        /**
         * 모바일 기기에서 소리를 출력하는 스피커 선택
         * 통화용 스피커
         * @see [setAudioRecvPipeline] 함수의 마지막 매개변수
         */
        const val STREAM_VOICE = "voice"
        /**
         * 모바일 기기에서 소리를 출력하는 스피커 선택
         * 외장 스피커
         * @see [setAudioRecvPipeline] 함수의 마지막 매개변수
         */
        const val STREAM_NONE = "none"
    }

    // 연결
    private external fun nativeInitPlay(sendVideoPipeline: String?, recvVideoPipeline: String?,
                                        audioPipeline: String?) // Initialize native code, build pipeline, etc
    // 종료
    private external fun nativeFinalize() // Destroy pipeline and shutdown native code
    private external fun nativeResume() // Set pipeline to PLAYING
    private external fun nativePause() // Set pipeline to PAUSED
    // 서페이스 연결
    private external fun nativeSurfaceInit(surface: Any) // A new surface is available
    // 서페이스 종료
    private external fun nativeSurfaceFinalize() // Surface about to be destroyed
    @JvmField var native_custom_data = 0L // Native code will use this to keep private data

    // audio pipeline format
    private val audioSenderFormat = "openslessrc " +
            "! audioresample ! audioconvert " +
            "! audio/x-raw, rate=%d, channels=%d " +
            "! webrtcdsp target-level-dbfs=20 ! audioconvert ! rtpL16pay " +
            "! udpsink host=%s port=%d async=false" // [rate, channel, ip, port]
    private var audioSender: String? = null
    private val audioReceiverFormat = " udpsrc timeout=5000 reuse=true port=%d " +
            "! application/x-rtp, media=(string)audio, clock-rate=(int)%d, channels=(int)%d, payload=(int)96 " +
            "! rtpjitterbuffer latency=100 ! rtpL16depay ! audioconvert " +
            //"! audiochebband mode=band-pass lower-frequency=1000 upper-frequency=4000 type=2 " +
            "! webrtcechoprobe ! audioconvert ! audioresample" +
            "! openslessink stream_type=%s sync=false"// [port, rate, channel, speaker type]
    private var audioReceiver: String? = null

    // video pipeline format
    // tee 는 영상 송수신을 동시에 하기 위한 분기 타기
    private val videoSenderFormat = "ahcsrc device=%d " +
            "! video/x-raw, width=%d, height=%d " +
            "! videoconvert ! x264enc tune=zerolatency ! rtph264pay " +
            "! udpsink host=%s port=%d" // [cameraIndex, width, height, ip, port]
    private var videoSender: String? = null
    private val videoReceiverFormat = "udpsrc port=%d " +
            "! application/x-rtp !  rtph264depay ! h264parse ! avdec_h264 ! autovideosink sync=false" // [port]
    private var videoReceiver: String? = null

    /**
     * 오디오 발신 프로토콜 설정
     * @param connectIp 오디오 통신 할 장치의 아이피
     * @param connectPort 오디오 통신 할 장치의 포트번호
     * @param audioRate 오디오 레이트 (서로 같은 값으로 설정해야됨)
     */
    fun setAudioSendPipeline (connectIp: String, connectPort: Int, audioRate: Int) {
        this.audioSender = String.format(audioSenderFormat, audioRate, 3, connectIp, connectPort)
    }

    /**
     * 오디오 수신 프로토콜 설정
     * @param myPort 오디오 통신에 사용 할 내 포트번호
     * @param audioRate 오디오 레이트 (서로 같은 값으로 설정해야됨)
     * @param streamType 오디오를 연결 할 스피커 선택 (기본 외장 스피커) [STREAM_VOICE] 내장(통화용) 스피커 [STREAM_NONE] 외장 스피커
     */
    fun setAudioRecvPipeline (myPort: Int, audioRate: Int, streamType: String = "none") {
        this.audioReceiver = String.format(audioReceiverFormat, myPort, audioRate, 3, streamType)
    }

    /**
     * 비디오 발신 프로토콜 설정
     * @param connectIp 비디오 통신 할 장치의 아이피
     * @param connectPort 비디오 통신 할 장치의 포트번호
     * @param cameraIndex 비디오를 전송 할 카메라 번호
     * @param videoWidth 전송 할 비디오의 가로 사이즈
     * @param videoHeight 전송 할 비디오의 세로 사이즈
     */
    fun setVideoSendPipeline (connectIp: String, connectPort: Int, cameraIndex: Int, videoWidth: Int, videoHeight: Int) {
        this.videoSender = String.format(videoSenderFormat, cameraIndex, videoWidth, videoHeight, connectIp, connectPort)
    }

    /**
     * 비디오 수신 프로토콜 설정
     * @param myPort 비디오 통신에 사용 할 내 포트번호
     */
    fun setVideoRecvPipeline (myPort: Int) {
        this.videoReceiver = String.format(videoReceiverFormat, myPort)
    }

    /**
     * 초기화
     * @param context GStreamer 초기화에 사용
     */
    fun init (context: Context) {
        // Initialize GStreamer and warn if it fails
        try {
            GStreamer.init(context)
        } catch (e: Exception) {
            setMessage(e.toString())
        }
    }

    /**
     * 통신 시작
     * @param surface 카메라가 연결 된 서페이스(기본 오디오 통신만 실행 비디오 통신 사용시에 매개변수 입력)
     */
    fun play(surface: Surface? = null) {
        surface?.let {
            nativeInitPlay(videoSender, videoReceiver, audioSender + audioReceiver)
            nativeSurfaceInit(it)
        } ?: run {
            nativeInitPlay(null, null, audioSender + audioReceiver)
        }
    }

    /**
     * 통신 다시 시작 [onPause] 사용 후 사용
     */
    fun onResume() {
        // play audio and video
        nativeResume()
    }

    /**
     * 통신 일시 정지 (연결은 되어있는 상태)
     */
    fun onPause() {
        // stop audio and video
        nativePause()
    }

    fun onDestroy() {
        nativeSurfaceFinalize()
        nativeFinalize()
    }

    /**
     * GStreamer 사용 중 발생하는 네이티브 에러를 반환
     * Override 하여 사용 가능
     * @param message 에러 메세지
     */
    fun setMessage(message: String) {
        Log.d ("NSIntercom", message)
    }

    /**
     * Gstreamer 초기화 완료 콜백
     * Override 하여 사용 가능
     */
    fun onGStreamerInitialized() {
        Log.i("GStreamer", "GStreamer initialized:")
    }
}