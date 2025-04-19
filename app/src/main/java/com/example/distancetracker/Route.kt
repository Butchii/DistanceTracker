package com.example.distancetracker

import org.osmdroid.util.GeoPoint
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.UUID

data class Route(
    var name: String,
    var duration: String,
    var geoPoints: ArrayList<GeoPoint>,
    var averageSpeed: String,
    var totalDistance: String,
    var routeId: String = UUID.randomUUID().toString(),
    var date: String = Calendar.getInstance().time.toString()
)
