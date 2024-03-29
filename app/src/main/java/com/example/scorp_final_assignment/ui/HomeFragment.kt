package com.example.scorp_final_assignment.ui

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.NavHostFragment.findNavController
import androidx.navigation.fragment.findNavController
import com.example.scorp_final_assignment.R
import com.example.scorp_final_assignment.databinding.FragmentHomeBinding
import com.example.scorp_final_assignment.repository.Repository.MinNickNameLength

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()){ permissions->
        var permissionCounter = 0
        permissions.forEach{
            if(it.value)
                permissionCounter++
        }
        if(permissionCounter == permissions.size)
            findNavController().navigate(R.id.action_homeFragment_to_liveChatFragment)
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

        handleNickName()

        return binding.root
    }


    private fun handleNickName() {
        val nickNameTextField = binding.nickNameTV
        binding.progressButton.isEnabled = false
        nickNameTextField.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(currentText: Editable?) {
                binding.progressButton.isEnabled = currentText.toString() != "" && currentText.toString().length >= MinNickNameLength
            }
        })
    }


    private fun progressButtonClicked(){

        var errorMessage = "You should give the following permission(s) before continue\n\n"
        var permissionsGranted = true
        if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
            errorMessage += " ➜ CAMERA\n"
            permissionsGranted = false
        }
        if(shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)){
            errorMessage += " ➜ AUDIO\n"
            permissionsGranted = false
        }

        if(permissionsGranted){
            requestPermission.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
        else{
            val dialog = Dialog(requireContext())
            dialog.setContentView(R.layout.custom_permission_dialog)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            errorMessage += "\nYou may want to go \"Settings > Apps\" and allow the required permissions"
            dialog.findViewById<TextView>(R.id.messageTV).text = errorMessage
            val okayBtn = dialog.findViewById<Button>(R.id.okayBtn)
            okayBtn.setOnClickListener {
                dialog.dismiss()
            }
            dialog.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}