package com.example.audiovideocaller

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceView
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.audiovideocaller.databinding.ActivityMainBinding
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val appId = "b780ee96d2c94960bb48ff55e7faea34"
    private val channelName = "aliCoder"
    private val token =
        "007eJxTYKj+WX/+alBT02WrjObH9fmzzno6iITvYr+T5ZfDNUfHwVaBIcncwiA11dIsxSjZ0sTSzCApycQiLc3UNNU8LTE10dhkWxdzekMgIwNn9hNGRgYIBPE5GBJzMp3zU1KLGBgA0FUgbQ=="
    private val uid = 0

    private var isJoinned = false
    private var agoraEngine: RtcEngine? = null
    private var localSerfaceView: SurfaceView? = null
    private var remoteSerfaceView: SurfaceView? = null

    private val REQUESTED_ID = 12
    private val REQUESTED_PERMISSION =
        arrayOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CAMERA
        )

    private fun chechSelfPermission(): Boolean {
        return !(ContextCompat.checkSelfPermission(
            this, REQUESTED_PERMISSION[0]
        ) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this, REQUESTED_PERMISSION[1]
                ) != PackageManager.PERMISSION_GRANTED)
    }

    private fun showMessage(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }


    private fun setupVideoSdkEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
            agoraEngine!!.enableVideo()
        } catch (e: Exception) {
            showMessage(e.message.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        if (!chechSelfPermission()) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSION, REQUESTED_ID)
        }
        setupVideoSdkEngine()
        binding.btnJoinCall.setOnClickListener {
            joinCall()
        }
        binding.btnDeleteCall.setOnClickListener {
            deleteCall()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        agoraEngine!!.stopPreview()
        agoraEngine!!.leaveChannel()
        Thread{
            RtcEngine.destroy()
            agoraEngine = null
        }.start()
    }
    private fun deleteCall() {
        if(!isJoinned){
            Toast.makeText(this, "Join a call first", Toast.LENGTH_SHORT).show()
        }else{
            agoraEngine!!.leaveChannel()
            showMessage("You left the call")
            if (localSerfaceView!=null) localSerfaceView!!.visibility = GONE
            if (remoteSerfaceView!=null) remoteSerfaceView!!.visibility = GONE
            isJoinned = false
        }
    }

    private fun joinCall() {
        if (chechSelfPermission()) {
            val option = ChannelMediaOptions()
            option.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            option.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            setupLocalVideo()
            localSerfaceView!!.visibility = VISIBLE
            agoraEngine!!.startPreview()
            agoraEngine!!.joinChannel(token, channelName, uid, option)
        }else{
            Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
        }
    }

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onUserJoined(uid: Int, elapsed: Int) {
            showMessage("User Joined Successfully $uid")
            runOnUiThread{setupRemoteVideo(uid)}
        }

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            isJoinned = true
            showMessage("joined Channel $channelName")
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            showMessage("User offline")
            runOnUiThread { remoteSerfaceView!!.visibility = GONE }
        }
    }
    private fun setupRemoteVideo(uid:Int){
        remoteSerfaceView = SurfaceView(baseContext)
        remoteSerfaceView!!.setZOrderMediaOverlay(true)
        binding.remoteUser.addView(remoteSerfaceView)
        agoraEngine!!.setupRemoteVideo(
            VideoCanvas(
                remoteSerfaceView,
                VideoCanvas.RENDER_MODE_FIT,
                uid
            )
        )
    }


    private fun setupLocalVideo(){
        localSerfaceView = SurfaceView(baseContext)
        binding.localUser.addView(localSerfaceView)
        agoraEngine!!.setupLocalVideo(
            VideoCanvas(
                localSerfaceView,
                VideoCanvas.RENDER_MODE_FIT,
                0
            )
        )
    }
}