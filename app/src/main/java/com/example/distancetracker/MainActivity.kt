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
import android.app.Dialog
import android.content.DialogInterface
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener

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

    private var geoPointList: ArrayList<GeoPoint> = ArrayList()

    private var showingRouteList: Boolean = false

    private var routeList: ArrayList<Route> = ArrayList()

    private var locationAverage:ArrayList<GeoPoint> = ArrayList()
    private var locationCounter:Int = 0

    private lateinit var listBtn: ImageButton

    private lateinit var routeListLayout: LinearLayout
    private lateinit var topBarLayout: LinearLayout

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

        FireStore.getRoutes(routeList, this)
        setFusedLocationClient()
        createLocationRequest()
        getCurrentLocation()
        mapHelper.centerOnPoint(currentLocation)
        startLocationsUpdates()
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
                mapHelper.updateStartMarkerLocation(currentLocation)
            }
        }
    }

    fun addRoutesToList() {
        Log.d("myTag", routeList.toString())
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

    private fun initializeButtons() {
        initializeButtonBar()
        initializeStartSessionBtn()
        initializeResetBtn()
        initializeListBtn()
        initializeCloseListBtn()
        initializeSaveBtn()
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
        routeListLayout = findViewById(R.id.routeListLayout)
        topBarLayout = findViewById(R.id.topBarLayout)

        listBtn.setOnClickListener {
            if (!showingRouteList) {
                routeListLayout.visibility = View.VISIBLE
                mapHelper.map.visibility = View.GONE
                showingRouteList = true

                topBarLayout.layoutParams =
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 8f)
                mapHelper.map.layoutParams =
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            } else {
                routeListLayout.visibility = View.GONE
                mapHelper.map.visibility = View.VISIBLE
                showingRouteList = false

                topBarLayout.layoutParams =
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
                mapHelper.map.layoutParams =
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 7f)
            }
        }
    }

    private fun initializeCloseListBtn() {
        val closeListBtn = findViewById<ImageButton>(R.id.closeListBtn)
        closeListBtn.setOnClickListener {
            routeListLayout.visibility = View.GONE
            mapHelper.map.visibility = View.VISIBLE
            showingRouteList = true

            topBarLayout.layoutParams =
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            mapHelper.map.layoutParams =
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 7f)
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
        geoPointList.add(currentLocation)
        mapHelper.updateStartMarkerLocation(currentLocation)
        mapHelper.addEndMarker(currentLocation)
        mapHelper.route.addPoint(currentLocation)

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
        resetTotalDistance()
        resetAverageSpeed()
        changeMainButtonDescription(R.string.start_session)
        changeMainButtonIcon(R.drawable.start_icon)
        mapHelper.removeRouteFromMap()
        mapHelper.removeEndMarker()
        hideButtonBar()
    }

    private fun resetTotalDistance() {
        totalDistance = 0.0
        totalDistanceTV.text = ContextCompat.getString(applicationContext, R.string._0_0km)
    }

    private fun resetAverageSpeed() {
        averageSpeed = 0.0
        averageSpeedTV.text = ContextCompat.getString(applicationContext, R.string._0_0_km_h)
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
            val newGeoPoint =
                GeoPoint(locations[0].latitude, locations[0].longitude)
            if (!startedSession) {
                updateCurrentLocation(newGeoPoint)
                mapHelper.updateStartMarkerLocation(currentLocation)
            } else {
                if (recording) {
                    val newLocation = Location("")
                    newLocation.latitude = mapHelper.endMarker.position.latitude
                    newLocation.longitude = mapHelper.endMarker.position.longitude
                    Log.d(
                        "myTag",
                        String.format("Distance walked ${locations[0].distanceTo(newLocation)} metres")
                    )

                    if (locations[0].distanceTo(newLocation) > 1) {
                        updateCurrentLocation(
                            newGeoPoint
                        )
                        updateTotalDistance(locations[0].distanceTo(newLocation))
                        mapHelper.updateEndMarkerLocation(
                            newGeoPoint
                        )
                        geoPointList.add(newGeoPoint)
                        Log.d("myTag", "Distance ACCEPTED by threshhold and updated")
                    } else {
                        Log.d("myTag", "Distance NOT ACCEPTED by threshhold")
                    }
                    updateAverageSpeed()
                }
            }
        }
    }

    private fun updateAverageSpeed() {
        averageSpeed =
            (totalDistance / (sessionTimer.sessionSeconds + (sessionTimer.sessionMinutes * 60) + (sessionTimer.sessionHours * 3600))) * 3.6
        averageSpeedTV.text = String.format("%.2f km/h", averageSpeed)
    }

    private fun updateCurrentLocation(newLocation: GeoPoint) {
        currentLocation = newLocation
    }

    private fun updateTotalDistance(distanceWalked: Float) {
        totalDistance += distanceWalked
        val totalDistanceMetres = totalDistance / 1000
        totalDistanceTV.text = String.format("%.2f km", totalDistanceMetres)
    }

    override fun onResume() {
        super.onResume()
        Log.d("myTag", "Resuming tracking")
        startLocationsUpdates()
    }

    override fun onPause() {
        super.onPause()
        if (!recording) {
            Log.d("myTag", "Stopped tracking")
            stopLocationUpdates()
        }
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
        FireStore.uploadRoute(
            Route(
                "route_name",
                String.format(
                    "${sessionTimer.sessionSeconds + sessionTimer.sessionMinutes * 60 + sessionTimer.sessionHours * 3600}"
                ),
                geoPointList,
                averageSpeed.toString(),
                totalDistance.toString()
            )
        )
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
        val dialog = Dialog(this)

        dialog.setContentView(R.layout.dialog_save_route)

        val totalDistanceDialog = dialog.findViewById<TextView>(R.id.totalDistance)
        val totalDistanceMetres = totalDistance / 1000
        totalDistanceDialog.text = String.format("%.2f km", totalDistanceMetres)

        val sessionTimeDialog = dialog.findViewById<TextView>(R.id.sessionTime)
        sessionTimeDialog.text = sessionTimer.getFormattedSessionDuration()

        val averageSpeedDialog = dialog.findViewById<TextView>(R.id.averageSpeed)
        averageSpeedDialog.text = String.format("%.2f km/h", averageSpeed)

        val saveBtn = dialog.findViewById<Button>(R.id.saveBtn)
        saveBtn.setOnClickListener {
            if (saveBtn.alpha == 1f) {
                saveSession()
                dialog.dismiss()
            } else {
                Toast.makeText(applicationContext, "Route name is not valid!", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        val routeNameEntry = dialog.findViewById<EditText>(R.id.routeNameEntry)
        routeNameEntry.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                isInputValid(saveBtn, p0.toString())
            }
        })

        val cancelBtn = dialog.findViewById<Button>(R.id.cancelBtn)
        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun isInputValid(saveBtn: Button, input: String) {
        //check if value of edittext on save dialog has 3 or more characters
        //changes save button alpha depending on the result
        if (input.length >= 3) {
            saveBtn.alpha = 1f
        } else {
            saveBtn.alpha = 0.3f
        }
    }

    private fun initializeTotalDistance() {
        totalDistanceTV = findViewById(R.id.totalDistance)
    }

    private fun initializeAverageSpeed() {
        averageSpeedTV = findViewById(R.id.averageSpeed)
    }

    private fun initializeTimer() {
        sessionTimer = CustomTimer(findViewById(R.id.sessionDuration), this)
    }

    private fun initializeSessionInformation() {
        initializeTotalDistance()
        initializeAverageSpeed()
    }

    private fun initializeButtonBar() {
        buttonBar = findViewById(R.id.buttonBar)
    }
}