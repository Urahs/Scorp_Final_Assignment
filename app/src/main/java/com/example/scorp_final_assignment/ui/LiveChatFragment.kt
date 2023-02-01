package com.example.scorp_final_assignment.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.scorp_final_assignment.MainViewModel
import com.example.scorp_final_assignment.databinding.FragmentLiveChatBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class LiveChatFragment : Fragment() {

    private val viewModel: MainViewModel by viewModels()

    private var _binding: FragmentLiveChatBinding? = null
    private val binding get() = _binding!!

    //SurfaceView to render local video in a Container.
    private var localSurfaceView: SurfaceView? = null
    //SurfaceView to render Remote video in a Container.
    private var remoteSurfaceView: SurfaceView? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentLiveChatBinding.inflate(inflater, container, false)

        setupVideoSDKEngine()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.setupRemoteVideo.observe(viewLifecycleOwner){
            lifecycleScope.launch(Dispatchers.IO) {
                setupRemoteVideo()
            }
        }

        viewModel.remoteSurfaceViewVisibility.observe(viewLifecycleOwner){
            lifecycleScope.launch(Dispatchers.IO) {
                remoteSurfaceView!!.visibility = View.GONE
            }
        }


        binding.JoinButton.setOnClickListener {
            joinChannel()
        }

        binding.LeaveButton.setOnClickListener {
            leaveChannel()
        }
    }

    private fun setupVideoSDKEngine() {
        viewModel.setupVideoSDKEngine()
    }

    private fun setupRemoteVideo() {
        val container = binding.remoteVideoViewContainer
        remoteSurfaceView = SurfaceView(this.context)
        remoteSurfaceView!!.setZOrderMediaOverlay(true)
        container.addView(remoteSurfaceView)

        viewModel.setupRemoteVideo(remoteSurfaceView)

        // Display RemoteSurfaceView.
        remoteSurfaceView!!.visibility = View.VISIBLE
    }

    private fun setupLocalVideo() {
        val container = binding.localVideoViewContainer
        // Create a SurfaceView object and add it as a child to the FrameLayout.
        localSurfaceView = SurfaceView(this.context)
        container.addView(localSurfaceView)
        // Pass the SurfaceView object to Agora so that it renders the local video.
        viewModel.setupLocalVideo(localSurfaceView)
    }

    fun joinChannel() {
        setupLocalVideo()
        localSurfaceView!!.visibility = View.VISIBLE
        viewModel.joinChannel()
    }

    fun leaveChannel() {
        if (!viewModel.isJoined) {
            showMessage("Join a channel first")
        } else {
            showMessage("You left the channel")
            // Stop remote video rendering.
            if (remoteSurfaceView != null) remoteSurfaceView!!.visibility = View.GONE
            // Stop local video rendering.
            if (localSurfaceView != null) localSurfaceView!!.visibility = View.GONE
            leaveChannel()
        }
    }

    fun showMessage(message: String?) {
        lifecycleScope.launch(Dispatchers.IO) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}