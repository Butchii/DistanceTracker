package com.example.distancetracker

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View.OnClickListener
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import java.util.Timer
import java.util.TimerTask

class MainActivity : AppCompatActivity() {

    private lateinit var totalDistanceTV: TextView
    private lateinit var sessionDurationTV: TextView
    private lateinit var averageSpeedTV: TextView

    private lateinit var sessionTimer: Timer
    private var sessionTime: Int = 0
    private var totalDistance: Double = 0.0
    private var averageSpeed: Double = 0.0

    private lateinit var listBtn: ImageButton
    private lateinit var startSessionBtn: ImageButton
    private lateinit var resetSessionBtn: ImageButton

    private lateinit var map: MapView

    private var recording: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeMap()

        initializeSessionInformation()
        initializeSessionTimer()
        initializeButtons()
    }

    private fun initializeSessionTimer() {
        sessionTimer = Timer()
    }

    private fun initializeMap() {
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        map = findViewById(R.id.map)

        map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        map.setMultiTouchControls(true)
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        map.controller.setZoom(15)
        map.controller.animateTo(GeoPoint(50.5532715, 7.1045565))
    }

    private fun initializeSessionInformation() {
        initializeTotalDistance()
        initializeRecordingTime()
        initializeAverageSpeed()
    }

    private fun initializeTotalDistance() {
        totalDistanceTV = findViewById(R.id.totalDistance)
    }

    private fun initializeRecordingTime() {
        sessionDurationTV = findViewById(R.id.sessionDuration)
    }

    private fun initializeAverageSpeed() {
        averageSpeedTV = findViewById(R.id.averageSpeed)
    }

    private fun initializeButtons() {
        initializeStartSessionBtn()
        initializeResetBtn()
        initializeListBtn()
    }

    private fun initializeListBtn() {
        listBtn = findViewById(R.id.listBtn)
    }

    private fun initializeStartSessionBtn() {
        startSessionBtn = findViewById(R.id.recordBtn)
        startSessionBtn.setOnClickListener {
            startSession()
            recording = true
        }
    }

    private fun startSession() {
        val timerTask = createTimerTask()
        sessionTimer.schedule(timerTask, 0, 1000)
    }

    private fun initializeResetBtn() {
        resetSessionBtn = findViewById(R.id.resetBtn)
    }

    private fun createTimerTask() = object : TimerTask() {
        override fun run() {
            sessionTime++
            runOnUiThread { sessionDurationTV.text = sessionTime.toString() }
        }
    }
}