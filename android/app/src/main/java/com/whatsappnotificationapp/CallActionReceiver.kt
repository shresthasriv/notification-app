package com.whatsappnotificationapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import android.util.Log

class CallActionReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val callId = intent.getStringExtra("call_id") ?: ""
        val callerName = intent.getStringExtra("caller_name") ?: ""
        
        Log.d(TAG, "Call action received: $action for call: $callId")
        
        // Stop ringtone service
        val ringtoneIntent = Intent(context, CallRingtoneService::class.java)
        ringtoneIntent.action = CallRingtoneService.ACTION_STOP_RINGTONE
        context.startService(ringtoneIntent)
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(callId.hashCode())
        
        when (action) {
            "ACCEPT_CALL" -> {
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    putExtra("action", "call_accepted")
                    putExtra("call_id", callId)
                    putExtra("caller_name", callerName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                context.startActivity(mainIntent)
            }
            "REJECT_CALL" -> {
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    putExtra("action", "call_rejected")
                    putExtra("call_id", callId)
                    putExtra("caller_name", callerName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                context.startActivity(mainIntent)
            }
        }
    }
    
    companion object {
        private const val TAG = "CallActionReceiver"
    }
} 