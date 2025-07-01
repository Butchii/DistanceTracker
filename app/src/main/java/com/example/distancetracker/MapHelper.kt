package com.example.distancetracker

import android.Manifest
import android.app.Activity
import android.content.Context
import android.location.Location
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.Locale

class MapHelper(
    private val context: Context,
    private val activity: Activity,
    val map: MapView,
    private val distanceTracker: DistanceTracker
) {
    var locationScope: CoroutineScope

    private var route: Polyline = Polyline()

    private var startMarker: Marker = Marker(map)
    private var endMarker: Marker = Marker(map)

    private var pauseSessionCounter: Int = 0

    private var resumeSessionCounter: Int = 0

    private var lowerDistanceThreshold: Double = 0.55  //  2  km/h

    var currentLocation: GeoPoint = GeoPoint(0.0, 0.0)

    private lateinit var locationClient: LocationClient

    private var firstLocation: Boolean = true

    init {
        Configuration.getInstance()
            .load(context, PreferenceManager.getDefaultSharedPreferences(context))
        setupMap(map)
        initializeLocationClient()
        locationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        configureMarkers(startMarker, endMarker, context)
        map.overlays.add(route)         // TODO processRoute in distancetracker
        map.overlays.add(startMarker)   // TODO nicht automatisch
        if (firstLocation) {
            map.controller.animateTo(currentLocation)
            firstLocation = false
        }
    }

    fun addRouteToMap(routeList: ArrayList<GeoPoint>){
        map.overlays.remove(route)
        route = Polyline()
        for(routePoint:GeoPoint in routeList){
            route.addPoint(routePoint)
        }
        map.overlays.add(route)
        map.invalidate()
    }

    private fun initializeLocationClient() {
        locationClient = DefaultLocationClient(
            context,
            LocationServices.getFusedLocationProviderClient(context), this, activity
        )
    }

    fun startLocationUpdates() {
        locationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        locationClient.getLocationUpdates(1000L).catch { e -> e.printStackTrace() }
            .onEach { location ->
                val lat = location.latitude
                val long = location.longitude
                val currentLocationAsGeoPoint = GeoPoint(lat, long)
                updateCurrentLocation(currentLocationAsGeoPoint)
                updateStartMarkerLocation(currentLocation)
                Log.d("myTag", String.format( "start marker location ${startMarker.position}"))
            }.launchIn(locationScope)

    }

    fun centerOnPoint(location: GeoPoint) {
        map.controller.animateTo(location)
    }

    fun getEndMarkerLocation(): Location {
        val endMarkerLocation = Location("")
        endMarkerLocation.latitude = endMarker.position.latitude
        endMarkerLocation.longitude = endMarker.position.longitude
        return endMarkerLocation
    }

    fun updateStartMarkerLocation(location: GeoPoint) {
        /*Log.d(
            "myTag",
            String.format("Updated STARTMARKER position from ${startMarker.position} to $location")
        )*/
        startMarker.position = location
        map.invalidate()
    }

    fun updateCurrentLocation(geoPoint: GeoPoint) {
        currentLocation = geoPoint
    }

    fun updateEndMarkerLocation(location: GeoPoint) {
        Log.d(
            "myTag",
            "Distance ACCEPTED by thresh hold and updated EndMarker Position"
        )
        endMarker.position = location

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
        map.overlays.add(endMarker)
    }

    fun processLocation(distance: Float, geoPoint: GeoPoint) {
        //check if calculated distance is higher then thresh hold
        //if  higher then accept location
        //otherwise reject location
        if (isDistanceValid(distance)) {
            distanceTracker.acceptLocation(distance, geoPoint)
        } else {
            distanceTracker.rejectLocation()
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
        if (pauseSessionCounter == 10 && distanceTracker.activeAutoPause) {
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

    /* private fun checkResumeCounter() {
         if (resumeSessionCounter > 5) {
             Toast.makeText(context, "Session resumed", Toast.LENGTH_SHORT).show()
             distanceTracker.resumeSession()
             distanceTracker.controlPanel.buttonSection.enterRecordingMode()
         }
     }*/

    private fun isDistanceValid(distance: Float): Boolean {
        return distance > lowerDistanceThreshold
    }

    fun updatePauseCounter() {
        incrementPauseCounter()
        checkPauseCounter()
    }

    /*fun updateResumeCounter() {
         incrementResumeCounter()
         checkResumeCounter()
     }*/

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