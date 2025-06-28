package com.example.distancetracker

import android.location.Location
import kotlinx.coroutines.flow.Flow
import org.osmdroid.util.GeoPoint

interface LocationClient {

    var currentLocation: GeoPoint
    var totalDistance: Double
    var totalAverageSpeed: Double

    fun getLocationUpdates(interval: Long): Flow<Location>

    fun pauseLocationUpdates()

    fun stopLocationUpdates()

    fun getInitialLocation()

    fun calculateDistance(
        lat: Double,
        long: Double
    ): Double

    fun calculateAverageSpeed(durationInSeconds: Int): String

    class LocationException(message: String) : Exception()

}