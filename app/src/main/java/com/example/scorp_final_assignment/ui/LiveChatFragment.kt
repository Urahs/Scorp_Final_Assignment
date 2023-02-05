package com.example.scorp_final_assignment.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import com.example.scorp_final_assignment.adapters.MessageAdapter
import com.example.scorp_final_assignment.R
import com.example.scorp_final_assignment.databinding.FragmentLiveChatBinding
import com.example.scorp_final_assignment.internet_connectivity.ConnectivityObserver
import com.example.scorp_final_assignment.internet_connectivity.NetworkConnectivityObserver
import com.example.scorp_final_assignment.repository.Repository
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
import kotlinx.coroutines.flow.collect
import java.time.LocalTime


class LiveChatFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()

    private var _binding: FragmentLiveChatBinding? = null
    private val binding get() = _binding!!

    var messageAdapter = MessageAdapter()
    var isJoinedVideoChannel = false
    var isJoinedTextChannel = false

    //region video chat variables
    private var localSurfaceView: SurfaceView? = null
    private var remoteSurfaceView: SurfaceView? = null
    private var agoraEngine: RtcEngine? = null
    private val uid = 0
    //endregion

    //region message chat variables
    private var mRtmClient: RtmClient? = null
    private var mRtmChannel: RtmChannel? = null
    private var jobList = mutableListOf<Repository.Gift>()
    private var jobContinue = false
    //endregion


    private lateinit var connectivityObserver: ConnectivityObserver
    var internetAvailable = false


    //region fragment override functions
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentLiveChatBinding.inflate(inflater, container, false)
        binding.textRecyclerView.adapter = messageAdapter

        connectivityObserver = NetworkConnectivityObserver(requireContext())

        lifecycleScope.launch{
            connectivityObserver.observe().collect{ status->
                if(status == ConnectivityObserver.Status.Avaliable){
                    internetAvailable = true
                    binding.connectionLostIV.visibility = View.GONE
                    binding.connectionLostTV.visibility = View.GONE
                }
                else {
                    internetAvailable = false
                    binding.connectionLostIV.visibility = View.VISIBLE
                    binding.connectionLostTV.visibility = View.VISIBLE
                }
            }
        }


        rtmConnection()

        binding.giftButton.setImageResource(R.drawable.gift_image)
        binding.chatButton.setImageResource(R.drawable.chat_image)
        binding.connectionLostIV.setImageResource(R.drawable.ic_connection_error)

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.JoinButton.setOnClickListener {
            connectChannels()
        }

        binding.LeaveButton.setOnClickListener {
            leaveChannel()
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


        val rootView = requireActivity().findViewById<View>(android.R.id.content)
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val heightDiff = rootView.rootView.height - rootView.height

            // Keyboard is shown
            if (heightDiff > dpToPx(requireActivity(), 200f)) {
                binding.textArea.visibility = View.VISIBLE
                binding.JoinButton.visibility = View.GONE
                binding.LeaveButton.visibility = View.GONE
                binding.chatButton.visibility = View.GONE
                binding.giftButton.visibility = View.GONE
            }
            // Keyboard is hidden
            else {
                binding.textArea.visibility = View.GONE
                binding.JoinButton.visibility = View.VISIBLE
                binding.LeaveButton.visibility = View.VISIBLE
                binding.chatButton.visibility = View.VISIBLE
                binding.giftButton.visibility = View.VISIBLE
            }
        }
    }

    fun dpToPx(context: Context, dp: Float): Int {
        return (dp * context.resources.displayMetrics.density + 0.5f).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mRtmChannel!!.leave(null)
        mRtmClient!!.logout(null)
        _binding = null
    }
    //endregion


    private fun connectChannels(){

        if(!internetAvailable){
            showSnackbar("Connection is lost!")
            return
        }

        if(!isJoinedVideoChannel){
            isJoinedVideoChannel = true
            setupVideoSDKEngine()
            joinVideoChannel()
        }

        if(!isJoinedTextChannel){
            isJoinedTextChannel = true
            loginTextChat()
            joinTextChat()
        }

        if(!isJoinedVideoChannel)
            showToastMessage("Couldn't connect to the video channel!")

        if(!isJoinedTextChannel)
            showToastMessage("Couldn't connect to the text channel!")
    }

    //region video chat section

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

            // Set the remote video view
            activity!!.runOnUiThread {
                setupRemoteVideo(uid)
            }
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            isJoinedVideoChannel = true
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            showToastMessage("Remote user offline")

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

    fun leaveChannel() {

        if(!internetAvailable){
            showSnackbar("Connection is lost!")
            return
        }

        if (!isJoinedVideoChannel) {
            showToastMessage("Join the channel first")
        } else {
            agoraEngine!!.leaveChannel()
            showToastMessage("You left the channel")
            // Stop remote video rendering.
            if (remoteSurfaceView != null) remoteSurfaceView!!.visibility = View.GONE
            // Stop local video rendering.
            if (localSurfaceView != null) localSurfaceView!!.visibility = View.GONE
            isJoinedVideoChannel = false
        }
    }

    fun showToastMessage(message: String?) {
        lifecycleScope.launch{
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun showSnackbar(content: String){
        Snackbar.make(requireView(), content, Snackbar.LENGTH_SHORT).show()
    }

    //endregion


    //region text chat section

    fun loginTextChat() {
        // Log in to Signaling
        try {
            mRtmClient!!.login(Token, viewModel.nickName, object : ResultCallback<Void?> {
                override fun onSuccess(responseInfo: Void?) {}
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

    private fun openMessageButtonClick() {

        if(!internetAvailable){
            showSnackbar("Connection is lost!")
            return
        }

        val textField = binding.textField
        textField.requestFocus()
        val inputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(textField, InputMethodManager.SHOW_IMPLICIT)
    }

    // Button to send channel message
    fun onClickSendChannelMsg() {

        val message = mRtmClient!!.createMessage()
        message.text = binding.textField.text.toString()
        binding.textField.setText("")

        mRtmChannel!!.sendMessage(message, object : ResultCallback<Void?> {
            override fun onSuccess(aVoid: Void?) {
                val text = "${viewModel.nickName} : ${message.text}"
                writeToMessageHistory(text)
            }
            override fun onFailure(errorInfo: ErrorInfo) {
                val text = "Couldn't connect to the text channel properly"
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
                val text = "Gift message couldn't sent"
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

        if(!internetAvailable){
            showSnackbar("Connection is lost!")
            return
        }

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

        dialog.findViewById<LinearLayout>(R.id.clubLayout)!!.setOnClickListener{ sendGift(Gift(true, clubGift, R.drawable.club)) }
        dialog.findViewById<LinearLayout>(R.id.spadeLayout)!!.setOnClickListener{ sendGift(Gift(true, spadeGift, R.drawable.spade)) }
        dialog.findViewById<LinearLayout>(R.id.diamondLayout)!!.setOnClickListener{ sendGift(Gift(true, diamondGift, R.drawable.diamond)) }
        dialog.findViewById<LinearLayout>(R.id.heartLayout)!!.setOnClickListener{ sendGift(Gift(true, heartGift, R.drawable.heart)) }

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