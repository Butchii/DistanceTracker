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

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class DistanceTracker(
    private val distanceTrackerLayout: LinearLayout,
    private val context: Context,
    private val mainActivity: MainActivity
) {
    lateinit var topBar: TopBar
    lateinit var mapHelper: MapHelper
    private lateinit var controlPanel: ControlPanel
    private lateinit var recordingBroadcastReceiver: BroadcastReceiver

    var isRecording: Boolean = false

    var sessionDurationInSeconds: Int = 0
    var sessionTotalDistance: Double = 0.0
    var sessionAverageSpeed: Double = 0.0
    var formattedSessionDuration: String = "0h 0m 0s"

    var isAutoPauseActivated: Boolean = true
    var isAutoResumeActivated: Boolean = true

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

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun initializeBroadCastReceiver() {
        recordingBroadcastReceiver = DataBroadCastReceiver()
        context.registerReceiver(
            recordingBroadcastReceiver,
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

    fun pauseRecording() {
        isRecording = false
    }

    fun startRecording() {
        isRecording = true
        topBar.hideTopBarExpand()
    }

    fun startSession() {
        mapHelper.locationScope.cancel()
        startRecording()
        mapHelper.showMap()
        mapHelper.centerOnPoint(mapHelper.currentLocation)
        controlPanel.buttonSection.enterRecordingMode()
        topBar.hideTopBarExpand()
    }

    fun resetSession() {
        pauseRecording()
        if (Utility.isRecordingServiceRunning(context)) {
            stopRecordingService()
        }
        mapHelper.startLocationUpdates()

        controlPanel.reset()
        mapHelper.firstLocation = true

        mapHelper.resetMap()
    }

    private fun stopRecordingService() {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = "ACTION_STOP"
        }
        context.startService(intent)
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
        processAverageSpeed(intent)
        processResetState(intent)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun processRoutePoints(intent: Intent) {
        val routePoints =
            intent.getParcelableArrayListExtra(
                "routePoints",
                GeoPoint::class.java
            ) as ArrayList<GeoPoint>

        mapHelper.routePoints = routePoints
        if (mapHelper.firstLocation) {
            val startMarkerLocation =
                GeoPoint(mapHelper.routePoints[0].latitude, mapHelper.routePoints[0].longitude)
            val endMarkerLocation = GeoPoint(
                mapHelper.routePoints[mapHelper.routePoints.size - 1].latitude,
                mapHelper.routePoints[mapHelper.routePoints.size - 1].longitude
            )
            mapHelper.updateStartMarkerLocation(
                GeoPoint(
                    startMarkerLocation
                )
            )
            mapHelper.addEndMarker(endMarkerLocation)
            mapHelper.map.controller.animateTo(endMarkerLocation)
            mapHelper.firstLocation = false
        }
        mapHelper.addRouteToMap(routePoints)
    }

    private fun processRecordingState(intent: Intent) {
        val isServiceRecording = intent.getBooleanExtra("recording", true)
        if (!isServiceRecording) {
            pauseRecording()
            controlPanel.buttonSection.enterPauseMode()
        }else{
            if(!isRecording){
                startRecording()
                controlPanel.buttonSection.enterRecordingMode()
            }
        }

    }

    private fun processResetState(intent: Intent) {
        val doReset = intent.getBooleanExtra("reset", true)
        if (doReset) {
            resetSession()
        }
    }

    private fun processSessionDuration(intent: Intent) {
        val duration = intent.getStringExtra("time")
        if (duration != null) {
            sessionDurationInSeconds = duration.toInt()

            val formattedDuration = Utility.formatSessionTime(sessionDurationInSeconds)
            formattedSessionDuration = formattedDuration
            controlPanel.infoSection.updateSessionDuration(formattedDuration)
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

    fun startLocating() {
        mapHelper.startLocationUpdates()
        topBar.showGPSEnabled()
    }

    fun unregisterReceiver() {
        context.unregisterReceiver(recordingBroadcastReceiver)
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
}