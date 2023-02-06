package com.example.scorp_final_assignment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import androidx.lifecycle.ViewModel


/*
@HiltViewModel
class MainViewModel @Inject constructor(
    private val application: Application
): ViewModel() {
*/
class MainViewModel(): ViewModel() {

    lateinit var nickName: String
        private set

    fun changeNickName(value: String){
        nickName = value
    }




}