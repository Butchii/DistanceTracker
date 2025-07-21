package com.example.distancetracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.distancetracker.controlpanel.ControlPanel
import com.example.distancetracker.topbar.TopBar
import kotlinx.coroutines.cancel
import org.osmdroid.util.GeoPoint

class DistanceTracker(
    private val distanceTrackerLayout: LinearLayout,
    private val context: Context,
    private val mainActivity: MainActivity
) {
    lateinit var topBar: TopBar
    lateinit var mapHelper: MapHelper
    private lateinit var controlPanel: ControlPanel
    private lateinit var broadcastReceiver: BroadcastReceiver

    var isRecording: Boolean = false
    private var isSessionRunning: Boolean = false

    private var sessionDurationInSeconds: Int = 0
    var sessionTotalDistance: Double = 0.0
    var sessionAverageSpeed: Double = 0.0
    var sessionDuration: Int = 0
    var formattedSessionDuration: String = "0h 0m 0s"

    var routePoints: ArrayList<GeoPoint> = ArrayList()

    var isAutoPauseActivated: Boolean = true
    var isAutoResumeActivated: Boolean = true

    private var firstLocation: Boolean = true

    init {
        initializeTopBar()
        initializeMapHelper()
        initializeControlPanel()
        initializeBroadCastReceiver()
        isGPSEnabled()
        isForegroundRunning()
    }

    private fun isGPSEnabled() {
        if (Utility.isGPSEnabled(context)) {
            topBar.gpsIndicator.setImageResource(R.drawable.gps_enabled)
        } else {
            topBar.gpsIndicator.setImageResource(R.drawable.gps_disabled)
        }
    }

    private fun isForegroundRunning() {
        if (Utility.isRecordingServiceRunning(context)) {
            //checks if fore ground service is running
            //if running -> bind to service
            //and create handler which takes data from service every second
            isSessionRunning = true
            startRecording()
            controlPanel.buttonSection.enterRecordingMode()
        } else {
            if (Utility.isGPSEnabled(context)) {
                if (Utility.isLocationPermissionGranted(mainActivity)) {
                    mapHelper.startLocationUpdates()
                } else {
                    Utility.requestLocationPermission(mainActivity)
                }
            } else {
                Utility.showSettingsDialog(mainActivity)
            }
        }
    }

    private fun initializeBroadCastReceiver() {
        broadcastReceiver = DataBroadCastReceiver()
        context.registerReceiver(
            broadcastReceiver,
            intentFilter,
            AppCompatActivity.RECEIVER_EXPORTED
        )
    }

    private fun initializeTopBar() {
        topBar = TopBar(
            context,
            distanceTrackerLayout.findViewById(R.id.topBarLayout),
            this,
            mainActivity
        )
    }

    private fun initializeMapHelper() {
        mapHelper = MapHelper(
            context,
            mainActivity,
            distanceTrackerLayout.findViewById(R.id.map)
        )
    }

    private fun initializeControlPanel() {
        controlPanel =
            ControlPanel(
                context,
                mainActivity,
                distanceTrackerLayout.findViewById(R.id.controlPanelLayout),
                this
            )
    }

    private fun stopRecording() {
        isRecording = false
    }

    fun pauseSession() {
        Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_RECORD
            context.startService(this)
        }
        stopRecording()
    }

    private fun stopSession() {
        isSessionRunning = false
        stopRecording()
    }

    fun resumeSession() {
        startRecording()

        val intent = Intent(context, RecordingService::class.java)
        intent.action = RecordingService.ACTION_RECORD
        context.startService(intent)
    }

    private fun startRecording() {
        isRecording = true
    }

    fun startSession() {
        mapHelper.locationScope.cancel()
        startRecordingService()
        isSessionRunning = true
        startRecording()
        mapHelper.showMap()
        mapHelper.centerOnPoint(mapHelper.currentLocation)
        controlPanel.buttonSection.enterRecordingMode()
        topBar.hideTopBarExpand()
    }

    fun resetSession() {
        stopSession()

        val intent = Intent(context, RecordingService::class.java).apply {
            action = "ACTION_RESET"
        }
        context.startService(intent)

        mapHelper.startLocationUpdates()

        controlPanel.reset()

        routePoints.clear()
        mapHelper.removeRouteFromMap()
        mapHelper.removeEndMarker()
    }

    private fun startRecordingService() {
        val intent = Intent(context, RecordingService::class.java)
        intent.action = RecordingService.ACTION_RECORD
        intent.putExtra("latitude", mapHelper.currentLocation.latitude.toString())
        intent.putExtra("longitude", mapHelper.currentLocation.longitude.toString())
        context.startService(intent)
    }

    companion object {
        const val ACTION_DATA = "package_your_app_DATA"
        private val filters = arrayOf(ACTION_DATA)
        private val intentFilter: IntentFilter by lazy {
            IntentFilter().apply {
                filters.forEach { addAction(it) }
            }
        }
    }

    inner class DataBroadCastReceiver : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                when (intent.action) {
                    ACTION_DATA -> processData(intent)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun processData(intent: Intent) {
        processRecordingState(intent)
        processRoutePoints(intent)
        processSessionDuration(intent)
        processLastDistance(intent)
        processTotalDistance(intent)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun processRoutePoints(intent: Intent) {
        val routePoints =
            intent.getParcelableArrayListExtra(
                "routePoints",
                GeoPoint::class.java
            ) as ArrayList<GeoPoint>

        this.routePoints = routePoints

        if (firstLocation) {
            val startMarkerLocation =
                GeoPoint(this.routePoints[0].latitude, this.routePoints[0].longitude)
            val endMarkerLocation = GeoPoint(
                this.routePoints[this.routePoints.size - 1].latitude,
                this.routePoints[this.routePoints.size - 1].longitude
            )
            mapHelper.updateStartMarkerLocation(
                GeoPoint(
                    startMarkerLocation
                )
            )
            mapHelper.addEndMarker(endMarkerLocation)
            mapHelper.map.controller.animateTo(endMarkerLocation)
            firstLocation = false
        }
        mapHelper.addRouteToMap(routePoints)
    }

    fun unregisterReceiver() {
        context.unregisterReceiver(broadcastReceiver)
    }

    private fun processRecordingState(intent: Intent) {
        val isServiceRecording = intent.getBooleanExtra("recording", true)
        if (!isServiceRecording) {
            stopRecording()
            controlPanel.buttonSection.enterPauseMode()
        }
    }

    private fun processSessionDuration(intent: Intent) {
        val duration = intent.getStringExtra("time")
        if (duration != null) {
            val formattedDuration = Utility.formatSessionTime(duration)
            sessionDurationInSeconds = duration.toInt()
            sessionDuration = sessionDurationInSeconds

            formattedSessionDuration = formattedDuration
            controlPanel.infoSection.updateSessionDuration(formattedDuration)
            if (sessionDurationInSeconds > 25) {
                processAverageSpeed(intent)
            }
        }
    }

    private fun processLastDistance(intent: Intent) {
        val endMarkerLatitude = intent.getStringExtra("latitude")
        val endMarkerLongitude = intent.getStringExtra("longitude")
        val lastDistance = intent.getStringExtra("lastDistance")
        if (endMarkerLatitude != null && endMarkerLongitude != null && lastDistance != null && lastDistance.toDouble() > 0.55) {
            mapHelper.updateEndMarkerLocation(
                GeoPoint(
                    endMarkerLatitude.toDouble(),
                    endMarkerLongitude.toDouble()
                )
            )
            mapHelper.updateCurrentLocation(
                GeoPoint(
                    endMarkerLatitude.toDouble(),
                    endMarkerLongitude.toDouble()
                )
            )
        }
    }

    private fun processAverageSpeed(intent: Intent) {
        val averageSpeed = intent.getStringExtra("averageSpeed")?.toDouble()
        if (averageSpeed != null) {
            controlPanel.infoSection.updateAverageSpeed(averageSpeed.toString())
            sessionAverageSpeed = averageSpeed
        }
    }

    private fun processTotalDistance(intent: Intent) {
        val totalDistance = intent.getStringExtra("totalDistance")?.toDouble()
        if (totalDistance != null) {
            controlPanel.infoSection.updateTotalDistance(totalDistance.toFloat())
            sessionTotalDistance = totalDistance
        }
    }

    fun startLocating(){
        mapHelper.startLocationUpdates()
        topBar.showGPSEnabled()
    }
}
