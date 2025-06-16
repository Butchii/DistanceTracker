package com.example.distancetracker.controlpanel

import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.distancetracker.DistanceTracker
import com.example.distancetracker.R
import com.example.distancetracker.Utility
import java.util.Locale

class InfoSection(
    private val infoSectionLayout: LinearLayout,
    private val distanceTracker: DistanceTracker,
    private val context: Context
) {
    private lateinit var sessionDurationTV: TextView
    private lateinit var totalDistanceTV: TextView
    private lateinit var averageSpeedTV: TextView

    init {
        initializeSessionDurationInfo()
        initializeTotalDistanceInfo()
        initializeAverageSpeedInfo()
    }

    private fun initializeSessionDurationInfo() {
        sessionDurationTV = infoSectionLayout.findViewById(R.id.sessionDuration)
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

    fun updateSessionDuration(formattedDuration: String) {
        sessionDurationTV.text = formattedDuration
    }

    fun updateTotalDistance(distanceWalked: Float) {
        totalDistanceTV.text = String.format(Locale.getDefault(), "%.2f km", distanceWalked)
    }

    fun updateAverageSpeed(avgSpeed: String) {
        averageSpeedTV.text =
            String.format(Locale.getDefault(), "%.2f km/h", avgSpeed)
    }

    fun reset() {
        resetTotalDistance()
        resetAverageSpeed()
    }
}