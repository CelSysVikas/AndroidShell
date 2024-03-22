package com.vikas.androidshell

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.vikas.androidshell.ViewModels.MainActivityViewModel
import com.vikas.androidshell.utils.AppPreference

class UnlockPhoneService : Service() {

    private lateinit var mainActivityViewModel: MainActivityViewModel
    private lateinit var appPreferences: AppPreference

    override fun onCreate() {
        super.onCreate()

        appPreferences = AppPreference.getInstance(application)
        mainActivityViewModel = MainActivityViewModel(application)

        if (!appPreferences.getConnection()){
            mainActivityViewModel.startADBServer {
                if (it)
                    mainActivityViewModel.adb.sendToShellProcess("adb devices")
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {

        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

    }
}