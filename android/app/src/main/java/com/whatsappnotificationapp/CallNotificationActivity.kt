package com.whatsappnotificationapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CallNotificationActivity : AppCompatActivity() {

    private var callerName: String? = null
    private var callId: String? = null
    private var callType: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupFullScreenWindow()
        setContentView(R.layout.activity_call_notification)
        
        extractCallData()
        setupUI()
        
        autoRejectAfterTimeout()
    }
    
    private fun setupFullScreenWindow() {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        )
    }
    
    private fun extractCallData() {
        callerName = intent.getStringExtra("caller_name") ?: "Unknown"
        callId = intent.getStringExtra("call_id") ?: ""
        callType = intent.getStringExtra("call_type") ?: "voice"
    }
    
    private fun setupUI() {
        val callerNameText = findViewById<TextView>(R.id.callerName)
        val callTypeText = findViewById<TextView>(R.id.callType)
        val acceptButton = findViewById<Button>(R.id.acceptButton)
        val rejectButton = findViewById<Button>(R.id.rejectButton)
        
        callerNameText.text = callerName
        callTypeText.text = "Incoming ${callType} call"
        
        acceptButton.setOnClickListener { acceptCall() }
        rejectButton.setOnClickListener { rejectCall() }
    }
    
    private fun stopRingtoneService() {
        val ringtoneIntent = Intent(this, CallRingtoneService::class.java)
        ringtoneIntent.action = CallRingtoneService.ACTION_STOP_RINGTONE
        startService(ringtoneIntent)
    }
    
    private fun acceptCall() {
        stopRingtoneService()
        
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("action", "call_accepted")
        intent.putExtra("call_id", callId)
        intent.putExtra("caller_name", callerName)
        intent.putExtra("call_type", callType)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        
        setResult(Activity.RESULT_OK)
        finish()
    }
    
    private fun rejectCall() {
        stopRingtoneService()
        
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("action", "call_rejected")
        intent.putExtra("call_id", callId)
        intent.putExtra("caller_name", callerName)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        
        setResult(Activity.RESULT_CANCELED)
        finish()
    }
    
    private fun autoRejectAfterTimeout() {
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing) {
                rejectCall()
            }
        }, 30000) // 30 seconds timeout
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopRingtoneService()
    }
} 