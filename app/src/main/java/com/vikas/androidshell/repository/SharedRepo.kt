package com.vikas.androidshell.repository

import androidx.lifecycle.ViewModelProvider
import com.vikas.androidshell.ViewModels.MainActivityViewModel

class SharedRepo {

    private val activityViewModel = ViewModelProvider.AndroidViewModelFactory().create(
        MainActivityViewModel::class.java
    )

    init {
        activityViewModel.startADBServer()
    }

    fun sendCommand(cmd:String){
        activityViewModel.adb.sendToShellProcess(cmd)
    }

    fun getMessage()=activityViewModel.outputData.value
}