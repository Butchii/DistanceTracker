package com.example.distancetracker

import android.app.Activity
import android.content.Context
import android.location.LocationManager
import android.preference.PreferenceManager
import androidx.core.content.ContextCompat
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class MapHelper(private val context: Context, val map: MapView) {

    var route: Polyline = Polyline()

    private var startMarker: Marker = Marker(map)
    var endMarker: Marker = Marker(map)

    private var locationAverage: ArrayList<GeoPoint> = ArrayList()
    private var locationCounter: Int = 0

    var currentLocation: GeoPoint = GeoPoint(0.0, 0.0)

    init {
        Configuration.getInstance()
            .load(context, PreferenceManager.getDefaultSharedPreferences(context))
        setupMap()
        configureMarkers()
        map.overlays.add(route)
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

    fun centerOnPoint() {
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
}