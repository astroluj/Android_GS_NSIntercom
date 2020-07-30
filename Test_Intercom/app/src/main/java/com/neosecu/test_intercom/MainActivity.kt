package com.neosecu.test_intercom

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.neosecu.intercom.NSIntercom
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.set

class MainActivity : AppCompatActivity() {

    companion object {
        private val nsIntercom by lazy { NSIntercom.getInstance() }
        private const val IP = "ipAddress"
    }
    private val ipEdit by lazy { findViewById<EditText>(R.id.ipEdit) }
    private val myInfoEdit by lazy { findViewById<EditText>(R.id.myInfoEdit) }

    private var isConn = false
    private var isSpeaker = false
    private var streamType = NSIntercom.STREAM_VOICE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getMyInfo()
    }

    fun onConnectClick(v: View) {
        if (isConn) {
            nsIntercom.onPause()
            nsIntercom.onDestroy()
            isConn = false
            (v as Button).text = "Connect"
            return
        }

        val sendInfo = ipEdit.text.toString().split(":")
        if (!isConn && sendInfo.size == 2) {
            nsIntercom.setAudioSendPipeline(sendInfo[0], sendInfo[1].toInt(),16000)
            nsIntercom.setAudioRecvPipeline(5001, 16000, streamType)
            nsIntercom.initPlay(applicationContext)
            isConn =  true

            (v as Button).text = "Disconnect"
        }
        else {
            Toast.makeText(this, "아이피 정보를 정확히 입력해 주세요 format : xxx.xxx.xxx.xxx:port", Toast.LENGTH_LONG).show()
            isConn = false
        }
    }

    fun onStreamTypeClick (v: View) {
        if (isSpeaker) {
            // 스피커 상태에서 보이스 변환
            streamType = NSIntercom.STREAM_VOICE
            (v as Button).text = "Voice Call"
        }
        else {
            // 보이스 상태에서 스피커 변환
            streamType = NSIntercom.STREAM_NONE
            (v as Button).text = "Speaker Call"
        }
        isSpeaker = !isSpeaker
        if (isConn) {
            nsIntercom.onPause()
            nsIntercom.onDestroy()
            nsIntercom.setAudioRecvPipeline(5001, 16000, streamType)
            nsIntercom.initPlay(applicationContext)
        }
    }

    override fun onResume() {
        super.onResume()
        // 권한이 필요하면 권한 체크
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
            if (PermissionUtils.isPermissionDenied(applicationContext)) {
                val permissionArray = ArrayList<String>()

                if (!PermissionUtils.isPermissionGranted(applicationContext, Manifest.permission.RECORD_AUDIO)) {
                    //if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO));
                    permissionArray.add(Manifest.permission.RECORD_AUDIO)
                }
                val permissions = arrayOfNulls<String>(permissionArray.size)
                requestPermissions(permissionArray.toArray(permissions), 111)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        nsIntercom.onPause()
        nsIntercom.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun getMyInfo () {
        // wifi network
        val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = HashMap<String, String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager.activeNetwork?.let {
                val active = manager.getNetworkCapabilities(it)
                if (active != null && active.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    networkInfo.putAll(getWifiNetwork())
                }
            }
        }
        else {
            // wifi network
            manager.activeNetworkInfo?.let {
                if (it.type == ConnectivityManager.TYPE_WIFI) {
                    networkInfo.putAll(getWifiNetwork())
                }
            }
        }

        myInfoEdit.hint = "${networkInfo[IP]}:5001"
    }

    private fun getWifiNetwork(): HashMap<String, String> {
        val networkInfo = HashMap<String, String>()
        val dhcpInfo = (applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).dhcpInfo
        // ip
        networkInfo[IP] = getWifiAddress(dhcpInfo.ipAddress)

        return networkInfo
    }

    private fun getWifiAddress(addr: Int): String {
        return String.format("%d.%d.%d.%d", addr and 0xFF, addr.shr(8) and 0xFF , addr.shr(16) and 0xFF, addr.shr(24) and 0xFF)
    }

    // 네트워크 변화 감지
    inner class NetworkChangeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            getMyInfo()
        }
    }
}
