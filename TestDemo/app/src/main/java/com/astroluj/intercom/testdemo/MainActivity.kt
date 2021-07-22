package com.astroluj.intercom.testdemo

import android.Manifest
import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.astroluj.intercom.NSIntercom
import com.astroluj.intercom.testdemo.NetworkUtils.Companion.IP
import java.util.*

class MainActivity : AppCompatActivity() {
    // my info widgets
    private val myIpText by lazy { findViewById<TextView>(R.id.myIpText) }
    private val myPortText by lazy { findViewById<TextView>(R.id.myPortText) }
    private val soundBtn by lazy { findViewById<Button>(R.id.soundBtn) }

    // 음성 통신 여부
    private var isSoundPlay = false

    // other info widgets
    private val otherIpEdit by lazy { findViewById<EditText>(R.id.otherIpEdit) }
    private val otherPortEdit by lazy { findViewById<EditText>(R.id.otherPortEdit) }
    private val videoBtn by lazy { findViewById<Button>(R.id.videoBtn) }
    // 영상 통신 여부
    private var isVideoPlay = false

    // speaker mode
    private val speakerStatusCheck by lazy { findViewById<CheckBox>(R.id.speakerStatusCheck) }
    // video receive mode
    private val videoReceiveStatusCheck by lazy { findViewById<CheckBox>(R.id.videoReceiveStatusCheck) }

    // intercom
    private val nsIntercom by lazy { NSIntercom.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.nsIntercom.init(applicationContext)
        this.initView()
    }

    // Result of permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            15 -> {
                var success = 0
                for (isGranted in grantResults) {
                    if (isGranted == PackageManager.PERMISSION_GRANTED) success++
                }
                // if (success == permissions.size) {}
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // 권한이 필요하면 권한 체크
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
            val permissionArray = ArrayList<String>()

            if (!isPermissionGranted(Manifest.permission.RECORD_AUDIO)) {
                //if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO));
                permissionArray.add(Manifest.permission.RECORD_AUDIO)
            }
            val permissions = arrayOfNulls<String>(permissionArray.size)
            if (permissions.isNotEmpty()) {
                requestPermissions(
                    permissionArray.toArray(permissions),
                    15
                )
            }
        }

        // ip setting
        val networkInfo = NetworkUtils.getNetwork(applicationContext)
        myIpText.text = networkInfo[IP]

        if (isSoundPlay || isVideoPlay) this.nsIntercom.onResume()
    }

    override fun onPause() {
        super.onPause()
        this.nsIntercom.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        nsIntercom.onDestroy()
    }

    // want to Permission granted state
    @TargetApi(Build.VERSION_CODES.M)
    fun isPermissionGranted(permission: String?): Boolean {
        return checkSelfPermission(permission!!) == PackageManager.PERMISSION_GRANTED
    }

    private fun initView () {
        // send sound click
        this.soundBtn.setOnClickListener {
            // 중지
            if (isSoundPlay) {
                this.nsIntercom.onPause()
            }
            // 실행
            else {
                this.nsIntercom.setAudioSendPipeline(
                    this.otherIpEdit.text.toString(),
                    this.otherPortEdit.text.toString().toInt(),
                    48000
                )
                this.nsIntercom.setAudioRecvPipeline(
                    this.myPortText.text.toString().toInt(),
                    48000,
                    if (speakerStatusCheck.isChecked) NSIntercom.STREAM_NONE else NSIntercom.STREAM_VOICE
                )
                this.nsIntercom.play(null)
            }

            isSoundPlay = !isSoundPlay
            this.soundBtn.text = resources.getString(if (isSoundPlay) R.string.txtSoundPause else R.string.txtSoundPlay)
        }

        // send Video click
        this.videoBtn.setOnClickListener {
            // 중지
            if (isVideoPlay) {
                this.nsIntercom.onPause()
            }
            // 실행
            else {
                if (videoReceiveStatusCheck.isChecked) {
                    this.nsIntercom.setVideoRecvPipeline(this.myPortText.text.toString().toInt())
                }
                else {
                    this.nsIntercom.setVideoSendPipeline(
                        this.otherIpEdit.text.toString(),
                        this.otherPortEdit.text.toString().toInt(),
                        0,
                        640,
                        480
                    )
                }
            }

            isVideoPlay = !isVideoPlay
            this.videoBtn.text = resources.getString(if (isVideoPlay) R.string.txtVideoPause else R.string.txtVideoPlay)
        }

        /*this.cameraTexture = object: CameraTexture(this, CameraUtils.getBackCameraIndex(), WIDTH, HEIGHT) {
            override fun registerLightSensorFailed() {
            }

            override fun registerOrientSensorFailed() {
            }

            override fun onCompleteRecording(p0: String?) {
            }

            override fun runFrame(
                data: ByteArray, camera: android.hardware.Camera,
                width: Int, height: Int,
                yuvDegree: Int, deviceDegree: Int, isFlashMode: Boolean
            ) {

            }
        }*/
        // this.frameLayout.removeAllViews()
        //this.frameLayout.addView(this.cameraTexture, 0)
    }
}