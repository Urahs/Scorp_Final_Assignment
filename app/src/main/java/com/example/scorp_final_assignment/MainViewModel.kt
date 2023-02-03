package com.example.scorp_final_assignment

import androidx.lifecycle.ViewModel

/*
@HiltViewModel
class MainViewModel @Inject constructor(
    private val application: Application
): ViewModel() {
*/
class MainViewModel(): ViewModel() {

    /*
    private val _nickName: MutableLiveData<String> = MutableLiveData("selim boi")
    val nickName: LiveData<String> = _nickName
     */

    var nickName: String = "aaa"
        private set

    fun changeNickName(value: String){
        nickName = value
    }
}