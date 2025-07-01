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
    lateinit var controlPanel: ControlPanel
    private lateinit var broadcastReceiver: BroadcastReceiver

    var recording: Boolean = false
    var startedSession: Boolean = false

    private var sessionDurationInSeconds: Int = 0
    var totalDistance: Double = 0.0
    var averageSpeed: Double = 0.0

    var geoPointList: ArrayList<GeoPoint> = ArrayList()

    var activeAutoPause: Boolean = true
    var activeAutoResume: Boolean = true

    var firstLocation: Boolean = true

    init {
        initializeTopBar()
        initializeMapHelper()
        initializeControlPanel()
        initializeBroadCastReceiver()

        if (Utility.isServiceRunningInForeground(context)) {
            //checks if fore ground service is running
            //if running -> bind to service
            //and create handler which takes data from service every second
            startedSession = true
            controlPanel.buttonSection.enterRecordingMode()
        } else {
            if (Utility.isGPSEnabled(context)) {
                if (Utility.isLocationPermissionGranted(mainActivity)) {
                    mapHelper.startLocationUpdates()
                } else {
                    Utility.requestLocationPermission(mainActivity)
                }
            } else {
                throw LocationClient.LocationException("GPS is disabled")
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
            distanceTrackerLayout.findViewById(R.id.map),
            this
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
        recording = false
    }

    fun pauseSession() {
        Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_RECORD
            context.startService(this)
        }

        stopRecording()
    }

    private fun stopSession() {
        startedSession = false
        stopRecording()
    }

    fun resumeSession() {
        startRecording()

        val intent = Intent(context, RecordingService::class.java)
        intent.action = RecordingService.ACTION_RECORD
        context.startService(intent)

        mapHelper.resetPauseCounter()
    }

    private fun startRecording() {
        recording = true
    }

    fun acceptLocation(distance: Float, newLocation: GeoPoint) {
        controlPanel.infoSection.updateTotalDistance(distance)

        mapHelper.updateEndMarkerLocation(newLocation)
        mapHelper.resetPauseCounter()

        geoPointList.add(newLocation)
    }

    fun rejectLocation() {
        mapHelper.updatePauseCounter()
    }

    fun startSession() {
        mapHelper.locationScope.cancel()
        startRecordingService()
        startedSession = true
        startRecording()
        mapHelper.showMap()
        mapHelper.addEndMarker(mapHelper.currentLocation)
        mapHelper.centerOnPoint(mapHelper.currentLocation)
        controlPanel.buttonSection.enterRecordingMode()
    }

    fun resetSession() {
        stopSession()

        val intent = Intent(context, RecordingService::class.java).apply {
            action = "ACTION_RESET"
        }
        context.startService(intent)

        mapHelper.removeRouteFromMap()
        mapHelper.removeEndMarker()
        mapHelper.resetPauseCounter()
        mapHelper.map.invalidate()

        mapHelper.startLocationUpdates()

        controlPanel.reset()

        geoPointList.clear()
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

        geoPointList = routePoints

        if (firstLocation) {
            val startMarkerLocation = GeoPoint(geoPointList[0].latitude, geoPointList[0].longitude)
            val endMarkerLocation = GeoPoint(
                geoPointList[geoPointList.size - 1].latitude,
                geoPointList[geoPointList.size - 1].longitude
            )
            mapHelper.updateStartMarkerLocation(
                GeoPoint(
                    startMarkerLocation
                )
            )
            mapHelper.addEndMarker(endMarkerLocation)
            mapHelper.map.controller.animateTo(startMarkerLocation)
            firstLocation != firstLocation
        }
        mapHelper.addRouteToMap(routePoints)
    }

    fun unregisterReceiver() {
        context.unregisterReceiver(broadcastReceiver)
    }

    private fun processSessionDuration(intent: Intent) {
        val sessionDuration = intent.getStringExtra("time")
        if (sessionDuration != null) {
            sessionDurationInSeconds = sessionDuration.toInt()
            val formattedSessionDuration = Utility.formatSessionTime(sessionDurationInSeconds)
            controlPanel.infoSection.updateSessionDuration(formattedSessionDuration)
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
        }
    }

    private fun processAverageSpeed(intent: Intent) {
        val averageSpeed = intent.getStringExtra("averageSpeed")
        if (averageSpeed != null) {
            controlPanel.infoSection.updateAverageSpeed(averageSpeed)
        }
    }

    private fun processTotalDistance(intent: Intent) {
        val totalDistance = intent.getStringExtra("totalDistance")?.toDouble()
        if (totalDistance != null) {
            controlPanel.infoSection.updateTotalDistance(totalDistance.toFloat())
        }
    }
}