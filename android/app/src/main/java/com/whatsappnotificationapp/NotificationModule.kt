package com.whatsappnotificationapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.facebook.react.bridge.*
import com.google.firebase.messaging.FirebaseMessaging
import android.util.Log
import android.media.RingtoneManager

class NotificationModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "NotificationModule"

    @ReactMethod
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val messageChannelId = "high_importance_channel"
            val messageName = "High Importance Notifications"
            val messageDescription = "Push notifications"
            val messageImportance = NotificationManager.IMPORTANCE_HIGH
            val messageChannel = NotificationChannel(messageChannelId, messageName, messageImportance).apply {
                description = messageDescription
                enableVibration(true)
                enableLights(true)
            }
            
            val callChannelId = "call_channel"
            val callName = "Call Notifications"
            val callDescription = "Incoming call notifications"
            val callImportance = NotificationManager.IMPORTANCE_HIGH
            val callChannel = NotificationChannel(callChannelId, callName, callImportance).apply {
                description = callDescription
                enableVibration(true)
                enableLights(true)
                setBypassDnd(true)
                setSound(null, null)
            }
            
            val notificationManager = reactContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(messageChannel)
            notificationManager.createNotificationChannel(callChannel)
        }
    }

    @ReactMethod
    fun showCustomNotification(title: String, body: String, data: ReadableMap) {
        val intent = Intent(reactContext, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        
        val iterator = data.keySetIterator()
        while (iterator.hasNextKey()) {
            val key = iterator.nextKey()
            when (val value = data.getDynamic(key)) {
                is String -> intent.putExtra(key, value)
                is Int -> intent.putExtra(key, value)
                is Boolean -> intent.putExtra(key, value)
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            reactContext, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(reactContext, "high_importance_channel")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationManager = reactContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    @ReactMethod
    fun showCallNotification(callerName: String, callType: String, callId: String) {
        // Start ringtone service for continuous sound
        val ringtoneIntent = Intent(reactContext, CallRingtoneService::class.java)
        ringtoneIntent.action = CallRingtoneService.ACTION_START_RINGTONE
        reactContext.startService(ringtoneIntent)
        
        val fullScreenIntent = Intent(reactContext, CallNotificationActivity::class.java).apply {
            putExtra("caller_name", callerName)
            putExtra("call_type", callType)
            putExtra("call_id", callId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        
        val fullScreenPendingIntent = PendingIntent.getActivity(
            reactContext, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val acceptIntent = Intent(reactContext, CallActionReceiver::class.java).apply {
            action = "ACCEPT_CALL"
            putExtra("call_id", callId)
            putExtra("caller_name", callerName)
        }
        val acceptPendingIntent = PendingIntent.getBroadcast(
            reactContext, 1, acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val rejectIntent = Intent(reactContext, CallActionReceiver::class.java).apply {
            action = "REJECT_CALL"
            putExtra("call_id", callId)
        }
        val rejectPendingIntent = PendingIntent.getBroadcast(
            reactContext, 2, rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(reactContext, "call_channel")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$callerName")
            .setContentText("Incoming $callType call")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .setOngoing(true)
            .setSound(null)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_LIGHTS)
            .addAction(R.drawable.ic_call_end, "Reject", rejectPendingIntent)
            .addAction(R.drawable.ic_call, "Accept", acceptPendingIntent)

        val notificationManager = reactContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(callId.hashCode(), notificationBuilder.build())
    }

    @ReactMethod
    fun getInitialNotification(promise: Promise) {
        try {
            val activity = reactContext.currentActivity
            if (activity != null) {
                val intent = activity.intent
                val extras = intent.extras
                
                if (extras != null) {
                    val notificationData = Arguments.createMap()
                    
                    for (key in extras.keySet()) {
                        val value = extras.get(key)
                        when (value) {
                            is String -> notificationData.putString(key, value)
                            is Int -> notificationData.putInt(key, value)
                            is Boolean -> notificationData.putBoolean(key, value)
                            is Double -> notificationData.putDouble(key, value)
                            else -> notificationData.putString(key, value.toString())
                        }
                    }
                    
                    if (notificationData.hasKey("type") || notificationData.hasKey("sender") || notificationData.hasKey("message") || notificationData.hasKey("action")) {
                        Log.d(TAG, "Found initial notification data: $notificationData")
                        promise.resolve(notificationData)
                    } else {
                        promise.resolve(null)
                    }
                } else {
                    promise.resolve(null)
                }
            } else {
                promise.resolve(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting initial notification", e)
            promise.resolve(null)
        }
    }

    @ReactMethod
    fun getFCMToken(promise: Promise) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                promise.reject("TOKEN_ERROR", "Failed to get FCM token", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d(TAG, "FCM Registration Token: $token")
            promise.resolve(token)
        }
    }

    @ReactMethod
    fun clearAllNotifications(promise: Promise) {
        try {
            val notificationManager = reactContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancelAll()
            promise.resolve("All notifications cleared")
        } catch (e: Exception) {
            promise.reject("CLEAR_ERROR", "Failed to clear notifications", e)
        }
    }

    @ReactMethod
    fun clearCallNotification(callId: String) {
        try {
            val notificationManager = reactContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(callId.hashCode())
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing call notification", e)
        }
    }

    companion object {
        private const val TAG = "NotificationModule"
    }
} 