package com.neosecu.test_intercom

import android.app.Application
import android.util.Log
import com.neosecu.intercom.NSIntercom

class App: Application() {
    companion object {
       // val nsIntercom by lazy {  NSIntercom.getInstance() }
    }

    override fun onCreate() {
        super.onCreate()
        /*nsIntercom.setAudioSendPipeline("192.168.1.117", 5001, 16000)
        nsIntercom.setAudioRecvPipeline(6001, 16000)
        nsIntercom.init(applicationContext)*/
    }
}