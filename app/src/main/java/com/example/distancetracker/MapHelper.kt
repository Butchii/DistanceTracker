package com.example.distancetracker

import android.content.Context
import android.preference.PreferenceManager
import androidx.core.content.ContextCompat
import android.util.Log
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
        map.overlays.add(endMarker)
    }

    private fun setupMap() {
        map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        map.setMultiTouchControls(true)
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        map.controller.setZoom(15)
    }

    fun centerOnPoint(geoPoint: GeoPoint){
        map.controller.animateTo(geoPoint)
    }

    fun updateStartMarker(location: GeoPoint) {
        Log.d(
            "myTag",
            String.format("Updated STARTMARKER position from ${startMarker.position} to $location")
        )
        startMarker.position = location
        map.invalidate()
    }

    fun updateEndMarker(location: GeoPoint) {
        Log.d(
            "myTag",
            String.format("Updated ENDMARKER position from ${endMarker.position} to $location")
        )
        endMarker.position = location
        route.addPoint(location)
        map.invalidate()
    }
}