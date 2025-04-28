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
import java.util.Collections

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

    private var locationList: HashMap<Float, GeoPoint> = HashMap()
    private var locationCounter: Int = 0

    private var pauseSessionCounter: Int = 0

    private var prePauseLocation: GeoPoint = GeoPoint(0.0, 0.0)
    private var prePauseAverageSpeed: Double = 0.0

    private var prePauseSessionSeconds: Int = 0
    private var prePauseSessionMinutes: Int = 0
    private var prePauseSessionHours: Int = 0

    private var prePauseTotalDistance: Double = 0.0
    private var prePauseRoute: Polyline = Polyline()

    private var resumeSessionCounter: Int = 0

    private var lowerDistanceThreshold: Double = 0.28  //  2  km/h
    private var upperDistanceThreshold: Double = 1.04  // 7,5 km/h

    var currentLocation: GeoPoint = GeoPoint(0.0, 0.0)

    private lateinit var locationRequest: LocationRequest

    init {
        setFusedLocationClient()
        createLocationRequest()
        getCurrentLocation()
        Configuration.getInstance()
            .load(context, PreferenceManager.getDefaultSharedPreferences(context))
        setupMap()
        configureMarkers()
        map.overlays.add(route)
        centerOnPoint()
        startLocationsUpdates()
    }

    private fun configureMarkers() {
        startMarker.setAnchor(0.25f, 0.35f)
        startMarker.icon = ContextCompat.getDrawable(context, R.drawable.start_marker_map_icon)
        map.overlays.add(startMarker)

        endMarker.setAnchor(0.25f, 0.35f)
        endMarker.icon = ContextCompat.getDrawable(context, R.drawable.end_marker_map_icon)
    }

    private fun setupMap() {
        map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        map.setMultiTouchControls(true)
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        map.controller.setZoom(15)
    }

    private fun centerOnPoint() {
        map.controller.animateTo(currentLocation)
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
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500).build()
    }

    fun hideMap() {
        map.visibility = View.GONE
    }

    fun showMap() {
        map.visibility = View.VISIBLE
    }

    private fun increaseLocationCounter() {
        locationCounter++
    }

    fun resetLocationCounter() {
        locationCounter = 0
    }

    private fun checkLocationCounter() {
        if (locationCounter == 3) {
            val minDistanceGeoPoint = locationList[Collections.min(locationList.keys)]

            val minDistanceLocation = Location("")
            if (minDistanceGeoPoint != null) {
                minDistanceLocation.latitude = minDistanceGeoPoint.latitude
                minDistanceLocation.longitude = minDistanceGeoPoint.longitude
            }

            val additionalDistance = minDistanceLocation.distanceTo(endMarkerLocation)
            distanceTracker.averageSpeed += additionalDistance / locationCounter

            if (minDistanceGeoPoint != null) {
                updateEndMarkerLocation(minDistanceGeoPoint)
            }

            resetLocationCounter()
            clearLocationList()

            Log.d(
                "myTag",
                "Location counter hit 3 and End marker position has been updated to saved minimum distance position"
            )
        }
    }

    fun clearLocationList() {
        locationList.clear()
    }

    private fun saveLocationForOptimization(location: GeoPoint, distance: Float) {
        locationList[distance] = location
        Log.d("myTag", String.format("Location counter is $locationCounter"))
        Log.d("myTag", String.format("Location to save is : $location"))
        Log.d("myTag", String.format("Distance is : $distance"))
        Log.d("myTag", String.format("Location list is: $locationList"))
    }

    private fun incrementPauseCounter() {
        Log.d("myTag", "Distance too low, updated Pause Counter")
        pauseSessionCounter++
        Log.d("myTag", String.format("Pause counter is $pauseSessionCounter"))
    }

    fun resetPauseCounter() {
        pauseSessionCounter = 0
        prePauseRoute = Polyline()
    }

    private fun checkPauseCounter() {
        if (pauseSessionCounter == 10) {
            Toast.makeText(context, "Session paused because of idling!", Toast.LENGTH_SHORT).show()
            Log.d("myTag", "Paused session because of idling")
            distanceTracker.pauseSession()
            distanceTracker.controlPanel.buttonSection.enterPauseMode()
            resetRoute()
            resetPauseCounter()
            resetSessionInformation()
            updateEndMarkerLocation(prePauseLocation)
            Log.d("myTag", "Changed End marker position to pre pause location")
        }
    }

    private fun resetSessionInformation() {
        resetSessionTotalDistance()
        resetSessionAverageSpeed()
        resetSessionTime()
    }

    private fun resetSessionAverageSpeed() {
        distanceTracker.averageSpeed = prePauseAverageSpeed
        distanceTracker.controlPanel.infoSection.setAverageSpeed()
    }

    private fun resetSessionTotalDistance() {
        distanceTracker.totalDistance = prePauseTotalDistance
        distanceTracker.controlPanel.infoSection.setTotalDistance()
    }

    private fun resetSessionTime() {
        distanceTracker.sessionTimer.sessionSeconds = prePauseSessionSeconds
        distanceTracker.sessionTimer.sessionMinutes = prePauseSessionMinutes
        distanceTracker.sessionTimer.sessionHours = prePauseSessionHours
        distanceTracker.sessionTimer.setSessionDurationDisplay()
    }

    private fun setPauseLocation(geoPoint: GeoPoint) {
        //save current position for a potential pause activation
        prePauseLocation = GeoPoint(geoPoint.latitude, geoPoint.longitude)
    }

    private fun savePauseRoute() {
        //save current route for a potential pause activation
        prePauseRoute = Polyline()
        for (geoPoint in route.actualPoints) {
            prePauseRoute.addPoint(geoPoint)
        }
        Log.d("myTag", String.format("saved route is: ${prePauseRoute.actualPoints} \n"))
    }

    private fun resetRoute() {
        //change current route to saved pre pause route
        map.overlays.remove(route)
        route = Polyline()
        if (prePauseRoute.actualPoints.size > 1) {
            for (geoPoint in prePauseRoute.actualPoints) {
                route.addPoint(geoPoint)
            }
        }
        map.overlays.add(route)
        Log.d("myTag", "Changed route to pre pause route")
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

    fun isDistanceWithinTheLimits(distance: Float): Boolean {
        return distance > lowerDistanceThreshold && distance < upperDistanceThreshold
    }

    fun isDistanceTooHigh(distance: Float): Boolean {
        return distance > upperDistanceThreshold
    }

    fun updatePauseCounter(geoPoint: GeoPoint) {
        incrementPauseCounter()
        if (pauseSessionCounter == 1) {
            setPauseLocation(geoPoint)
            saveSessionAverageSpeed()
            saveSessionTotalDistance()
            saveSessionTime()
        }
        Log.d("myTag", String.format("saved avg speed $prePauseAverageSpeed"))
        savePauseRoute()
        checkPauseCounter()
    }

    private fun saveSessionAverageSpeed() {
        prePauseAverageSpeed = distanceTracker.averageSpeed
    }

    private fun saveSessionTime() {
        saveSessionSeconds()
        saveSessionMinutes()
        saveSessionHours()

    }

    private fun saveSessionHours() {
        prePauseSessionHours = distanceTracker.sessionTimer.sessionHours
    }

    private fun saveSessionMinutes() {
        prePauseSessionMinutes = distanceTracker.sessionTimer.sessionMinutes
    }

    private fun saveSessionSeconds() {
        prePauseSessionSeconds = distanceTracker.sessionTimer.sessionSeconds
    }

    private fun saveSessionTotalDistance() {
        prePauseTotalDistance = distanceTracker.totalDistance
    }

    fun updateLocationCounter(geoPoint: GeoPoint, distance: Float) {
        increaseLocationCounter()
        saveLocationForOptimization(geoPoint, distance)
        checkLocationCounter()
    }

    fun updateResumeCounter() {
        incrementResumeCounter()
        checkResumeCounter()
    }
}