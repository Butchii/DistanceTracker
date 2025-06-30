package com.example.distancetracker

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.util.Locale

class DefaultLocationClient(
    private val context: Context,
    private val client: FusedLocationProviderClient,
    private val mapHelper: MapHelper?,
    private val activity: Activity?
) : LocationClient {
    override var currentLocation: GeoPoint = GeoPoint(0.0, 0.0)
    override var totalDistanceInKilometres: Double = 0.0
    override var totalAverageSpeed: Double = 0.0
    override var lastDistance: Double = 0.0

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(interval: Long): Flow<Location> {
        getInitialLocation()
        return callbackFlow {
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled =
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled && !isNetworkEnabled) {
                throw LocationClient.LocationException("GPS is disabled")
            }

            val request =
                LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval).build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    super.onLocationResult(result)
                    result.locations.lastOrNull()?.let { location -> launch { send(location) } }
                }
            }
            client.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())

            awaitClose {
                client.removeLocationUpdates(locationCallback)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun getInitialLocation() {
        if (!Utility.isGPSEnabled(context)) {
            throw LocationClient.LocationException("GPS is disabled")
        }

        if (activity != null) {
            client.lastLocation.addOnSuccessListener(activity) { location ->
                if (location != null) {
                    val currentLocation = GeoPoint(location.latitude, location.longitude)
                    this.currentLocation = currentLocation
                    mapHelper?.updateCurrentLocation(currentLocation)
                    mapHelper?.updateStartMarkerLocation(mapHelper.currentLocation)
                    mapHelper?.centerOnPoint(mapHelper.currentLocation)
                }
            }
        }

        val request =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()

        client.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    override fun calculateDistance(
        lat: Double,
        long: Double
    ) {
        val oldLocation = Location("")
        oldLocation.latitude = currentLocation.latitude
        oldLocation.longitude = currentLocation.longitude

        val newLocation = Location("")
        newLocation.latitude = lat
        newLocation.longitude = long

        lastDistance = (oldLocation.distanceTo(newLocation)).toDouble()
    }

    override fun calculateAverageSpeed(durationInSeconds: Int): String {
        val averageSpeed = totalDistanceInKilometres / (durationInSeconds.toDouble() / 3600)
        totalAverageSpeed = averageSpeed
        return String.format(Locale.getDefault(), "%.3f km/h", averageSpeed)
    }

    private val locationCallback = object : LocationCallback() {
    }
}