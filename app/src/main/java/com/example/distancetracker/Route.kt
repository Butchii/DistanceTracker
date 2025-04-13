package com.example.distancetracker

import org.osmdroid.util.GeoPoint
import java.util.UUID

data class Route(
    var name: String,
    var duration: String,
    var geoPoints: ArrayList<GeoPoint>,
    var averageSpeed: String,
    var totalDistance: String,
    var routeId: String = UUID.randomUUID().toString()
)
