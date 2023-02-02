package com.example.scorp_final_assignment.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.scorp_final_assignment.MainViewModel
import com.example.scorp_final_assignment.databinding.FragmentLiveChatBinding
import com.example.scorp_final_assignment.repository.Repository.AppID
import com.example.scorp_final_assignment.repository.Repository.ChannelID
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas
import kotlinx.coroutines.launch
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.scorp_final_assignment.repository.Repository.Token
import io.agora.rtm.*

class LiveChatFragment : Fragment() {

    private val viewModel: MainViewModel by viewModels()

    private var _binding: FragmentLiveChatBinding? = null
    private val binding get() = _binding!!


    //SurfaceView to render local video in a Container.
    private var localSurfaceView: SurfaceView? = null
    //SurfaceView to render Remote video in a Container.
    private var remoteSurfaceView: SurfaceView? = null
    private var agoraEngine: RtcEngine? = null
    private val uid = 0
    private var isJoined = false



    // <Vg k="MESS" /> client instance
    private var mRtmClient: RtmClient? = null

    // <Vg k="MESS" /> channel instance
    private var mRtmChannel: RtmChannel? = null

    // TextView to show message records in the UI
    private var message_history: TextView? = null

    private var et_message_content: EditText? = null

    // <Vg k="MESS" /> user ID of the message receiver
    private var peer_id: String? = null

    // Message content
    private var message_content: String? = null

    private  val user_name = "behzat"




    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentLiveChatBinding.inflate(inflater, container, false)

        setupVideoSDKEngine()

        try {
            mRtmClient = RtmClient.createInstance(requireContext(), AppID, object : RtmClientListener {
                override fun onConnectionStateChanged(state: Int, reason: Int) {
                    val text = "Connection state changed to $state Reason: $reason\n"
                    writeToMessageHistory(text)
                }

                override fun onImageMessageReceivedFromPeer(rtmImageMessage: RtmImageMessage, s: String) {
                }

                override fun onFileMessageReceivedFromPeer(rtmFileMessage: RtmFileMessage, s: String) {
                }

                override fun onMediaUploadingProgress(rtmMediaOperationProgress: RtmMediaOperationProgress, l: Long) {
                }

                override fun onMediaDownloadingProgress(rtmMediaOperationProgress: RtmMediaOperationProgress, l: Long) {
                }

                override fun onTokenExpired() {
                }

                override fun onPeersOnlineStatusChanged(map: Map<String, Int>) {
                }

                override fun onMessageReceived(rtmMessage: RtmMessage, peerId: String) {
                    val text = "Message received from $peerId Message: ${rtmMessage.text}\n"
                    writeToMessageHistory(text)
                }
            })
        } catch (e: Exception) {
            throw RuntimeException("initialization failed!")
        }


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.JoinButton.setOnClickListener {
            joinChannel()
        }

        binding.LeaveButton.setOnClickListener {
            leaveChannel()
        }

        binding.sendChannelMsgButton.setOnClickListener{
            onClickSendChannelMsg()
        }

        loginTextChat()
        joinTextChat()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    //region video chat
    private fun setupVideoSDKEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = context
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

            // Set the remote video view
            activity!!.runOnUiThread {
                setupRemoteVideo(uid)
            }
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            isJoined = true
            showMessage("Joined Channel $channel")
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            showMessage("Remote user offline $uid $reason")

