package com.example.distancetracker.controlpanel

import android.content.Context
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.distancetracker.DistanceTracker
import com.example.distancetracker.R

class InfoSection(
    private val infoSectionLayout: LinearLayout,
    private val distanceTracker: DistanceTracker,
    private val context: Context
) {

    private lateinit var totalDistanceTV: TextView
    private lateinit var averageSpeedTV: TextView

    init {
        initializeTotalDistanceInfo()
        initializeAverageSpeedInfo()
    }

    private fun initializeAverageSpeedInfo() {
        averageSpeedTV = infoSectionLayout.findViewById(R.id.averageSpeed)
    }

    private fun initializeTotalDistanceInfo() {
        totalDistanceTV = infoSectionLayout.findViewById(R.id.totalDistance)
    }

    fun resetTotalDistance() {
        distanceTracker.totalDistance = 0.0
        totalDistanceTV.text = ContextCompat.getString(context, R.string._0_0km)
    }

    fun resetAverageSpeed() {
        distanceTracker.averageSpeed = 0.0
        averageSpeedTV.text = ContextCompat.getString(context, R.string._0_0_km_h)
    }

    fun updateTotalDistance(distanceWalked: Float) {
        Log.d("myTag",String.format("total distance was ${distanceTracker.totalDistance} "))
        distanceTracker.totalDistance += distanceWalked
        Log.d("myTag",String.format("total distance is ${distanceTracker.totalDistance} "))
        val totalDistanceMetres = distanceTracker.totalDistance / 1000
        totalDistanceTV.text = String.format("%.2f km", totalDistanceMetres)
    }

    fun updateAverageSpeed() {
        if (distanceTracker.sessionTimer.sessionSeconds > 2) {
            distanceTracker.averageSpeed =
                (distanceTracker.totalDistance / (distanceTracker.sessionTimer.sessionSeconds + (distanceTracker.sessionTimer.sessionMinutes * 60) + (distanceTracker.sessionTimer.sessionHours * 3600))) * 3.6
            averageSpeedTV.text = String.format("%.2f km/h", distanceTracker.averageSpeed)
            Log.d("myTAg",String.format("this is the other avg ${distanceTracker.averageSpeed}"))
        }
    }

    fun setAverageSpeed() {
        averageSpeedTV.text = String.format("%.2f km/h", distanceTracker.averageSpeed)
    }

    fun setTotalDistance() {
        totalDistanceTV.text = String.format("%.2f km", distanceTracker.totalDistance)
    }

}