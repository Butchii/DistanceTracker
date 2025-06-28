package com.example.distancetracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.osmdroid.util.GeoPoint
import java.util.Locale

class RecordingService : Service() {
    private var serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    lateinit var locationClient: LocationClient
    private lateinit var timerClient: TimerClient

    private val CHANNEL_ID = "123"

    var geoPoints: ArrayList<GeoPoint> = ArrayList()

    private var recording: Boolean = false

    private lateinit var notificationIntent: Intent
    private lateinit var pendingIntent: PendingIntent
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()

        notificationIntent = Intent(this, MainActivity::class.java)
        pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel()
        locationClient = DefaultLocationClient(
            applicationContext,
            LocationServices.getFusedLocationProviderClient(applicationContext), null, null
        )
        timerClient = TimerClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            addStartMarkerLocation(intent)
        }

        when (intent?.action) {
            ACTION_RECORD -> checkState()
            ACTION_RESET -> reset()
            ACTION_PAUSE -> pause()
        }
        return START_STICKY
    }

    private fun addStartMarkerLocation(intent: Intent) {
        val startMarkerLatitude = intent.getStringExtra("latitude")
        val startMarkerLongitude = intent.getStringExtra("longitude")
        Log.d("myTag",String.format("$startMarkerLatitude $startMarkerLongitude"))
        if (startMarkerLatitude != null && startMarkerLongitude != null) {
            val startMarkerLocation =
                GeoPoint(startMarkerLatitude.toDouble(), startMarkerLongitude.toDouble())
            geoPoints.add(startMarkerLocation)

            locationClient.currentLocation.latitude = startMarkerLatitude.toDouble()
            locationClient.currentLocation.longitude = startMarkerLongitude.toDouble()
        }
    }

    private fun reset() {
        geoPoints.clear()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun pauseTimer() {
        timerClient.stopTimer()
    }

    private fun pause() {
        pauseTimer()
        serviceScope.cancel()

        val notification = createPauseNotification(pendingIntent)

        notificationManager.notify(1, notification.build())
    }

    private fun checkState() {
        recording = if (recording) {
            pause()
            false
        } else {
            start()
            true
        }
    }

    private fun start() {
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        val notification = createRecordNotification(pendingIntent)

        timerClient.startTimer()
        locationClient.getLocationUpdates(1000L).catch { e -> e.printStackTrace() }
            .onEach { location ->
                val lat = location.latitude
                val long = location.longitude
                val lastDistance: Double = locationClient.calculateDistance(lat, long)

                if (timerClient.sessionSeconds > 30) {
                    locationClient.calculateAverageSpeed(timerClient.sessionSeconds)
                }

                if (lastDistance > 0.55) {
                    locationClient.totalDistance += (lastDistance / 1000)
                    geoPoints.add(GeoPoint(lat, long))
                }

                locationClient.currentLocation = GeoPoint(lat, long)
                logInformation(lat, long, lastDistance)
                updateNotification(notification)
            }.launchIn(serviceScope)
        startForeground(1, notification.build())
    }

    private fun updateNotification(notification: NotificationCompat.Builder) {
        val updatedNotification =
            notification.setContentText(
                String.format(
                    Locale.getDefault(),
                    "Recording: ${timerClient.getFormattedSessionDuration()} \nDistance: %.2f km ",
                    locationClient.totalDistance
                )
            )
        notificationManager.notify(1, updatedNotification.build())
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

    private fun getRecordPendingIntent(): PendingIntent {
        val pauseIntent = Intent(this, RecordingService::class.java).apply {
            action = "ACTION_RECORD"
        }
        return PendingIntent.getService(this, 0, pauseIntent, PendingIntent.FLAG_MUTABLE)
    }

    private fun getResetPendingIntent(): PendingIntent {
        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = "ACTION_RESET"
        }
        return PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_MUTABLE)
    }

    companion object {
        const val ACTION_RECORD = "ACTION_RECORD"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESET = "ACTION_RESET"
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    fun sendData(
        latitude: Double,
        longitude: Double,
        sessionDurationInSeconds: Int,
        totalDistance: String,
        avgSpeed: Double,
        geoPoints: ArrayList<GeoPoint>? = null
    ) {
        Intent().run {
            action = DistanceTracker.ACTION_DATA
            this.putExtra("latitude", latitude.toString())
            this.putExtra("longitude", longitude.toString())
            this.putExtra("time", sessionDurationInSeconds.toString())
            this.putExtra("totalDistance", totalDistance)
            this.putExtra("averageSpeed", avgSpeed)
            this.putParcelableArrayListExtra("test", geoPoints)

            this@RecordingService.sendBroadcast(this)
        }
    }

    private fun logInformation(lat: Double, long: Double, lastDistance: Double) {
        Log.d(
            "myTag",
            String.format(
                Locale.getDefault(),
                "new $lat $long old ${locationClient.currentLocation}  distance $lastDistance  total distance ${locationClient.totalDistance}"
            )
        )
    }

    private fun createRecordNotification(pendingIntent: PendingIntent): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Distance Tracker")
            .setContentText(
                String.format(
                    Locale.getDefault(),
                    "Recording: ${timerClient.getFormattedSessionDuration()} \nDistance: %.2f km ",
                    locationClient.totalDistance
                )
            )
            .setSmallIcon(R.drawable.avg_icon)
            .setContentIntent(pendingIntent).setOngoing(true)
            .setOngoing(true)
            .addAction(0, "Reset", getResetPendingIntent())
            .addAction(0, "Pause", getRecordPendingIntent())
    }

    private fun createPauseNotification(pendingIntent: PendingIntent): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Distance Tracker")
            .setContentText(
                String.format(
                    Locale.getDefault(),
                    "Recording: ${timerClient.getFormattedSessionDuration()} \nDistance: %.2f km ",
                    locationClient.totalDistance
                )
            )
            .setSmallIcon(R.drawable.avg_icon)
            .setContentIntent(pendingIntent).setOngoing(true)
            .setOngoing(true)
            .addAction(0, "Reset", getResetPendingIntent())
            .addAction(0, "Resume", getRecordPendingIntent())
    }
}