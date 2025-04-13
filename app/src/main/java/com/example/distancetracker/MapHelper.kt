package com.example.distancetracker

import android.content.Context
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

    var route: Polyline = Polyline()

    private var startMarker: Marker = Marker(map)
    private var endMarker: Marker = Marker(map)

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
        map.controller.animateTo(GeoPoint(50.5532715, 7.1045565))
    }

    fun updateStartMarker(location: GeoPoint) {
        startMarker.position = location
        map.invalidate()
    }

    fun updateEndMarker(location: GeoPoint) {
        endMarker.position = location
        route.addPoint(location)
        map.invalidate()
    }
}