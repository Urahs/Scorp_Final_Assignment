package com.example.scorp_final_assignment

import android.app.Application
import android.view.SurfaceView
import android.view.View
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scorp_final_assignment.repository.Repository.AppID
import com.example.scorp_final_assignment.repository.Repository.ChannelName
import com.example.scorp_final_assignment.repository.Repository.Token
import com.example.scorp_final_assignment.repository.Repository.UserId
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.properties.Delegates


@HiltViewModel
class MainViewModel @Inject constructor(
    private val application: Application
): ViewModel() {

    private var agoraEngine: RtcEngine? = null
    var onUserJoinedUid by Delegates.notNull<Int>()

    private val _setupRemoteVideo = MutableLiveData(false)
    val setupRemoteVideo: LiveData<Boolean> = _setupRemoteVideo

    private val _remoteSurfaceViewVisibility = MutableLiveData(false)
    val remoteSurfaceViewVisibility: LiveData<Boolean> = _remoteSurfaceViewVisibility

    var isJoined = false
        private set(value) {
            field = value
        }

    fun showMessage(message: String?) {
        viewModelScope.launch {
            Toast.makeText(application, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun setupVideoSDKEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = application
            config.mAppId = AppID
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
            // By default, the video module is disabled, call enableVideo to enable it.
            agoraEngine!!.enableVideo()
        } catch (e: Exception) {
            showMessage(e.toString())
        }
    }

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        // Listen for the remote host joining the channel to get the uid of the host.
        override fun onUserJoined(uid: Int, elapsed: Int) {
            showMessage("Remote user joined $uid")
            onUserJoinedUid = uid
            _setupRemoteVideo.value = true
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            isJoined = true
            showMessage("Joined Channel $channel")
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            showMessage("Remote user offline $uid $reason")
            _remoteSurfaceViewVisibility.value = false
        }
    }

    fun setupRemoteVideo(remoteSurfaceView: SurfaceView?){
        agoraEngine!!.setupRemoteVideo(
            VideoCanvas(
                remoteSurfaceView,
                VideoCanvas.RENDER_MODE_FIT,
                onUserJoinedUid
            )
        )
    }

    fun setupLocalVideo(localSurfaceView: SurfaceView?) {
        agoraEngine!!.setupLocalVideo(
            VideoCanvas(
                localSurfaceView,
                VideoCanvas.RENDER_MODE_HIDDEN,
                0
            )
        )
    }

    fun joinChannel() {

        val options = ChannelMediaOptions()

        // For a Video call, set the channel profile as COMMUNICATION.
        options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
        // Set the client role as BROADCASTER or AUDIENCE according to the scenario.
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
        // Start local preview.
        agoraEngine!!.startPreview()
        // Join the channel with a temp token.
        // You need to specify the user ID yourself, and ensure that it is unique in the channel.
        agoraEngine!!.joinChannel(Token, ChannelName, UserId, options)
    }

    fun leaveChannel(){
        agoraEngine!!.leaveChannel()
        isJoined = false
    }

    override fun onCleared() {
        super.onCleared()
        agoraEngine!!.stopPreview()
        agoraEngine!!.leaveChannel()

        // Destroy the engine in a sub-thread to avoid congestion
        Thread {
            RtcEngine.destroy()
            agoraEngine = null
        }.start()
    }
}