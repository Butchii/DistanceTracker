package com.example.distancetracker

import android.Manifest
import android.app.Activity
import android.content.Context
import android.location.Location
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class MapHelper(
    private val context: Context,
    private val activity: Activity,
    val map: MapView,
    private val locationCallback: LocationCallback,
    private val distanceTracker: DistanceTracker
) {
    var route: Polyline = Polyline()

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var startMarker: Marker = Marker(map)
    private var endMarker: Marker = Marker(map)

    var endMarkerLocation: Location = Location("")

    private var pauseSessionCounter: Int = 0

    private var resumeSessionCounter: Int = 0

    private var lowerDistanceThreshold: Double = 0.55  //  2  km/h

    var currentLocation: GeoPoint = GeoPoint(0.0, 0.0)

    private lateinit var locationRequest: LocationRequest

    init {
        setFusedLocationClient()
        createLocationRequest()
        getCurrentLocation()
        Configuration.getInstance()
            .load(context, PreferenceManager.getDefaultSharedPreferences(context))
        setupMap(map)
        configureMarkers(startMarker, endMarker, context)
        map.overlays.add(route)
        map.overlays.add(startMarker)
        centerOnPoint(currentLocation)
        startLocationsUpdates()
    }

    fun centerOnPoint(location: GeoPoint) {
        map.controller.animateTo(location)
    }

    fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            context.getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    fun updateStartMarkerLocation(location: GeoPoint) {
        Log.d(
            "myTag",
            String.format("Updated STARTMARKER position from ${startMarker.position} to $location")
        )
        startMarker.position = location
        map.invalidate()
    }

    fun updateCurrentLocation(geoPoint: GeoPoint){
        currentLocation = geoPoint
    }

    fun updateEndMarkerLocation(location: GeoPoint) {
        Log.d(
            "myTag",
            "Distance ACCEPTED by thresh hold and updated EndMarker Position"
        )
        endMarker.position = location

        endMarkerLocation.latitude = location.latitude
        endMarkerLocation.longitude = location.longitude

        route.addPoint(location)
        map.invalidate()
    }

    fun removeRouteFromMap() {
        map.overlays.remove(route)
        route = Polyline()
        map.overlays.add(route)
    }

    fun removeEndMarker() {
        map.overlays.remove(endMarker)
    }

    fun addEndMarker(location: GeoPoint) {
        endMarker.position = location

        endMarkerLocation.latitude = location.latitude
        endMarkerLocation.longitude = location.longitude
        map.overlays.add(endMarker)
    }

    private fun setFusedLocationClient() {
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)
    }

    private fun getCurrentLocation() {
        if (isLocationEnabled()) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    context,
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
                updateStartMarkerLocation(currentLocation)
            }
        }
    }

    private fun requestPermissions() {
        val LOCATION_REQUEST = 44
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            LOCATION_REQUEST
        )
    }

    fun startLocationsUpdates() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest, locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
    }

    fun hideMap() {
        map.visibility = View.GONE
    }

    fun showMap() {
        map.visibility = View.VISIBLE
    }

    private fun incrementPauseCounter() {
        Log.d("myTag", "Distance too low, updated Pause Counter")
        pauseSessionCounter++
        Log.d("myTag", String.format("Pause counter is $pauseSessionCounter"))
    }

    fun resetPauseCounter() {
        pauseSessionCounter = 0
    }

    private fun checkPauseCounter() {
        if (pauseSessionCounter == 5 && distanceTracker.activeAutoPause) {
            Toast.makeText(context, "Session paused because of idling!", Toast.LENGTH_SHORT).show()
            Log.d("myTag", "Paused session because of idling")
            distanceTracker.pauseSession()
            distanceTracker.controlPanel.buttonSection.enterPauseMode()
            resetPauseCounter()
        }
    }

    private fun incrementResumeCounter() {
        resumeSessionCounter++
    }

    private fun checkResumeCounter() {
        if (resumeSessionCounter > 5) {
            Toast.makeText(context, "Session resumed", Toast.LENGTH_SHORT).show()
            distanceTracker.resumeSession()
            distanceTracker.controlPanel.buttonSection.enterRecordingMode()
        }
    }

    fun isDistanceValid(distance: Float): Boolean {
        return distance > lowerDistanceThreshold
    }

    fun updatePauseCounter() {
        incrementPauseCounter()
        checkPauseCounter()
    }

    fun updateResumeCounter() {
        incrementResumeCounter()
        checkResumeCounter()
    }

    companion object {
        fun setupMap(map: MapView) {
            map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
            map.setMultiTouchControls(true)
            map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            map.controller.setZoom(18)
        }

        fun configureMarkers(startMarker: Marker, endMarker: Marker, context: Context) {
            startMarker.setAnchor(0.25f, 0.35f)
            startMarker.icon = ContextCompat.getDrawable(context, R.drawable.start_marker_map_icon)

            endMarker.setAnchor(0.25f, 0.35f)
            endMarker.icon = ContextCompat.getDrawable(context, R.drawable.end_marker_map_icon)
        }
    }
}