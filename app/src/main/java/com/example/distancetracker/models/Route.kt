package com.example.distancetracker.models

import android.annotation.SuppressLint
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.UUID

data class Route(
    var name: String,
    var duration: String,
    var geoPoints: ArrayList<GeoPoint>,
    var averageSpeed: String,
    var totalDistance: String,
    var routeId: String = UUID.randomUUID().toString(),
    @SuppressLint("SimpleDateFormat") var date: String = SimpleDateFormat("dd.MM.yyyy").format(Calendar.getInstance().time),
    var geoPointsToConvert: ArrayList<HashMap<String,String>> = ArrayList(),
)
