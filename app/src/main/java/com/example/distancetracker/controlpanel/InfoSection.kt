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
        distanceTracker.totalDistance += distanceWalked

        val totalDistanceMetres = Utility.transformMetresToKilometres(distanceTracker.totalDistance)

        totalDistanceTV.text = String.format(Locale.getDefault(),"%.2f km", totalDistanceMetres)
    }

    fun updateAverageSpeed() {
        if (distanceTracker.sessionTimer.sessionSeconds > 2) {
            distanceTracker.averageSpeed = Utility.calculateAverageSpeed(
                distanceTracker.totalDistance,
                distanceTracker.sessionTimer.sessionSeconds,
                distanceTracker.sessionTimer.sessionMinutes,
                distanceTracker.sessionTimer.sessionHours
            )

            averageSpeedTV.text = String.format(Locale.getDefault(),"%.2f km/h", distanceTracker.averageSpeed)
        }
    }
}