package com.vikas.androidshell.ViewModels

import android.app.Application
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.vikas.androidshell.R
import com.vikas.androidshell.utils.ADB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File


class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val _outputText = MutableLiveData<String>()
    val outputData: LiveData<String> = _outputText

    val adb = ADB.getInstance(getApplication<Application>().applicationContext)

    init {
        startOutputThread()
    }

    /**
     * Start a death listener to restart the shell once it dies
     */
    private fun startShellDeathThread() {
        viewModelScope.launch(Dispatchers.IO) {
            adb.waitForDeathAndReset()
        }
    }

    fun startADBServer(callback: ((Boolean) -> (Unit))? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = adb.initServer()
            if (success)
                startShellDeathThread()
            callback?.invoke(success)
        }
    }

    /**
     * Continuously update shell output
     */
    private fun startOutputThread() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val out = readOutputFile(adb.outputBufferFile)
                val currentText = _outputText.value
                if (out != currentText) {
                    _outputText.postValue(out)
                }
                Thread.sleep(ADB.OUTPUT_BUFFER_DELAY_MS)
            }
        }
    }

    /**
     * Read the content of the ABD output file
     */
    private fun readOutputFile(file: File): String {
        val out = ByteArray(adb.getOutputBufferSize())

        synchronized(file) {
            if (!file.exists())
                return ""

            file.inputStream().use {
                val size = it.channel.size()

                if (size <= out.size)
                    return String(it.readBytes())

                val newPos = (it.channel.size() - out.size)
                it.channel.position(newPos)
                it.read(out)
            }
        }
        return String(out)
    }
}