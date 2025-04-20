package com.example.distancetracker

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import android.preference.PreferenceManager
import androidx.core.content.ContextCompat
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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
    private val locationCallback: LocationCallback
) {

    var route: Polyline = Polyline()

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var startMarker: Marker = Marker(map)
    var endMarker: Marker = Marker(map)

    private var locationAverage: ArrayList<GeoPoint> = ArrayList()
    private var locationCounter: Int = 0

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
        startMarker.setAnchor(0.175f, 0.35f)
        startMarker.icon = ContextCompat.getDrawable(context, R.drawable.map_marker)
        map.overlays.add(startMarker)

        endMarker.setAnchor(0.175f, 0.35f)
        endMarker.icon = ContextCompat.getDrawable(context, R.drawable.map_marker)
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
            String.format("Updated ENDMARKER position from ${endMarker.position} to $location")
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

    fun updateCurrentLocation(newLocation: GeoPoint) {
        currentLocation = newLocation
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
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000).build()
    }
}