            activity!!.runOnUiThread {
                remoteSurfaceView!!.visibility = View.GONE
            }
        }
    }

    private fun setupRemoteVideo(uid: Int) {
        val container = binding.remoteVideoViewContainer
        remoteSurfaceView = SurfaceView(context)
        remoteSurfaceView!!.setZOrderMediaOverlay(true)
        container.addView(remoteSurfaceView)
        agoraEngine!!.setupRemoteVideo(
            VideoCanvas(
                remoteSurfaceView,
                VideoCanvas.RENDER_MODE_HIDDEN,
                uid
            )
        )
        // Display RemoteSurfaceView.
        remoteSurfaceView!!.visibility = View.VISIBLE
    }

    private fun setupLocalVideo() {
        val container = binding.localVideoViewContainer
        // Create a SurfaceView object and add it as a child to the FrameLayout.
        localSurfaceView = SurfaceView(context)
        container.addView(localSurfaceView)
        // Pass the SurfaceView object to Agora so that it renders the local video.
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
        // Display LocalSurfaceView.
        setupLocalVideo()
        localSurfaceView!!.visibility = View.VISIBLE
        // Start local preview.
        agoraEngine!!.startPreview()
        // Join the channel with a temp token.
        // You need to specify the user ID yourself, and ensure that it is unique in the channel.
        agoraEngine!!.joinChannel(null, ChannelID, uid, options)
    }

    fun leaveChannel() {
        if (!isJoined) {
            showMessage("Join a channel first")
        } else {
            agoraEngine!!.leaveChannel()
            showMessage("You left the channel")
            // Stop remote video rendering.
            if (remoteSurfaceView != null) remoteSurfaceView!!.visibility = View.GONE
            // Stop local video rendering.
            if (localSurfaceView != null) localSurfaceView!!.visibility = View.GONE
            isJoined = false
        }
    }

    fun showMessage(message: String?) {
        lifecycleScope.launch{
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    //endregion




    //region text chat

    // Button to login to Signaling
    fun loginTextChat() {
        // Log in to Signaling
        mRtmClient!!.login(Token, user_name, object : ResultCallback<Void?> {
            override fun onSuccess(responseInfo: Void?) {}
            override fun onFailure(errorInfo: ErrorInfo) {
                val text: CharSequence = "User: $uid failed to log in to Signaling!$errorInfo"
                val duration = Toast.LENGTH_SHORT
                activity!!.runOnUiThread {
                    val toast = Toast.makeText(context, text, duration)
                    toast.show()
                }
            }
        })
    }


    // Button to join the <Vg k="MESS" /> channel
    fun joinTextChat() {
        //channel_name = et_channel_name!!.getText().toString()
        // Create a channel listener
        val mRtmChannelListener: RtmChannelListener = object : RtmChannelListener {
            override fun onMemberCountUpdated(i: Int) {}
            override fun onAttributesUpdated(list: List<RtmChannelAttribute>) {}
            override fun onMessageReceived(message: RtmMessage, fromMember: RtmChannelMember) {
                val text = message.text
                val fromUser = fromMember.userId
                val message_text = "Message received from $fromUser : $text\n"
                writeToMessageHistory(message_text)
            }

            override fun onImageMessageReceived(
                rtmImageMessage: RtmImageMessage,
                rtmChannelMember: RtmChannelMember
            ) {
            }

            override fun onFileMessageReceived(
                rtmFileMessage: RtmFileMessage,
                rtmChannelMember: RtmChannelMember
            ) {
            }

            override fun onMemberJoined(member: RtmChannelMember) {}
            override fun onMemberLeft(member: RtmChannelMember) {}
        }
        try {
            // Create an <Vg k="MESS" /> channel
            mRtmChannel = mRtmClient!!.createChannel(ChannelID, mRtmChannelListener)
        } catch (e: RuntimeException) {
        }
        // Join the <Vg k="MESS" /> channel
        mRtmChannel!!.join(object : ResultCallback<Void?> {
            override fun onSuccess(responseInfo: Void?) {}
            override fun onFailure(errorInfo: ErrorInfo) {
                //val text: CharSequence = "User: $uid failed to join the channel!$errorInfo"
                val text: CharSequence = "$errorInfo"
                val duration = Toast.LENGTH_SHORT
                activity!!.runOnUiThread {
                    val toast = Toast.makeText(context, text, duration)
                    toast.show()
                }
            }
        })
    }

    // Button to log out of Signaling
    fun onClickLogout(v: View?) {
        // Log out of Signaling
        mRtmClient!!.logout(null)
    }

    // Button to leave the <Vg k="MESS" /> channel
    fun onClickLeave(v: View?) {
        // Leave the <Vg k="MESS" /> channel
        mRtmChannel!!.leave(null)
    }

    // Button to send channel message
    fun onClickSendChannelMsg() {
        et_message_content = binding.msgBox
        message_content = et_message_content!!.getText().toString()

        // Create <Vg k="MESS" /> message instance
        val message = mRtmClient!!.createMessage()
        message.text = message_content

        // Send message to channel
        mRtmChannel!!.sendMessage(message, object : ResultCallback<Void?> {
            override fun onSuccess(aVoid: Void?) {
                val text = """Message sent to channel ${mRtmChannel!!.id} : ${message.text}"""
                writeToMessageHistory(text)
            }

            override fun onFailure(errorInfo: ErrorInfo) {
                val text = """Message fails to send to channel ${mRtmChannel!!.id} Error: $errorInfo"""
                writeToMessageHistory(text)
            }
        })
    }

    private fun writeToMessageHistory(record: String) {
        message_history = binding.messageHistory
        message_history!!.append(record)
    }


    //endregion
















}