package com.example.scorp_final_assignment.ui

import android.Manifest
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.scorp_final_assignment.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()){ permissions->
        permissions.forEach{
            val permissionName = it.key
            val isGranted = it.value
            // TODO: is there anything that should I handle here?
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        binding.progressButton.setOnClickListener{
            progressButtonClicked()
        }

        return binding.root
    }


    private fun progressButtonClicked(){
        //findNavController().navigate(R.id.action_homeFragment_to_liveChatFragment)
        var errorMessage = ""
        if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){

        }
        if(shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)){

        }

        if(errorMessage != ""){
            // TODO: add custom dialog
        }
        else{
            requestPermission.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}