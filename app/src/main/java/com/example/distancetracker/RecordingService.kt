package com.example.distancetracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
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

    private var routePoints: ArrayList<GeoPoint> = ArrayList()

    private var recording: Boolean = false
    private var sessionStarted: Boolean = false

    private lateinit var notificationIntent: Intent
    private lateinit var pendingIntent: PendingIntent
    private lateinit var notificationManager: NotificationManager

    private var autoPauseCounter: Int = 0
    private var autoResumeCounter: Int = 0

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
        when (intent?.action) {
            ACTION_RECORD -> checkState(intent)
            ACTION_RESET -> reset()
        }
        return START_STICKY
    }

    private fun isDistanceTooLow(): Boolean {
        return locationClient.lastDistance < 0.55
    }

    private fun isDistanceValid(): Boolean {
        return locationClient.lastDistance > 0.55
    }

    private fun increasePauseCounter() {
        autoPauseCounter++
    }

    fun checkPauseCounter() {
        if (autoPauseCounter >= 10) {
            pause()
        }
    }

    fun checkResumeCounter() {
        if (autoResumeCounter >= 10) {
            start()
        }
    }

    private fun resetPauseCounter() {
        autoPauseCounter = 0
    }

    private fun resetResumeCounter() {
        autoResumeCounter = 0
    }

    private fun setRecordingState() {
        recording = true
    }

    fun isRecording(): Boolean {
        return recording
    }

    private fun start(intent: Intent? = null) {
        setRecordingState()
        resetPauseCounter()
        resetResumeCounter()
        if (!sessionStarted && intent != null) {
            addStartMarkerLocation(intent)
            sessionStarted = true
        }

        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val notification = createRecordNotification(pendingIntent)

        timerClient.startTimer()
        locationClient.getLocationUpdates(1000L).catch { e -> e.printStackTrace() }
            .onEach { location ->
                val lat = location.latitude
                val long = location.longitude
                locationClient.calculateDistance(lat, long)

                if (recording) {
                    if (isDistanceTooLow()) {
                        increasePauseCounter()
                    }
                } else {
                    if (isDistanceValid()) {
                        increaseResumeCounter()
                    }
                }

                locationClient.calculateAverageSpeed(timerClient.sessionSeconds + timerClient.sessionMinutes * 60 + timerClient.sessionHours * 3600)

                if (locationClient.lastDistance > 0.55) {
                    locationClient.totalDistanceInKilometres += (locationClient.lastDistance / 1000)
                    routePoints.add(GeoPoint(lat, long))
                }
                locationClient.currentLocation = GeoPoint(lat, long)
                updateNotification(notification)
            }.launchIn(serviceScope)
        startForeground(1, notification.build())
    }

    private fun increaseResumeCounter() {
        autoResumeCounter++
    }

    private fun setPauseState() {
        recording = false
    }

    private fun stopLocationUpdates() {
        serviceScope.cancel()
    }

    private fun showPauseNotification() {
        val notification = createPauseNotification(pendingIntent)
        notificationManager.notify(1, notification.build())
    }

    fun pause() {
        setPauseState()
        sendData(
            timerClient.getTotalTimeInSeconds(),
            locationClient.totalAverageSpeed
        )
        stopTimer()
        stopLocationUpdates()
        showPauseNotification()
    }

    private fun stopTimer() {
        timerClient.stopTimer()
    }


    private fun checkState(intent: Intent) {
        recording = if (recording) {
            pause()
            false
        } else {
            start(intent)
            true
        }
    }

    private fun addStartMarkerLocation(intent: Intent) {
        val startMarkerLatitude = intent.getStringExtra("latitude")
        val startMarkerLongitude = intent.getStringExtra("longitude")
        if (startMarkerLatitude != null && startMarkerLongitude != null) {
            val startMarkerLocation =
                GeoPoint(startMarkerLatitude.toDouble(), startMarkerLongitude.toDouble())
            routePoints.add(startMarkerLocation)

            locationClient.currentLocation.latitude = startMarkerLatitude.toDouble()
            locationClient.currentLocation.longitude = startMarkerLongitude.toDouble()
        }
    }

    private fun updateNotification(notification: NotificationCompat.Builder) {
        val updatedNotification =
            notification.setContentText(
                String.format(
                    Locale.getDefault(),
                    "Recording: ${timerClient.getFormattedSessionDuration()} \nDistance: %.2f km ",
                    locationClient.totalDistanceInKilometres
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

    fun sendData(
        sessionDurationInSeconds: Int,
        avgSpeed: Double
    ) {
        Intent().run {
            action = DistanceTracker.ACTION_DATA
            this.putExtra("lastDistance", locationClient.lastDistance.toString())
            this.putExtra("latitude", locationClient.currentLocation.latitude.toString())
            this.putExtra("longitude", locationClient.currentLocation.longitude.toString())
            this.putExtra("time", sessionDurationInSeconds.toString())
            this.putExtra("totalDistance", locationClient.totalDistanceInKilometres.toString())
            this.putExtra("averageSpeed", avgSpeed.toString())
            this.putExtra("recording", recording)
            this.putParcelableArrayListExtra("routePoints", routePoints)

            this@RecordingService.sendBroadcast(this)
        }
    }

    companion object {
        const val ACTION_RECORD = "ACTION_RECORD"
        const val ACTION_RESET = "ACTION_RESET"
    }

    fun logInformation(lat: Double, long: Double) {
        Log.d(
            "myTag",
            String.format(
                Locale.getDefault(),
                "New Latitude: $lat \n" +
                        "New Longitude: $long \n" +
                        "Old Location: ${locationClient.currentLocation} \n" +
                        "Distance walked: ${locationClient.lastDistance} \n" +
                        "Total distance walked: ${locationClient.totalDistanceInKilometres}\n" +
                        "Session duration: ${timerClient.sessionHours}h ${timerClient.sessionMinutes}m ${timerClient.sessionSeconds}s\n" +
                        "Average speed: ${locationClient.totalAverageSpeed}\n" +
                        "Saved geopoints: $routePoints"
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
                    locationClient.totalDistanceInKilometres
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
                    "Paused: ${timerClient.getFormattedSessionDuration()} \nDistance: %.2f km ",
                    locationClient.totalDistanceInKilometres
                )
            )
            .setSmallIcon(R.drawable.avg_icon)
            .setContentIntent(pendingIntent).setOngoing(true)
            .setOngoing(true)
            .addAction(0, "Reset", getResetPendingIntent())
            .addAction(0, "Resume", getRecordPendingIntent())
    }

    private fun reset() {
        timerClient.stopTimer()
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}