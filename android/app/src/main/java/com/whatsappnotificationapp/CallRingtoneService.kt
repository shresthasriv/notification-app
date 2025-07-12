package com.whatsappnotificationapp

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.IBinder
import android.util.Log
import java.io.IOException

class CallRingtoneService : Service() {
    
    private var mediaPlayer: MediaPlayer? = null
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        
        when (action) {
            ACTION_START_RINGTONE -> {
                startRingtone()
            }
            ACTION_STOP_RINGTONE -> {
                stopRingtone()
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }
    
    private fun startRingtone() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                return
            }
            
            val ringtoneUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, ringtoneUri)
                isLooping = true
                prepare()
                start()
            }
            
            Log.d(TAG, "Ringtone started with MediaPlayer")
        } catch (e: IOException) {
            Log.e(TAG, "Error starting ringtone", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting ringtone", e)
        }
    }
    
    private fun stopRingtone() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
            Log.d(TAG, "Ringtone stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping ringtone", e)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopRingtone()
    }
    
    companion object {
        private const val TAG = "CallRingtoneService"
        const val ACTION_START_RINGTONE = "START_RINGTONE"
        const val ACTION_STOP_RINGTONE = "STOP_RINGTONE"
    }
} 