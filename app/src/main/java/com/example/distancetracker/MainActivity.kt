package com.example.distancetracker

import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView

class MainActivity : AppCompatActivity() {

    private lateinit var totalDistanceTV: TextView
    private lateinit var sessionDurationTV: TextView
    private lateinit var averageSpeedTV: TextView

    private var totalDistance: Double = 0.0
    private var sessionDuration: Double = 0.0
    private var averageSpeed: Double = 0.0

    private lateinit var listBtn: ImageButton
    private lateinit var recordBtn: ImageButton
    private lateinit var resetBtn: ImageButton

    private lateinit var map: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeMap()

        initializeSessionInformation()
        initializeButtons()
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
        initializeRecordBtn()
        initializeResetBtn()
        initializeListBtn()
    }

    private fun initializeListBtn() {
        listBtn = findViewById(R.id.listBtn)
    }

    private fun initializeRecordBtn() {
        recordBtn = findViewById(R.id.recordBtn)
    }

    private fun initializeResetBtn() {
        resetBtn = findViewById(R.id.resetBtn)
    }
}