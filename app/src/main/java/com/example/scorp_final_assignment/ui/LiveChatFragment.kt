package com.example.scorp_final_assignment.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.view.*
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
import android.widget.LinearLayout
import androidx.fragment.app.activityViewModels
import com.example.scorp_final_assignment.adapters.MessageAdapter
import com.example.scorp_final_assignment.R
import com.example.scorp_final_assignment.databinding.FragmentLiveChatBinding
import com.example.scorp_final_assignment.internet_connectivity.NetworkConnectivity
import com.example.scorp_final_assignment.repository.Repository
import com.example.scorp_final_assignment.repository.Repository.Message
import com.example.scorp_final_assignment.repository.Repository.Token
import com.example.scorp_final_assignment.repository.Repository.byteValueToImageDictionary
import com.example.scorp_final_assignment.repository.Repository.clubGift
import com.example.scorp_final_assignment.repository.Repository.Gift
import com.example.scorp_final_assignment.repository.Repository.diamondGift
import com.example.scorp_final_assignment.repository.Repository.heartGift
import com.example.scorp_final_assignment.repository.Repository.spadeGift
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import io.agora.rtm.*
import kotlinx.coroutines.*
import java.time.LocalTime


class LiveChatFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()

    private var _binding: FragmentLiveChatBinding? = null
    private val binding get() = _binding!!

    //region video chat variables
    private var isJoinedVideoChannel = false
    private var localSurfaceView: SurfaceView? = null
    private var remoteSurfaceView: SurfaceView? = null
    private var agoraEngine: RtcEngine? = null
    private val uid = 0
    private var remoteOnBigView = true
    //endregion

    //region message chat variables
    private val messageAdapter = MessageAdapter()
    private var isJoinedTextChannel = false
    private var mRtmClient: RtmClient? = null
    private var mRtmChannel: RtmChannel? = null
    private var giftList = mutableListOf<Gift>()
    private var displayingGift = false
    //endregion

    private var onGlobalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    private lateinit var connectivityObserver: NetworkConnectivity
    private var internetAvailable = true


    //region fragment override functions
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentLiveChatBinding.inflate(inflater, container, false)
        binding.textRecyclerView.adapter = messageAdapter

        internetConnection()
        connectToTextChannel()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonClickEvents()
        detectIfKeyboardOpened()
        messageAdapter.submitList(mutableListOf())
    }

    override fun onPause() {
        super.onPause()
        val rootView = requireActivity().findViewById<View>(android.R.id.content)
        rootView.viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayoutListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        mRtmChannel?.leave(null)
        mRtmClient?.logout(null)
        _binding = null
    }
    //endregion fragment override functions

    //region video chat section
    private fun joinOrLeaveChannel(view: View){

        if(internetAvailable){
            if(!isJoinedVideoChannel){
                isJoinedVideoChannel = true
                setupVideoSDKEngine()
                joinVideoChannel()
                binding.joinLeaveButton.setImageResource(R.drawable.leave_video_chat)
            }
            else{
                exitChannel()
            }
        }
        else{
            showSnackbar("Connection is lost!")
        }
    }

    private fun rtmConnection() {
        try {
            mRtmClient = RtmClient.createInstance(requireContext(), AppID, object : RtmClientListener {
                override fun onConnectionStateChanged(state: Int, reason: Int) {}
                override fun onImageMessageReceivedFromPeer(rtmImageMessage: RtmImageMessage, s: String) {}
                override fun onFileMessageReceivedFromPeer(rtmFileMessage: RtmFileMessage, s: String) {}
                override fun onMediaUploadingProgress(rtmMediaOperationProgress: RtmMediaOperationProgress, l: Long) {}
                override fun onMediaDownloadingProgress(rtmMediaOperationProgress: RtmMediaOperationProgress, l: Long) {}
                override fun onTokenExpired() {}
                override fun onPeersOnlineStatusChanged(map: Map<String, Int>) {}
                override fun onMessageReceived(rtmMessage: RtmMessage, peerId: String) {}
            })
        } catch (_: Exception) {
            isJoinedTextChannel = false
        }
    }

    private fun setupVideoSDKEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = context
            config.mAppId = AppID
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
            // By default, the video module is disabled, call enableVideo to enable it.
            agoraEngine!!.enableVideo()
        } catch (_: Exception) {
            isJoinedVideoChannel = false
        }
    }

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        // Listen for the remote host joining the channel to get the uid of the host.
        override fun onUserJoined(uid: Int, elapsed: Int) {
            showToastMessage("Remote user joined")

            activity!!.runOnUiThread {
                setupRemoteVideo(uid)
            }
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            isJoinedVideoChannel = true
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            showToastMessage("Remote user offline")

            if(!remoteOnBigView)
                switchViewContainers(null)

            activity!!.runOnUiThread {
                remoteSurfaceView!!.visibility = View.INVISIBLE
                binding.bigVideoViewContainer.visibility = View.INVISIBLE
            }
        }
    }

    private fun setupRemoteVideo(uid: Int) {
        val container = binding.bigVideoViewContainer
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
        container.visibility = View.VISIBLE
        remoteSurfaceView!!.visibility = View.VISIBLE
    }

    private fun setupLocalVideo() {
        val container = binding.smallVideoViewContainer
        container.visibility = View.VISIBLE
        localSurfaceView = SurfaceView(context)
        container.addView(localSurfaceView)
        localSurfaceView!!.setZOrderMediaOverlay(true)

        agoraEngine!!.setupLocalVideo(
            VideoCanvas(
                localSurfaceView,
                VideoCanvas.RENDER_MODE_HIDDEN,
                0
            )
        )
    }

    fun joinVideoChannel() {
        try{
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
        } catch(_: Exception){
            isJoinedVideoChannel = false
        }
    }

    private fun exitChannel(){
        if(isJoinedVideoChannel){
            agoraEngine!!.leaveChannel()
            showToastMessage("You left the channel")

            if (remoteSurfaceView != null) remoteSurfaceView!!.visibility = View.GONE
            if (localSurfaceView != null) localSurfaceView!!.visibility = View.GONE
            binding.smallVideoViewContainer.visibility = View.GONE
            binding.bigVideoViewContainer.visibility = View.GONE
            binding.joinLeaveButton.setImageResource(R.drawable.join_voice_chat)

            isJoinedVideoChannel = false
        }
    }

    private fun switchViewContainers(view: View?){

        if (remoteSurfaceView == null)
            return

        fun changeSurfaceViews(surfaceView1: SurfaceView, surfaceView2: SurfaceView){
            surfaceView1.setZOrderMediaOverlay(true)
            surfaceView2.setZOrderMediaOverlay(false)
            binding.bigVideoViewContainer.addView(surfaceView2)
            binding.smallVideoViewContainer.addView(surfaceView1)
        }

        requireActivity().runOnUiThread {
            binding.bigVideoViewContainer.removeAllViews()
            binding.smallVideoViewContainer.removeAllViews()

            if(remoteOnBigView){
                changeSurfaceViews(remoteSurfaceView!!, localSurfaceView!!)
            }
            else {
                changeSurfaceViews(localSurfaceView!!, remoteSurfaceView!!)
            }

            remoteOnBigView = !remoteOnBigView
        }
    }
    //endregion video chat section

    //region text chat section

    fun loginTextChat() {
        // Log in to Signaling
        try {
            mRtmClient!!.login(Token, viewModel.nickName, object : ResultCallback<Void?> {
                override fun onSuccess(responseInfo: Void?) {
                    joinTextChat()
                }
                override fun onFailure(errorInfo: ErrorInfo) {
                    isJoinedTextChannel = false
                }
            })
        } catch (_:Exception){
            isJoinedTextChannel = false
        }
    }


    fun joinTextChat() {
        // Create a channel listener
        val mRtmChannelListener: RtmChannelListener = object : RtmChannelListener {
            override fun onMemberCountUpdated(i: Int) {}
            override fun onAttributesUpdated(list: List<RtmChannelAttribute>) {}
            override fun onImageMessageReceived(rtmImageMessage: RtmImageMessage, rtmChannelMember: RtmChannelMember) {}
            override fun onFileMessageReceived(rtmFileMessage: RtmFileMessage, rtmChannelMember: RtmChannelMember) {}
            override fun onMemberJoined(member: RtmChannelMember) {}
            override fun onMemberLeft(member: RtmChannelMember) {}
            override fun onMessageReceived(message: RtmMessage, fromMember: RtmChannelMember) {
                when(message.messageType){

                    RtmMessageType.TEXT -> {
                        val text = message.text
                        val fromUser = fromMember.userId
                        val messageText = "$fromUser : $text"
                        writeToMessageHistory(messageText)
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
        }
        try {
            mRtmChannel = mRtmClient!!.createChannel(ChannelID, mRtmChannelListener)
        } catch (_: RuntimeException) {}

        try{
            mRtmChannel!!.join(object : ResultCallback<Void?> {
                override fun onSuccess(responseInfo: Void?) {}
                override fun onFailure(errorInfo: ErrorInfo) {
                    isJoinedTextChannel = false
                }
            })
        } catch (_ : Exception){
            isJoinedTextChannel = false
        }
    }

    private fun openMessageButtonClick(view: View) {

        if(!internetAvailable){
            showSnackbar("Connection is lost!")
            return
        }

        val textField = binding.messageEditText
        textField.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(Repository.MaxTextMessageLength))
        textField.requestFocus()
        val inputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(textField, InputMethodManager.SHOW_IMPLICIT)
    }

    // Button to send channel message
    private fun onClickSendTextMsg(view: View) {

        val message = mRtmClient!!.createMessage()
        message.text = binding.messageEditText.text.toString()
        binding.messageEditText.setText("")

        mRtmChannel!!.sendMessage(message, object : ResultCallback<Void?> {
            override fun onSuccess(aVoid: Void?) {
                val text = "${viewModel.nickName} : ${message.text}"
                writeToMessageHistory(text)
            }
            override fun onFailure(errorInfo: ErrorInfo) {
                val text = "Could not connect properly to the text channel."
                writeToMessageHistory(text)
            }
        })
    }

    // Button to send gift message
    private fun onClickSendGiftMsg(gift: Gift) {

        val message = mRtmClient!!.createMessage(gift.giftByteArray)

        mRtmChannel!!.sendMessage(message, object : ResultCallback<Void?> {
            override fun onSuccess(aVoid: Void?) {
                writeToMessageHistory("Gift message is sent!")

                giftList.add(gift)
                if(!displayingGift)
                    showGift()
            }

            override fun onFailure(errorInfo: ErrorInfo) {
                writeToMessageHistory("The gift message was unable to be sent.")
            }
        })
    }

    private fun writeToMessageHistory(content: String) {

        val currentMessages = messageAdapter.currentList.toMutableList()
        currentMessages.add(Message(content, LocalTime.now()))
        messageAdapter.submitList(currentMessages)
        binding.textRecyclerView.smoothScrollToPosition(messageAdapter.itemCount)
    }



    private fun showGiftMessages(view: View) {

        if(!internetAvailable){
            showSnackbar("Connection is lost!")
            return
        }

        val dialog = BottomSheetDialog(requireContext(), R.style.AppBottomSheetDialogTheme)
        dialog.setContentView(R.layout.dialog_gift_message)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.findViewById<LinearLayout>(R.id.clubLayout)!!.setOnClickListener{ sendGift(Gift(true, clubGift, R.drawable.club)) }
        dialog.findViewById<LinearLayout>(R.id.spadeLayout)!!.setOnClickListener{ sendGift(Gift(true, spadeGift, R.drawable.spade)) }
        dialog.findViewById<LinearLayout>(R.id.diamondLayout)!!.setOnClickListener{ sendGift(Gift(true, diamondGift, R.drawable.diamond)) }
        dialog.findViewById<LinearLayout>(R.id.heartLayout)!!.setOnClickListener{ sendGift(Gift(true, heartGift, R.drawable.heart)) }

        dialog.show()
    }

    private fun sendGift(gift: Gift){
        if(gift.isSended)
            onClickSendGiftMsg(gift)
    }


    private fun showGift(){

        lifecycleScope.launch(Dispatchers.Main){

            val sentGiftIV = binding.sentGiftIV
            displayingGift = true

            sentGiftIV.setImageResource(giftList[0].giftImage!!)
            sentGiftIV.visibility = View.VISIBLE

            //animation
            sentGiftIV.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.zoom_in))
            delay(3000L)
            sentGiftIV.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.zoom_out))
            sentGiftIV.visibility = View.GONE
            delay(1000L)

            giftList.removeAt(0)
            displayingGift = false
            if(giftList.isNotEmpty())
                showGift()
        }
    }

    //endregion text chat section

    //region helper functions
    private fun connectToTextChannel() {
        rtmConnection()
        loginTextChat()
    }

    private fun internetConnection() {

        fun noConnection(){
            exitChannel()
            internetAvailable = false
            binding.connectionLostLayout.visibility = View.VISIBLE
        }

        connectivityObserver = NetworkConnectivity()
        lifecycleScope.launch{
            connectivityObserver.observe(requireContext()).collect{ isConnectedToInternet->

                if(isConnectedToInternet){
                    internetAvailable = true
                    binding.connectionLostLayout.visibility = View.GONE
                }
                else
                    noConnection()
            }
        }

        // first check when open the page
        if(!viewModel.checkForInternetConnection(requireContext())){
            noConnection()
        }
    }

    private fun detectIfKeyboardOpened() {

        val rootView = requireActivity().findViewById<View>(android.R.id.content)
        onGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            val heightDiff = rootView.rootView.height - rootView.height

            // Keyboard is shown
            if (heightDiff > (200f * requireContext().resources.displayMetrics.density + 0.5f).toInt()) {
                binding.textArea.visibility = View.VISIBLE
                binding.bottomLayout.visibility = View.GONE
            }
            // Keyboard is hidden
            else {
                binding.textArea.visibility = View.GONE
                binding.bottomLayout.visibility = View.VISIBLE
            }
        }
        rootView.viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)
    }

    private fun buttonClickEvents() {
        binding.joinLeaveButton.setOnClickListener(::joinOrLeaveChannel)
        binding.giftButton.setOnClickListener(::showGiftMessages)
        binding.chatButton.setOnClickListener(::openMessageButtonClick)
        binding.sendTextMessageButton.setOnClickListener(::onClickSendTextMsg)
        binding.smallVideoViewContainer.setOnClickListener(::switchViewContainers)
    }

    fun showToastMessage(message: String?) {
        lifecycleScope.launch{
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun showSnackbar(content: String){
        Snackbar.make(requireView(), content, Snackbar.LENGTH_SHORT).show()
    }
    //endregion helper functions
}