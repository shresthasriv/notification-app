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

class NotificationModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "NotificationModule"

    @ReactMethod
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "high_importance_channel"
            val name = "High Importance Notifications"
            val descriptionText = "Push notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
            }
            
            val notificationManager = reactContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
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
    fun getInitialNotification(promise: Promise) {
        try {
            val activity = reactContext.currentActivity
            if (activity != null) {
                val intent = activity.intent
                val extras = intent.extras
                
                if (extras != null) {
                    val notificationData = Arguments.createMap()
                    
                    // Get all the extras from the intent
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
                    
                    // Check if this is a notification intent
                    if (notificationData.hasKey("type") || notificationData.hasKey("sender") || notificationData.hasKey("message")) {
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

    companion object {
        private const val TAG = "NotificationModule"
    }
} 