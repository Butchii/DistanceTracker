package com.example.distancetracker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.util.GeoPoint
import android.util.Log
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import org.osmdroid.views.overlay.Marker

open class MainActivity : AppCompatActivity() {
    private lateinit var distanceTracker: DistanceTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        createDistanceTracker()
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
        override fun onLocationResult(p0: LocationResult) {
            super.onLocationResult(p0)
            val locations = p0.locations
            Log.d(
                "myTag",
                String.format("total distance walked before : ${distanceTracker.totalDistance}")
            )
            val newLocationAsGeoPoint =
                GeoPoint(locations[0].latitude, locations[0].longitude)
            if (!distanceTracker.hasStartedSession()) {
                distanceTracker.mapHelper.updateStartMarkerLocation(newLocationAsGeoPoint)
            } else {
                val lastLocation = distanceTracker.mapHelper.endMarkerLocation

                val distance = locations[0].distanceTo(lastLocation)
                Log.d(
                    "myTag",
                    String.format("Distance walked ${locations[0].distanceTo(lastLocation)} metres")
                )
                if (distanceTracker.isRecording()) {
                    if (distanceTracker.mapHelper.isDistanceWithinTheLimits(distance)) {
                        distanceTracker.acceptLocation(distance, newLocationAsGeoPoint)
                    } else {
                        distanceTracker.rejectLocation(distance, newLocationAsGeoPoint)
                    }
                    distanceTracker.controlPanel.infoSection.updateAverageSpeed()
                } else {
                    if (distanceTracker.mapHelper.isDistanceWithinTheLimits(distance)) {
                        distanceTracker.mapHelper.updateResumeCounter()
                    }
                }
                Log.d(
                    "myTag",
                    String.format("total distance walked after : ${distanceTracker.totalDistance}")
                )
                Log.d(
                    "myTag",
                    String.format("total distance saved in textview : ${distanceTracker.controlPanel.infoSection.totalDistanceTV.text}")
                )
            }
            if (!distanceTracker.locatedFirstTime) {
                distanceTracker.mapHelper.centerOnPoint(newLocationAsGeoPoint)
                distanceTracker.locatedFirstTime = !distanceTracker.locatedFirstTime

                distanceTracker.mapHelper.endMarkerLocation.latitude = locations[0].latitude
                distanceTracker.mapHelper.endMarkerLocation.longitude = locations[0].longitude
            }
        }
    }
}