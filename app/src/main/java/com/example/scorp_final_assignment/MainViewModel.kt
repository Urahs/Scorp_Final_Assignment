package com.example.scorp_final_assignment

import android.app.Application
import android.content.Context
import android.view.SurfaceView
import android.view.View
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scorp_final_assignment.repository.Repository.AppID
import com.example.scorp_final_assignment.repository.Repository.ChannelName
import com.example.scorp_final_assignment.repository.Repository.Token
import com.example.scorp_final_assignment.repository.Repository.UserId
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.properties.Delegates

/*
@HiltViewModel
class MainViewModel @Inject constructor(
    private val application: Application
): ViewModel() {
*/
class MainViewModel(): ViewModel() {

}