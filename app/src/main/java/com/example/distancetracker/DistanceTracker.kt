package com.example.distancetracker

import android.content.Context
import android.widget.LinearLayout
import com.example.distancetracker.controlpanel.ControlPanel
import com.example.distancetracker.topbar.TopBar
import com.google.android.gms.location.LocationCallback
import org.osmdroid.util.GeoPoint

class DistanceTracker(
    private val distanceTrackerLayout: LinearLayout,
    private val context: Context,
    private val mainActivity: MainActivity,
    private val locationCallback: LocationCallback
) {
    private lateinit var topBar: TopBar
    lateinit var mapHelper: MapHelper
    lateinit var controlPanel: ControlPanel

    var recording: Boolean = false
    var startedSession: Boolean = false

    var totalDistance: Double = 0.0
    var averageSpeed: Double = 0.0

    var geoPointList: ArrayList<GeoPoint> = ArrayList()

    lateinit var sessionTimer: CustomTimer

    init {
        initializeTopBar()
        initializeMapHelper()
        initializeTimer()
        initializeControlPanel()
    }

    private fun initializeTopBar() {
        topBar = TopBar(context, distanceTrackerLayout.findViewById(R.id.topBarLayout), this)
    }

    private fun initializeMapHelper() {
        mapHelper = MapHelper(
            context,
            mainActivity,
            distanceTrackerLayout.findViewById(R.id.map),
            locationCallback,
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
        sessionTimer.stopTimer()
        stopRecording()
    }

    private fun initializeTimer() {
        sessionTimer =
            CustomTimer(distanceTrackerLayout.findViewById(R.id.sessionDuration), mainActivity)
    }

    fun stopSession() {
        sessionTimer.stopTimer()
        startedSession = false
        stopRecording()
    }

    fun startSession() {
        geoPointList.add(mapHelper.currentLocation)
        mapHelper.updateStartMarkerLocation(mapHelper.currentLocation)
        mapHelper.addEndMarker(mapHelper.currentLocation)
        mapHelper.route.addPoint(mapHelper.currentLocation)

        startedSession = true
        startRecording()
        controlPanel.buttonSection.buttonSubBar.showButtonBar()
        sessionTimer.createTimer()
    }

    fun resumeSession() {
        startRecording()
        mapHelper.resetPauseCounter()
        sessionTimer.createTimer()
    }

    private fun startRecording() {
        recording = true
    }

    fun isRecording(): Boolean {
        return recording
    }

    fun hasStartedSession(): Boolean {
        return startedSession
    }
}