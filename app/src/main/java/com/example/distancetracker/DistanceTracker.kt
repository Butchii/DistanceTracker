package com.example.distancetracker

import android.content.Context
import android.util.Log
import android.widget.LinearLayout
import com.example.distancetracker.controlpanel.ControlPanel
import com.example.distancetracker.topbar.TopBar
import org.osmdroid.util.GeoPoint

class DistanceTracker(
    private val distanceTrackerLayout: LinearLayout,
    private val context: Context,
    private val mainActivity: MainActivity
) {
    private lateinit var topBar: TopBar
    lateinit var mapHelper: MapHelper
    private lateinit var controlPanel: ControlPanel

    var recording: Boolean = false
    var startedSession: Boolean = false

    var totalDistance: Double = 0.0
    var averageSpeed: Double = 0.0

    var geoPointList: ArrayList<GeoPoint> = ArrayList()

    private var routeList: ArrayList<Route> = ArrayList()

    lateinit var sessionTimer: CustomTimer

    init {
        initializeTopBar()
        initializeMap()
        initializeTimer()
        initializeControlPanel()
        FireStore.getRoutes(routeList, mainActivity)
    }

    private fun initializeTopBar() {
        topBar = TopBar(distanceTrackerLayout.findViewById(R.id.distanceTrackerLayout))
    }

    private fun initializeMap() {
        mapHelper = MapHelper(context, distanceTrackerLayout.findViewById(R.id.map))
    }

    private fun initializeControlPanel() {
        controlPanel =
            ControlPanel(distanceTrackerLayout.findViewById(R.id.controlPanelLayout), this)
    }

    private fun stopRecording() {
        recording = false
    }

    private fun pauseSession() {
        sessionTimer.stopTimer()
        stopRecording()
    }

    private fun initializeTimer() {
        sessionTimer =
            CustomTimer(distanceTrackerLayout.findViewById(R.id.sessionDuration), mainActivity)
    }
}