package com.example.distancetracker

import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import android.Manifest
import android.content.DialogInterface
import android.location.LocationManager
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.views.overlay.Marker

import java.util.Timer
import java.util.TimerTask

open class MainActivity : AppCompatActivity() {

    private lateinit var totalDistanceTV: TextView
    private lateinit var sessionDurationTV: TextView
    private lateinit var averageSpeedTV: TextView

    private lateinit var sessionTimer: Timer

    private var sessionHours: Int = 0
    private var sessionMinutes: Int = 0
    private var sessionSeconds: Int = 0

    private var totalDistance: Double = 0.0
    private var averageSpeed: Double = 0.0

    private lateinit var locationRequest: LocationRequest
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var currentLocation: GeoPoint = GeoPoint(0.0, 0.0)

    private lateinit var listBtn: ImageButton

    private lateinit var startSessionBtn: ImageButton
    private lateinit var startSessionBtnDescription: TextView

    private lateinit var resetSessionBtn: ImageButton

    private lateinit var map: MapView

    private var recording: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeMap()

        initializeSessionInformation()
        initializeButtons()

        setFusedLocationClient()
        createLocationRequest()
        getCurrentLocation()
    }

    private fun getCurrentLocation() {
        if (isLocationEnabled()) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions()
                return
            }
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation.latitude = location.latitude
                    currentLocation.longitude = location.longitude
                }
                addMarker()
            }
        }
    }

    private fun addMarker() {
        val marker = Marker(map)
        marker.position = currentLocation
        map.overlays.add(marker)
    }

    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            applicationContext.getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun requestPermissions() {
        val LOCATION_REQUEST = 44
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            LOCATION_REQUEST
        )
    }

    private fun setFusedLocationClient() {
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(applicationContext)
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000).build()
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
        startSessionBtnDescription = findViewById(R.id.recordBtnDescription)

        startSessionBtn.setOnClickListener {
            if (isLocationEnabled()) {
                if (!recording) {
                    startSession()
                    startSessionBtnDescription.text =
                        ContextCompat.getString(applicationContext, R.string.recording)
                    startSessionBtn.setImageResource(R.drawable.record_icon)
                    recording = true
                    showResetButton()
                }
            } else {
                Toast.makeText(applicationContext, "Please enable your GPS!", Toast.LENGTH_SHORT)
                    .show()
            }

        }
    }

    private fun startSession() {
        val timerTask = createTimerTask()
        sessionTimer = Timer()
        sessionTimer.schedule(timerTask, 0, 1000)
    }

    private fun initializeResetBtn() {
        resetSessionBtn = findViewById(R.id.resetBtn)
        resetSessionBtn.setOnClickListener {
            showResetDialog()
        }
    }

    private fun resetSession() {
        sessionTimer.cancel()
        recording = false
        sessionSeconds = 0
        sessionMinutes = 0
        sessionHours = 0
        sessionDurationTV.text =
            String.format("$sessionHours h $sessionMinutes m $sessionSeconds s")

        startSessionBtnDescription.text =
            ContextCompat.getString(applicationContext, R.string.start_session)
        startSessionBtn.setImageResource(R.drawable.start_icon)
        hideResetButton()
    }

    private fun showResetButton() {
        resetSessionBtn.visibility = View.VISIBLE
    }

    private fun hideResetButton() {
        resetSessionBtn.visibility = View.GONE
    }

    private fun createTimerTask() = object : TimerTask() {
        override fun run() {
            sessionSeconds++
            if (sessionSeconds > 0 && sessionSeconds % 60 == 0) {
                sessionSeconds = 0
                sessionMinutes++
            }
            if (sessionMinutes > 0 && sessionMinutes % 60 == 0) {
                sessionMinutes = 0
                sessionHours++
            }
            runOnUiThread {
                sessionDurationTV.text =
                    String.format("$sessionHours h $sessionMinutes m $sessionSeconds s")
            }
        }
    }

    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            super.onLocationResult(p0)
            val locations = p0.locations
            currentLocation = GeoPoint(locations[0].latitude, locations[0].longitude)
        }
    }

    override fun onResume() {
        super.onResume()
        if (checkPermissions()) {
            startLocationsUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun startLocationsUpdates() {
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest, locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    private fun showResetDialog() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.apply {
            setTitle("Reset session")
            setMessage("Are you sure you want to reset the current session?")
            setPositiveButton("Yes") { _: DialogInterface?, _: Int ->
                resetSession()
            }
            setNegativeButton("Cancel") { _, _ -> }
        }.create().show()
    }
}