package com.example.scorp_final_assignment.ui

import android.Manifest
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.example.scorp_final_assignment.MainViewModel
import com.example.scorp_final_assignment.R
import com.example.scorp_final_assignment.databinding.FragmentHomeBinding
import com.example.scorp_final_assignment.repository.Repository.MinNickNameLength
import kotlinx.coroutines.launch
import java.time.LocalTime

class HomeFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()){ permissions->
        var permissionCounter = 0
        permissions.forEach{
            if(it.value)
                permissionCounter++
        }
        if(permissionCounter == permissions.size){
            viewModel.changeNickName(binding.nickNameTV.text.toString())
            findNavController().navigate(R.id.action_homeFragment_to_liveChatFragment)
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

        handleNickName()

        return binding.root
    }

    fun isEnglishAlphabet(text: String): Boolean {
        val pattern = Regex("^[a-zA-Z]+$")
        return pattern.matches(text)
    }


    private fun handleNickName() {
        val nickNameTextField = binding.nickNameTV
        binding.progressButton.isEnabled = false

        nickNameTextField.doOnTextChanged{ text, start, before, count ->
            var errorMessage = ""
            if (text!!.length < MinNickNameLength)
                errorMessage += "Nickname should be more than 4 characters!\n"
            if(!isEnglishAlphabet(text.toString()))
                errorMessage += "Nickname should be consist of english alphabet!"

            binding.tilName.error = errorMessage
            binding.progressButton.isEnabled = text.length >= MinNickNameLength && isEnglishAlphabet(text.toString())
        }
    }

    private fun progressButtonClicked(){

        var errorMessage = "Following permission(s) was not granted\n\n"
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
            dialog.setContentView(R.layout.dialog_custom_permission)
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