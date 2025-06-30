package com.example.distancetracker

import android.location.Location
import kotlinx.coroutines.flow.Flow
import org.osmdroid.util.GeoPoint

interface LocationClient {

    var currentLocation: GeoPoint
    var totalDistanceInKilometres: Double
    var totalAverageSpeed: Double
    var lastDistance: Double

    fun getLocationUpdates(interval: Long): Flow<Location>

    fun getInitialLocation()

    fun calculateDistance(
        lat: Double,
        long: Double
    )

    fun calculateAverageSpeed(durationInSeconds: Int): String

    class LocationException(message: String) : Exception()

}