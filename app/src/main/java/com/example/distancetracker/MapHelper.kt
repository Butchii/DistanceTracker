package com.example.distancetracker

import android.content.Context
import android.location.Location
import android.preference.PreferenceManager
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class MapHelper(private val context: Context, private val map: MapView) {

    private var route: Polyline = Polyline()

    init {
        Configuration.getInstance()
            .load(context, PreferenceManager.getDefaultSharedPreferences(context))
        setupMap()
    }

    private fun setupMap() {
        map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        map.setMultiTouchControls(true)
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        map.controller.setZoom(15)
        map.controller.animateTo(GeoPoint(50.5532715, 7.1045565))
    }

    fun addMarker(location: GeoPoint) {
        val marker = Marker(map)
        marker.icon = ContextCompat.getDrawable(context,R.drawable.map_marker)
        marker.setAnchor(0.175f, 0.35f)
        marker.position = location
        map.overlays.add(marker)
        route.addPoint(location)
        map.overlays.add(route)
        map.invalidate()
    }

    fun updateCurrentLocationMarker(currentLocation: GeoPoint) {
        map.overlays.clear()
        val currentLocationMarker = Marker(map)
        currentLocationMarker.setAnchor(0.175f, 0.35f)
        currentLocationMarker.icon = ContextCompat.getDrawable(context,R.drawable.map_marker)
        currentLocationMarker.position = currentLocation
        map.overlays.add(currentLocationMarker)
        map.invalidate()
    }
}