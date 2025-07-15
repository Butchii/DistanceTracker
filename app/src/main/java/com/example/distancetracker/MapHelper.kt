package com.example.distancetracker


import android.app.Activity
import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
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

class MapHelper(
    private val context: Context,
    private val activity: Activity,
    val map: MapView
) {
    var locationScope: CoroutineScope

    private var route: Polyline = Polyline()

    private var startMarker: Marker = Marker(map)
    private var endMarker: Marker = Marker(map)

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
        map.overlays.add(route)
        map.overlays.add(startMarker)
    }

    fun addRouteToMap(routeList: ArrayList<GeoPoint>) {
        map.overlays.remove(route)
        route = Polyline()
        for (routePoint: GeoPoint in routeList) {
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
            }.launchIn(locationScope)
    }

    fun centerOnPoint(location: GeoPoint) {
        map.controller.animateTo(location)
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

    fun hideMap() {
        map.visibility = View.GONE
    }

    fun showMap() {
        map.visibility = View.VISIBLE
    }

    companion object {
        fun setupMap(map: MapView) {
            map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
            map.setMultiTouchControls(true)
            map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            map.controller.setZoom(20)
        }

        fun configureMarkers(startMarker: Marker, endMarker: Marker, context: Context) {
            startMarker.setAnchor(0.25f, 0.35f)
            startMarker.icon = ContextCompat.getDrawable(context, R.drawable.start_marker_map_icon)

            endMarker.setAnchor(0.25f, 0.35f)
            endMarker.icon = ContextCompat.getDrawable(context, R.drawable.end_marker_map_icon)
        }
    }
}