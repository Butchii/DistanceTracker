package com.example.distancetracker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import org.osmdroid.util.GeoPoint

class ForeGroundService : Service() {
    private val binder = MyBinder()

    private val CHANNEL_ID = "123"

    private var timeInSeconds = 0
    private val handler = Handler(Looper.getMainLooper())
    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            timeInSeconds++

            updateNotification(timeInSeconds)

            handler.postDelayed(this, 1000)
        }
    }

    private var isPaused = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(1, getNotification())

        when (intent?.action) {
            "PAUSE_TIMER" -> pauseTimer()
            "STOP_TIMER" -> stopTimer()
        }

        if (!isPaused) {
            handler.post(updateTimeRunnable)
        }

        return START_STICKY
    }

    private fun pauseTimer() {
        if (isPaused) {
            isPaused = false
            return
        }
        isPaused = true
        handler.removeCallbacks(updateTimeRunnable)
    }

    private fun stopTimer() {
        stopSelf()
        handler.removeCallbacks(updateTimeRunnable)
    }


    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateTimeRunnable)
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Location Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )

            serviceChannel.setSound(null, null)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun getNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder =
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Distance Tracker")
                .setContentText("Paused: $timeInSeconds s")
                .setSmallIcon(R.drawable.avg_icon)
                .setContentIntent(pendingIntent).setOngoing(true)
                .setOngoing(true)
                .addAction(0, "Pause", getPausePendingIntent())
                .addAction(0, "Stop", getStopPendingIntent())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        return builder.build()
    }

    private fun getPausePendingIntent(): PendingIntent {
        val pauseIntent = Intent(this, ForeGroundService::class.java).apply {
            action = "PAUSE_TIMER"
        }
        return PendingIntent.getService(this, 0, pauseIntent, PendingIntent.FLAG_MUTABLE)
    }

    private fun getStopPendingIntent(): PendingIntent {
        val stopIntent = Intent(this, ForeGroundService::class.java).apply {
            action = "STOP_TIMER"
        }
        return PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_MUTABLE)
    }

    private fun updateNotification(seconds: Int) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder =
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Distance Tracker")
                .setContentText("Recording: $seconds s")
                .setSmallIcon(R.drawable.avg_icon)
                .setContentIntent(pendingIntent).setOngoing(true)
                .addAction(0, "Pause", getPausePendingIntent())
                .addAction(0, "Stop", getStopPendingIntent())
                .build()

        startForeground(1, builder)
    }

    fun getDuration(): Int {
        return timeInSeconds
    }


    inner class MyBinder : Binder() {
        fun getService() = this@ForeGroundService
    }
}