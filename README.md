# gstreamer이용하여 인터컴 기능 사용

# Gstreamer 1.16.2 version <b>
  https://gstreamer.freedesktop.org/download/
# Android kotlin<br>
- current video streaming not abled
- UDP RTP Audio communication


<p><p>
<h2> example<br></h2>
<pre><code>
/***********************************Intercom***********************************/<br>
val nsIntercom  by lazy { NSIntercom.getInstance() }
...
// intercom init
nsIntercom.init(context)
...
// 받은 포트로 인터폰 접속
private fun intercomOn (context: Context, port: Int, remoteIp: String) {
    // intercom connect
    nsIntercom.setAudioSendPipeline("192.168.x.x", port, DEFAULT_INTERCOM_BITRATE)
    nsIntercom.setAudioRecvPipeline(556, DEFAULT_INTERCOM_BITRATE)
    nsIntercom.play()
}
// 받은 인터폰 접속 해제
private fun intercomOff () {
    nsIntercom.onPause()
    nsIntercom.onDestroy()
}
/***********************************Intercom***********************************/
</code></pre>
<p><p>

<h2>Dependency<br></h2>
Project build.gradle
<code><pre>
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
</pre></code>
Application build.gradle
<code><pre>
dependencies {
	implementation 'com.github.astroluj:Android_Intercom:1.2.11'
}
</pre></code>

<h2>License</h2><br>
<p style="font-size:x-large">
  License<br>
  <a href="http://www.apache.org/licenses/LICENSE-2.0">
      Apache 2.0 License
  </a>
  <br>
  <a href="https://www.gnu.org/licenses/lgpl-3.0.html">
      LGPL 3.0 License
  </a>
</p>



