package com.example.distancetracker

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.osmdroid.util.GeoPoint
import android.Manifest
import android.content.DialogInterface
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

open class MainActivity : AppCompatActivity() {

    private lateinit var totalDistanceTV: TextView
    private lateinit var averageSpeedTV: TextView

    private var totalDistance: Double = 0.0
    private var averageSpeed: Double = 0.0

    private lateinit var locationRequest: LocationRequest
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var currentLocation: GeoPoint = GeoPoint(0.0, 0.0)
    private lateinit var lastLocation: GeoPoint

    private var geoPointList: ArrayList<GeoPoint> = ArrayList()

    private lateinit var listBtn: ImageButton

    private lateinit var sessionBtn: ImageButton
    private lateinit var sessionBtnDescription: TextView

    private lateinit var buttonBar: LinearLayout
    private lateinit var resetSessionBtn: ImageButton
    private lateinit var saveSessionBtn: ImageButton

    private lateinit var mapHelper: MapHelper

    private lateinit var sessionTimer: CustomTimer

    private var recording: Boolean = false
    private var startedSession: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeMapHelper()
        initializeTimer()

        initializeSessionInformation()
        initializeButtons()

        setFusedLocationClient()
        createLocationRequest()
        getCurrentLocation()
        startLocationsUpdates()
    }

    private fun initializeTimer() {
        sessionTimer = CustomTimer(findViewById(R.id.sessionDuration), this)
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
                mapHelper.addMarker(currentLocation)
            }
        }
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

    private fun initializeMapHelper() {
        mapHelper = MapHelper(applicationContext, findViewById(R.id.map))
    }

    private fun initializeSessionInformation() {
        initializeTotalDistance()
        initializeAverageSpeed()
    }

    private fun initializeTotalDistance() {
        totalDistanceTV = findViewById(R.id.totalDistance)
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
            if (!recording) {
                showSaveDialog()
            } else {
                Toast.makeText(
                    applicationContext,
                    "Please pause the recording before saving!",
                    Toast.LENGTH_SHORT
                ).show()
            }

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
                    //no session started yet
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
        startRecording()
        sessionTimer.createTimer()
    }

    private fun activateSaveBtn() {
        saveSessionBtn.alpha = 1f
    }

    private fun deactivateSaveBtn() {
        saveSessionBtn.alpha = 0.2f
    }

    private fun startSession() {
        lastLocation = currentLocation
        geoPointList.add(lastLocation)
        //create start marker on map TODO

        startedSession = true
        startRecording()
        showButtonBar()
        sessionTimer.createTimer()
    }


    private fun startRecording() {
        recording = true
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
        sessionTimer.resetSessionTimes()
        sessionTimer.setSessionDurationDisplay()
        changeMainButtonDescription(R.string.start_session)
        changeMainButtonIcon(R.drawable.start_icon)
        hideButtonBar()
    }

    private fun pauseSession() {
        sessionTimer.stopTimer()
        stopRecording()
    }

    private fun stopRecording() {
        recording = false
    }

    private fun stopSession() {
        sessionTimer.stopTimer()
        startedSession = false
        stopRecording()
    }

    private fun changeMainButtonIcon(iconId: Int) {
        sessionBtn.setImageResource(iconId)
    }

    private fun changeMainButtonDescription(stringId: Int) {
        sessionBtnDescription.text =
            ContextCompat.getString(applicationContext, stringId)
    }

    private fun showResetButton() {
        resetSessionBtn.visibility = View.VISIBLE
    }

    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            super.onLocationResult(p0)
            val locations = p0.locations

            if (!startedSession) {
                currentLocation = GeoPoint(locations[0].latitude, locations[0].longitude)
                mapHelper.updateCurrentLocationMarker(currentLocation)
            } else {
                val newLocation = Location("")
                newLocation.latitude = currentLocation.latitude
                newLocation.longitude = currentLocation.longitude
                if (locations[0].distanceTo(newLocation) > 1.0 && locations[0].distanceTo(
                        newLocation
                    ) < 6 && recording
                ) {
                    Log.d(
                        "myTag",
                        locations[0].distanceTo(newLocation).toString()
                    )
                    updateCurrentLocation(GeoPoint(locations[0].latitude, locations[0].longitude))
                    mapHelper.addMarker(currentLocation)
                    updateTotalDistance(locations[0].distanceTo(newLocation))
                }
            }


        }
    }

    private fun updateCurrentLocation(newLocation: GeoPoint) {
        currentLocation = newLocation
    }

    private fun updateTotalDistance(distanceWalked: Float) {
        totalDistance += distanceWalked
        val totalDistanceMetres = totalDistance / 1000
        totalDistanceTV.text = String.format("%.2f  km", totalDistanceMetres)
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