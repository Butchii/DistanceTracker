package com.example.distancetracker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.util.GeoPoint
import android.util.Log
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult

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
            val currentLocationAsGeoPoint =
                GeoPoint(locations[0].latitude, locations[0].longitude)
            if (!distanceTracker.hasStartedSession()) {
                distanceTracker.mapHelper.updateStartMarkerLocation(currentLocationAsGeoPoint)
            } else {
                val lastLocation = distanceTracker.mapHelper.endMarkerLocation
                val distance = locations[0].distanceTo(lastLocation)
                if (distanceTracker.isRecording()) {
                    Log.d(
                        "myTag",
                        String.format("Distance walked ${locations[0].distanceTo(lastLocation)} metres")
                    )

                    if (distanceTracker.mapHelper.isDistanceWithinTheLimits(distance)) {
                        distanceTracker.controlPanel.infoSection.updateTotalDistance(
                            locations[0].distanceTo(
                                lastLocation
                            )
                        )
                        distanceTracker.mapHelper.updateEndMarkerLocation(
                            currentLocationAsGeoPoint
                        )
                        distanceTracker.geoPointList.add(currentLocationAsGeoPoint)
                        distanceTracker.mapHelper.resetPauseCounter()
                    } else if (distanceTracker.mapHelper.isDistanceTooHigh(distance)) {
                        distanceTracker.mapHelper.updateLocationCounter(
                            currentLocationAsGeoPoint,
                            distance
                        )
                    } else {
                        distanceTracker.mapHelper.updatePauseCounter(currentLocationAsGeoPoint)
                        Log.d("myTag", "Distance NOT ACCEPTED by thresh hold")
                    }
                    distanceTracker.controlPanel.infoSection.updateAverageSpeed()
                } else {
                    if (distanceTracker.mapHelper.isDistanceWithinTheLimits(distance)) {
                        distanceTracker.mapHelper.updateResumeCounter()
                    }
                }
            }
        }
    }
}