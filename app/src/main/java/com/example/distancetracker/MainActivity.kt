package com.example.distancetracker

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.util.GeoPoint
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult

open class MainActivity : AppCompatActivity() {
    private lateinit var distanceTracker: DistanceTracker


    private lateinit var debugLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        createDistanceTracker()

        debugLayout = findViewById(R.id.debugLayout)
    }

    private fun createDistanceTracker() {
        distanceTracker =
            DistanceTracker(
                findViewById(R.id.distanceTrackerLayout),
                applicationContext,
                this,
                locationCallback
            )
    }

    override fun onResume() {
        super.onResume()
        Log.d("myTag", "Resuming tracking")
        distanceTracker.mapHelper.startLocationsUpdates()
    }

    override fun onPause() {
        super.onPause()
        if (!distanceTracker.recording) {
            Log.d("myTag", "Stopped tracking")
            distanceTracker.mapHelper.stopLocationUpdates()
        }
    }

    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(newLocation: LocationResult) {
            super.onLocationResult(newLocation)
            val locations = newLocation.locations
            val newLocationAsGeoPoint =
                GeoPoint(locations[0].latitude, locations[0].longitude)
            if (!distanceTracker.hasStartedSession()) {
                distanceTracker.mapHelper.updateStartMarkerLocation(newLocationAsGeoPoint)
                distanceTracker.mapHelper.updateEndMarkerLocation(newLocationAsGeoPoint)
            } else {
                val lastLocation = distanceTracker.mapHelper.endMarkerLocation

                val distance = locations[0].distanceTo(lastLocation)

                createDebugMessage(String.format("Distance walked ${locations[0].distanceTo(lastLocation)} metres"))
                if (distanceTracker.isRecording()) {
                    if (distanceTracker.mapHelper.isDistanceValid(distance)) {
                        distanceTracker.acceptLocation(distance, newLocationAsGeoPoint)
                        createDebugMessage(String.format("Distance accepted"))
                    } else {
                        distanceTracker.rejectLocation(newLocationAsGeoPoint)
                        createDebugMessage(String.format("Distance denied"))
                    }
                    distanceTracker.controlPanel.infoSection.updateAverageSpeed()
                } else {
                    if (distanceTracker.mapHelper.isDistanceValid(distance)) {
                        distanceTracker.mapHelper.updateResumeCounter()
                    }
                }
            }
            if (!distanceTracker.locatedFirstTime) {
                distanceTracker.mapHelper.centerOnPoint(newLocationAsGeoPoint)
                distanceTracker.locatedFirstTime = !distanceTracker.locatedFirstTime

                distanceTracker.mapHelper.endMarkerLocation.latitude = locations[0].latitude
                distanceTracker.mapHelper.endMarkerLocation.longitude = locations[0].longitude
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun createDebugMessage(message: String) {
        val debugMessageLayout =
            LayoutInflater.from(applicationContext).inflate(R.layout.debug_message, null)
        val debugMessage = debugMessageLayout.findViewById<TextView>(R.id.debugMessage)
        debugMessage.text = message
        debugLayout.addView(debugMessageLayout)
    }
}