package com.example.distancetracker


import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.location.Location
import android.os.IBinder
import android.util.Log
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.example.distancetracker.controlpanel.ControlPanel
import com.example.distancetracker.topbar.TopBar
import com.google.android.gms.location.LocationCallback
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class DistanceTracker(
    private val distanceTrackerLayout: LinearLayout,
    private val context: Context,
    private val mainActivity: MainActivity,
    private val locationCallback: LocationCallback
){
    lateinit var topBar: TopBar
    lateinit var mapHelper: MapHelper
    lateinit var controlPanel: ControlPanel

    var recording: Boolean = false
    var startedSession: Boolean = false

    var totalDistance: Double = 0.0
    var averageSpeed: Double = 0.0

    var geoPointList: ArrayList<GeoPoint> = ArrayList()

    lateinit var sessionTimer: CustomTimer

    var locatedFirstTime: Boolean = false

    var activeAutoPause: Boolean = true
    var activeAutoResume: Boolean = true

    private lateinit var mService: ForeGroundService
    private var mBound = false
    private val mConnection = object:ServiceConnection{
        override fun onServiceConnected(className: ComponentName?, binder: IBinder?) {
            val service = binder as ForeGroundService.MyBinder
            mService = service.getService()
            mBound = true
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            mBound = false
        }
    }

    init {
        initializeTopBar()
        initializeMapHelper()
        initializeTimer()
        initializeControlPanel()
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

    private fun stopSession() {
        sessionTimer.stopTimer()
        startedSession = false
        stopRecording()
    }

    fun startSession() {
        //notification starting TODO location using in background and  timer
        val intent = Intent(context,ForeGroundService::class.java)
        context.startForegroundService(intent)

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

    fun acceptLocation(distance: Float, newLocation: GeoPoint) {
        controlPanel.infoSection.updateTotalDistance(distance)

        mapHelper.updateEndMarkerLocation(newLocation)
        mapHelper.resetPauseCounter()

        geoPointList.add(newLocation)
    }

    fun rejectLocation() {
        mapHelper.updatePauseCounter()
    }

    fun resetSession() {
        Log.d("myTag", isRecording().toString())
        Log.d("myTag", hasStartedSession().toString())
        stopSession()

        mapHelper.removeRouteFromMap()
        mapHelper.removeEndMarker()
        mapHelper.resetPauseCounter()
        mapHelper.map.invalidate()

        sessionTimer.resetSessionTimes()
        sessionTimer.setSessionDurationDisplay()
        controlPanel.infoSection.resetTotalDistance()
        controlPanel.infoSection.resetAverageSpeed()
        controlPanel.buttonSection.changeSessionButtonDescription(R.string.start_session)
        controlPanel.buttonSection.changeSessionButtonIcon(R.drawable.start_icon)

        geoPointList.clear()
        controlPanel.buttonSection.buttonSubBar.hideButtonBar()
    }
}