package com.vikas.androidshell.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.vikas.androidshell.R
import com.vikas.androidshell.UnlockPhoneService
import com.vikas.androidshell.ViewModels.MainActivityViewModel
import com.vikas.androidshell.databinding.ActivityMainBinding
import com.vikas.androidshell.utils.AppPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val mainActivityViewModel: MainActivityViewModel by viewModels()
    private lateinit var dataBinding: ActivityMainBinding
    private lateinit var appPreference: AppPreference
    private var lastCommand = ""

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        appPreference = AppPreference.getInstance(application)

        sendCommandToADB()
        pairAndStart()

        supportFragmentManager.beginTransaction().replace(R.id.fragmentLayout, CommandFragment()).commit()

        dataBinding.btn.setOnClickListener {
            if (dataBinding.editText.text.trim().isEmpty())
                return@setOnClickListener
            else {
                sendCommandToADB()
            }
        }
    }

    private fun sendCommandToADB() {
        startService(Intent(this, UnlockPhoneService::class.java))

        val text = dataBinding.editText.text.trim().toString()
        lastCommand = text
        dataBinding.editText.text = null
        lifecycleScope.launch(Dispatchers.IO) {
            mainActivityViewModel.adb.sendToShellProcess(text)
        }
    }

    private fun pairAndStart() {

        if (appPreference.getConnection()) {
            mainActivityViewModel.adb.debug("Requesting pairing information")
            pairDialogue { thisPairSuccess ->
                if (thisPairSuccess) {
                    appPreference.saveConnection(true)
                    mainActivityViewModel.startADBServer()
                } else {
                    mainActivityViewModel.adb.debug("Failed to pair! Trying again...")
                    pairAndStart()
                }
            }
        } else {
            mainActivityViewModel.startADBServer()
        }
    }

    private fun pairDialogue(onSubmit: (Boolean) -> Unit) {
        val setupPortDialog = Dialog(this)
        setupPortDialog.setContentView(R.layout.input_port)

        val localHostPort = setupPortDialog.findViewById<EditText>(R.id.port)
        val debugSetUpPin = setupPortDialog.findViewById<EditText>(R.id.pin)
        val btnSetUp = setupPortDialog.findViewById<Button>(R.id.btnSetUpConnection)

        btnSetUp.setOnClickListener {
            val portText = localHostPort.text.trim().toString()
            val pinText = debugSetUpPin.text.trim().toString()

            if (portText.isEmpty()) {
                localHostPort.error = "Please enter a port"
                return@setOnClickListener
            }

            if (pinText.isEmpty()) {
                debugSetUpPin.error = "Please enter a pin"
                return@setOnClickListener
            }

            setupPortDialog.dismiss()
            lifecycleScope.launch(Dispatchers.IO) {
                mainActivityViewModel.adb.debug("Trying to pair...")
                val success = mainActivityViewModel.adb.pair(portText, pinText)
                onSubmit(success)
            }

            setupPortDialog.dismiss()
        }

        setupPortDialog.setCancelable(false)
        setupPortDialog.show()
    }
}