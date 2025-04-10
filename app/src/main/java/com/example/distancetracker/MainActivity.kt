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
import android.widget.LinearLayout
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

    private lateinit var sessionBtn: ImageButton
    private lateinit var sessionBtnDescription: TextView

    private lateinit var buttonBar: LinearLayout
    private lateinit var resetSessionBtn: ImageButton
    private lateinit var saveSessionBtn: ImageButton

    private lateinit var map: MapView

    private var recording: Boolean = false
    private var startedSession: Boolean = false

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
        initializeButtonBar()
        initializeStartSessionBtn()
        initializeResetBtn()
        initializeListBtn()
        initializeSaveBtn()
    }

    private fun initializeButtonBar() {
        buttonBar = findViewById(R.id.buttonBar)
    }

    private fun initializeSaveBtn() {
        saveSessionBtn = findViewById(R.id.saveBtn)

        saveSessionBtn.setOnClickListener {
            showSaveDialog()
        }
    }

    private fun initializeListBtn() {
        listBtn = findViewById(R.id.listBtn)

        listBtn.setOnClickListener {
            //TODO
        }
    }

    private fun initializeStartSessionBtn() {
        sessionBtn = findViewById(R.id.recordBtn)
        sessionBtnDescription = findViewById(R.id.recordBtnDescription)

        sessionBtn.setOnClickListener {
            if (isLocationEnabled()) {
                if (!startedSession) {
                    //now session started yet
                    startSession()
                    changeMainButtonDescription(R.string.recording)
                    changeMainButtonIcon(R.drawable.record_icon)
                    showResetButton()
                } else {
                    //session already exists
                    if (recording) {
                        pauseSession()
                        changeMainButtonDescription(R.string.paused)
                        changeMainButtonIcon(R.drawable.pause_icon)
                        activateSaveBtn()
                    } else {
                        resumeSession()
                        changeMainButtonDescription(R.string.recording)
                        changeMainButtonIcon(R.drawable.record_icon)
                        deactivateSaveBtn()
                    }
                }
            } else {
                Toast.makeText(applicationContext, "Please enable your GPS!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun resumeSession() {
        recording = true
        restartTimer()
    }

    private fun restartTimer() {
        val timerTask = createTimerTask()
        sessionTimer = Timer()
        sessionTimer.schedule(timerTask, 0, 1000)
    }

    private fun activateSaveBtn() {
        saveSessionBtn.alpha = 1f
    }

    private fun deactivateSaveBtn() {
        saveSessionBtn.alpha = 0.2f
    }

    private fun startSession() {
        startedSession = true
        recording = true
        showButtonBar()
        val timerTask = createTimerTask()
        sessionTimer = Timer()
        sessionTimer.schedule(timerTask, 0, 1000)
    }

    private fun showButtonBar() {
        buttonBar.visibility = View.VISIBLE
    }

    private fun hideButtonBar() {
        buttonBar.visibility = View.GONE
    }

    private fun initializeResetBtn() {
        resetSessionBtn = findViewById(R.id.resetBtn)
        resetSessionBtn.setOnClickListener {
            showResetDialog()
        }
    }

    private fun resetSession() {
        stopSession()
        resetSessionTimes()
        setSessionDurationDisplay()
        changeMainButtonDescription(R.string.start_session)
        changeMainButtonIcon(R.drawable.start_icon)
        hideButtonBar()
    }

    private fun setSessionDurationDisplay() {
        //set the textview, which displays session duration, to hours,minutes,
        // seconds saved in sessionHours, sessionMinutes and sessionSeconds
        sessionDurationTV.text =
            String.format("$sessionHours h $sessionMinutes m $sessionSeconds s")
    }

    private fun pauseSession() {
        stopTimer()
        recording = false
    }

    private fun stopSession() {
        stopTimer()
        startedSession = false
        recording = false
    }

    private fun changeMainButtonIcon(iconId: Int) {
        sessionBtn.setImageResource(iconId)
    }

    private fun changeMainButtonDescription(stringId: Int) {
        sessionBtnDescription.text =
            ContextCompat.getString(applicationContext, stringId)
    }

    private fun resetSessionTimes() {
        resetSessionSeconds()
        resetSessionMinutes()
        resetSessionHours()
    }

    private fun resetSessionHours() {
        sessionHours = 0
    }

    private fun resetSessionMinutes() {
        sessionMinutes = 0
    }

    private fun resetSessionSeconds() {
        sessionSeconds = 0
    }

    private fun stopTimer() {
        sessionTimer.cancel()
    }

    private fun showResetButton() {
        resetSessionBtn.visibility = View.VISIBLE
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
                setSessionDurationDisplay()
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

    private fun saveSession() {
        Toast.makeText(applicationContext, "Session saved", Toast.LENGTH_SHORT).show()
        //save on firestore TODO
        resetSession()
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

    private fun showSaveDialog() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.apply {
            setTitle("Save session")
            setMessage("Save the current session and reset?")
            setPositiveButton("Yes") { _: DialogInterface?, _: Int ->
                saveSession()
            }
            setNegativeButton("Cancel") { _, _ -> }
        }.create().show()
    }
}