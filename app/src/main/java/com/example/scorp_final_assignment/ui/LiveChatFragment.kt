package com.example.scorp_final_assignment.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.scorp_final_assignment.MainViewModel
import com.example.scorp_final_assignment.repository.Repository.AppID
import com.example.scorp_final_assignment.repository.Repository.ChannelID
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import com.example.scorp_final_assignment.adapters.MessageAdapter
import com.example.scorp_final_assignment.R
import com.example.scorp_final_assignment.adapters.TextAdapter
import com.example.scorp_final_assignment.databinding.FragmentLiveChatBinding
import com.example.scorp_final_assignment.repository.Repository.Token
import com.example.scorp_final_assignment.repository.Repository.clubGift
import com.example.scorp_final_assignment.repository.Repository.diamondGift
import com.example.scorp_final_assignment.repository.Repository.giftImageDictionary
import com.example.scorp_final_assignment.repository.Repository.heartGift
import com.example.scorp_final_assignment.repository.Repository.spadeGift
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.agora.rtm.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.*

class LiveChatFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()

    private var _binding: FragmentLiveChatBinding? = null
    private val binding get() = _binding!!

    var textAdapter = TextAdapter()
    var messageAdapter = MessageAdapter()


    //region video chat variables
    //SurfaceView to render local video in a Container.
    private var localSurfaceView: SurfaceView? = null
    //SurfaceView to render Remote video in a Container.
    private var remoteSurfaceView: SurfaceView? = null
    private var agoraEngine: RtcEngine? = null
    private val uid = 0
    private var isJoined = false
    //endregion

    //region message chat variables
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


    private var jobList = mutableListOf<Boolean>()
    private var jobContinue = false

    //endregion



    //region fragment override functions
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentLiveChatBinding.inflate(inflater, container, false)

        binding.textRecyclerView.adapter = messageAdapter

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

        binding.giftButton.setOnClickListener{
            showGiftMessages()
        }

        /*
        viewModel.nickName.observe(viewLifecycleOwner){
            nickName = it
        }
        */

        loginTextChat()
        joinTextChat()

        val x = binding.sentGiftIV
        x.setImageResource(giftImageDictionary["club"]!!)
        x.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    //endregion


    //region video chat section
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


    //region text chat section

    // Button to login to Signaling
    fun loginTextChat() {
        // Log in to Signaling
        mRtmClient!!.login(Token, viewModel.nickName, object : ResultCallback<Void?> {
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

    fun flowFnc(): Flow<Boolean> = flow{
        val sentGiftIV = binding.sentGiftIV
        lifecycleScope.launch(Dispatchers.Main){
            sentGiftIV.visibility = View.VISIBLE
            sentGiftIV.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.zoom_in))
            delay(3000L)
            sentGiftIV.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.zoom_out))
            sentGiftIV.visibility = View.GONE
            emit(true)
        }
    }.flowOn(Dispatchers.Default)

    val testFlow = flow<Boolean>{
        val sentGiftIV = binding.sentGiftIV
        lifecycleScope.launch(Dispatchers.Main){
            sentGiftIV.visibility = View.VISIBLE
            sentGiftIV.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.zoom_in))
            delay(3000L)
            sentGiftIV.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.zoom_out))
            sentGiftIV.visibility = View.GONE
            delay(1000L)
            jobContinue = false
            jobList.removeAt(0)
            if(jobList.isNotEmpty())
                showGift()
        }
    }


    // Button to join the <Vg k="MESS" /> channel
    fun joinTextChat() {
        //channel_name = et_channel_name!!.getText().toString()
        // Create a channel listener
        val mRtmChannelListener: RtmChannelListener = object : RtmChannelListener {
            override fun onMemberCountUpdated(i: Int) {}
            override fun onAttributesUpdated(list: List<RtmChannelAttribute>) {}
            override fun onMessageReceived(message: RtmMessage, fromMember: RtmChannelMember) {
                when(message.messageType){
                    RtmMessageType.TEXT -> {
                        val text = message.text
                        val fromUser = fromMember.userId
                        val message_text = "Message received from $fromUser : $text\n"
                        writeToMessageHistory(message_text)
                    }
                    RtmMessageType.RAW -> {
                        /*lifecycleScope.launch{
                            testFlow.collect(){

                            }
                        }

                         */

                        jobList.add(false)
                        Log.d("Deneme", "In Receive Part >>> ${jobList}")
                        if(!jobContinue){
                            binding.sentGiftIV.setImageResource(giftImageDictionary["club"]!!)
                            showGift()
                        }


                        Log.d("Deneme", "In Receive Part <<< ${jobList}")


                        Log.d("Deneme", "AAAAAAAAAAAAAAAAAAA")

                    }
                    else -> Log.d("Deneme", "EEEEEEEEE")
                }
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
        //val bytes: ByteArray = byteArrayOf(0x02)
        //val message = mRtmClient!!.createMessage(bytes)

        //val message = mRtmClient!!.createMessage(clubGift)
        val message = mRtmClient!!.createMessage()
        message.text = message_content
        // Send message to channel
        mRtmChannel!!.sendMessage(message, object : ResultCallback<Void?> {
            override fun onSuccess(aVoid: Void?) {
                val text = "${viewModel.nickName} : ${message.text}"
                writeToMessageHistory(text)
                /*
                Log.d("Deneme", message.rawMessage.toString())
                if(message.rawMessage.contentEquals(byteArrayOf(0x02))){
                    Log.d("Deneme", "KUDURRRRRRRDUMMMMMM!!!!")
                }
                */
            }

            override fun onFailure(errorInfo: ErrorInfo) {
                val text = "${mRtmChannel!!.id} Error: $errorInfo"
                writeToMessageHistory(text)
            }
        })
    }

    // Button to send gift message
    fun onClickSendGiftMsg(gift: ByteArray) {

        val message = mRtmClient!!.createMessage(gift)

        // Send message to channel
        mRtmChannel!!.sendMessage(message, object : ResultCallback<Void?> {
            override fun onSuccess(aVoid: Void?) {
                val text = "Gift message is sent!"
                writeToMessageHistory(text)

                /*
                when(gift){
                    clubGift -> Log.d("Deneme", "CLUB IS SENT")
                    spadeGift -> Log.d("Deneme", "spade IS SENT")
                    heartGift -> Log.d("Deneme", "heart IS SENT")
                    diamondGift -> Log.d("Deneme", "Diamond IS SENT")
                }
                 */
            }

            override fun onFailure(errorInfo: ErrorInfo) {
                val text = "Gift Message Error:"
                writeToMessageHistory(text)
            }
        })
    }

    private fun writeToMessageHistory(record: String) {
        val currentMessages = messageAdapter.currentList.toMutableList()
        currentMessages.add(record)
        messageAdapter.submitList(currentMessages)
    }



    private fun showGiftMessages() {

        val dialog = BottomSheetDialog(requireContext(), R.style.AppBottomSheetDialogTheme)
        //dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_gift_message)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val clubIV = dialog.findViewById<TextView>(R.id.clubIV) as ImageView
        val spadeIV = dialog.findViewById<TextView>(R.id.spadeIV) as ImageView
        val heartIV = dialog.findViewById<TextView>(R.id.heartIV) as ImageView
        val diamongIV = dialog.findViewById<TextView>(R.id.diamondIV) as ImageView

        clubIV.setImageResource(R.drawable.club)
        spadeIV.setImageResource(R.drawable.spade)
        heartIV.setImageResource(R.drawable.heart)
        diamongIV.setImageResource(R.drawable.diamond)

        val sentGiftIV = binding.sentGiftIV

        clubIV.setOnClickListener{
            //onClickSendGiftMsg(clubGift)
            //sentGiftIV.setImageResource(giftImageDictionary["club"]!!)

            /*
            lifecycleScope.launch(Dispatchers.Main){
                sentGiftIV.visibility = View.VISIBLE
                sentGiftIV.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.zoom_in))
                delay(3000L)
                sentGiftIV.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.zoom_out))
                sentGiftIV.visibility = View.GONE
            }
             */

/*
            runBlocking {
                onClickSendGiftMsg(clubGift)
                sentGiftIV.setImageResource(giftImageDictionary["club"]!!)
                testFlow.collect()
            }
*/

            jobList.add(true)
            if(!jobContinue)
                showGift()

            /*
            channel.trySend(
                lifecycleScope
                    .launch(start = CoroutineStart.LAZY) {
                    onClickSendGiftMsg(clubGift)
                    sentGiftIV.setImageResource(giftImageDictionary["club"]!!)
                    testFlow.collect()
                }
            )
*/

        }

        spadeIV.setOnClickListener{ onClickSendGiftMsg(spadeGift) }
        diamongIV.setOnClickListener{ onClickSendGiftMsg(diamondGift) }
        heartIV.setOnClickListener{ onClickSendGiftMsg(heartGift) }


        dialog.show()

    }



    private fun showGift(){

        lifecycleScope.launch(Dispatchers.Main){
            jobContinue = true
            if(jobList[0])
                onClickSendGiftMsg(clubGift)
            val sentGiftIV = binding.sentGiftIV
            sentGiftIV.setImageResource(R.drawable.club)
            sentGiftIV.visibility = View.VISIBLE
            sentGiftIV.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.zoom_in))
            delay(3000L)
            sentGiftIV.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.zoom_out))
            sentGiftIV.visibility = View.GONE
            delay(1000L)
            jobList.removeAt(0)
            jobContinue = false
            if(jobList.isNotEmpty())
                showGift()
        }
    }



    //endregion
}