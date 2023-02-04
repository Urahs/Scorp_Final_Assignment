package com.example.scorp_final_assignment.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.scorp_final_assignment.MainViewModel
import com.example.scorp_final_assignment.repository.Repository.AppID
import com.example.scorp_final_assignment.repository.Repository.ChannelID
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import com.example.scorp_final_assignment.adapters.MessageAdapter
import com.example.scorp_final_assignment.R
import com.example.scorp_final_assignment.databinding.FragmentLiveChatBinding
import com.example.scorp_final_assignment.repository.Repository
import com.example.scorp_final_assignment.repository.Repository.Token
import com.example.scorp_final_assignment.repository.Repository.byteValueToImageDictionary
import com.example.scorp_final_assignment.repository.Repository.clubGift
import com.example.scorp_final_assignment.repository.Repository.Gift
import com.example.scorp_final_assignment.repository.Repository.diamondGift
import com.example.scorp_final_assignment.repository.Repository.heartGift
import com.example.scorp_final_assignment.repository.Repository.spadeGift
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.agora.rtm.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalTime


class LiveChatFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()

    private var _binding: FragmentLiveChatBinding? = null
    private val binding get() = _binding!!

    var messageAdapter = MessageAdapter()

    //region video chat variables
    private var localSurfaceView: SurfaceView? = null
    private var remoteSurfaceView: SurfaceView? = null
    private var agoraEngine: RtcEngine? = null
    private val uid = 0
    private var isJoined = false
    //endregion

    //region message chat variables
    private var mRtmClient: RtmClient? = null
    private var mRtmChannel: RtmChannel? = null
    private var jobList = mutableListOf<Repository.Gift>()
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

        binding.chatButton.setOnClickListener{
            openMessageButtonClick()
        }

        binding.sendTextMessageButton.setOnClickListener{
            onClickSendChannelMsg()
        }


        /*
        viewModel.nickName.observe(viewLifecycleOwner){
            nickName = it
        }
        */

        val rootView = requireActivity().findViewById<View>(android.R.id.content)
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val heightDiff = rootView.rootView.height - rootView.height

            // Keyboard is shown
            if (heightDiff > dpToPx(requireActivity(), 200f)) {
                Log.d("Deneme", "KEYBOARD OPENED")

                binding.textArea.visibility = View.VISIBLE
                binding.JoinButton.visibility = View.GONE
                binding.LeaveButton.visibility = View.GONE
            }
            // Keyboard is hidden
            else {
                Log.d("Deneme", "KEYBOARD CLOSED")
                binding.textArea.visibility = View.GONE
                binding.JoinButton.visibility = View.VISIBLE
                binding.LeaveButton.visibility = View.VISIBLE
            }
        }


        loginTextChat()
        joinTextChat()
    }

    fun dpToPx(context: Context, dp: Float): Int {
        return (dp * context.resources.displayMetrics.density + 0.5f).toInt()
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
                        val message_text = "$fromUser : $text\n"
                        writeToMessageHistory(message_text)
                    }
                    RtmMessageType.RAW -> {

                        val giftImage = byteValueToImageDictionary[message.rawMessage.contentToString()]
                        var giftByteArray : ByteArray? = null

                        when(message.rawMessage.contentToString()){
                            clubGift.contentToString() -> giftByteArray = clubGift
                            heartGift.contentToString() -> giftByteArray = heartGift
                            diamondGift.contentToString() -> giftByteArray = diamondGift
                            spadeGift.contentToString() -> giftByteArray = spadeGift
                        }

                        sendGift(Gift(false, giftByteArray!!, giftImage))
                    }
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
        mRtmClient!!.logout(null)
    }

    // Button to leave the channel
    fun onClickLeave(v: View?) {
        mRtmChannel!!.leave(null)
    }

    private fun openMessageButtonClick() {

        /*
        binding.textArea.visibility = View.VISIBLE
        binding.JoinButton.visibility = View.GONE
        binding.LeaveButton.visibility = View.GONE

         */

        Log.d("Deneme", "BUTTON CLICKEDDD!!!")


        val textField = binding.textField
        textField.requestFocus()
        val inputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(textField, InputMethodManager.SHOW_IMPLICIT)
    }

    // Button to send channel message
    fun onClickSendChannelMsg() {
        /*
        binding.textArea.visibility = View.GONE
        binding.JoinButton.visibility = View.VISIBLE
        binding.LeaveButton.visibility = View.VISIBLE
         */

        val message = mRtmClient!!.createMessage()
        message.text = binding.textField.text.toString()
        binding.textField.setText("")

        mRtmChannel!!.sendMessage(message, object : ResultCallback<Void?> {
            override fun onSuccess(aVoid: Void?) {
                val text = "${viewModel.nickName} : ${message.text}"
                writeToMessageHistory(text)
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
            }

            override fun onFailure(errorInfo: ErrorInfo) {
                val text = "Gift Message Error:"
                writeToMessageHistory(text)
            }
        })
    }

    private fun writeToMessageHistory(record: String) {
        val currentMessages = messageAdapter.currentList.toMutableList()
        currentMessages.add(Repository.Message(record, LocalTime.now()))
        messageAdapter.submitList(currentMessages)
        binding.textRecyclerView.smoothScrollToPosition(messageAdapter.itemCount)
    }



    private fun showGiftMessages() {

        val dialog = BottomSheetDialog(requireContext(), R.style.AppBottomSheetDialogTheme)
        //dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_gift_message)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val clubIV = dialog.findViewById<TextView>(R.id.clubIV) as ImageView
        val spadeIV = dialog.findViewById<TextView>(R.id.spadeIV) as ImageView
        val heartIV = dialog.findViewById<TextView>(R.id.heartIV) as ImageView
        val diamondIV = dialog.findViewById<TextView>(R.id.diamondIV) as ImageView

        clubIV.setImageResource(R.drawable.club)
        spadeIV.setImageResource(R.drawable.spade)
        heartIV.setImageResource(R.drawable.heart)
        diamondIV.setImageResource(R.drawable.diamond)

        clubIV.setOnClickListener{ sendGift(Gift(true, clubGift, R.drawable.club)) }
        spadeIV.setOnClickListener{ sendGift(Gift(true, spadeGift, R.drawable.spade)) }
        diamondIV.setOnClickListener{ sendGift(Gift(true, diamondGift, R.drawable.diamond)) }
        heartIV.setOnClickListener{ sendGift(Gift(true, heartGift, R.drawable.heart)) }

        dialog.show()
    }

    private fun sendGift(gift: Gift){

        if(gift.isSended)
            onClickSendGiftMsg(gift.giftByteArray)

        jobList.add(gift)
        if(!jobContinue)
            showGift()
    }


    private fun showGift(){

        lifecycleScope.launch(Dispatchers.Main){

            val sentGiftIV = binding.sentGiftIV
            jobContinue = true

            sentGiftIV.setImageResource(jobList[0].giftImage!!)
            sentGiftIV.visibility = View.VISIBLE

            //animation
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