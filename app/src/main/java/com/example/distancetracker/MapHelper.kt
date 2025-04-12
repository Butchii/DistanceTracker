package com.example.distancetracker

import android.content.Context
import android.preference.PreferenceManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapHelper(context: Context, private val map: MapView) {

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

    fun addMarker(location:GeoPoint){
        val marker = Marker(map)
        marker.position = location
        map.overlays.add(marker)
    }
}