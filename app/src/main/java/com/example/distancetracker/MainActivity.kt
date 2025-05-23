package com.example.distancetracker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import org.osmdroid.util.GeoPoint

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
        override fun onLocationResult(newLocation: LocationResult) {
            super.onLocationResult(newLocation)
            val locations = newLocation.locations
            distanceTracker.mapHelper.updateCurrentLocation(
                GeoPoint(
                    locations[0].latitude,
                    locations[0].longitude
                )
            )
            val currentLocation = distanceTracker.mapHelper.currentLocation
            if (!distanceTracker.hasStartedSession()) {
                //no session is running
                distanceTracker.mapHelper.updateMarkerLocations(currentLocation)
            } else {
                //session is running
                val lastLocation = distanceTracker.mapHelper.getEndMarkerLocation()
                val distance = locations[0].distanceTo(lastLocation)
                if (distanceTracker.isRecording()) {
                    // session is recording
                    distanceTracker.mapHelper.processLocation(distance, currentLocation)
                    distanceTracker.controlPanel.infoSection.updateAverageSpeed()
                } else {
                    // session is in pause mode
                    if (distanceTracker.mapHelper.isDistanceValid(distance) && distanceTracker.activeAutoResume) {
                        distanceTracker.mapHelper.updateResumeCounter()
                    }
                }
            }
            if (!distanceTracker.locatedFirstTime) {
                //first location received
                distanceTracker.mapHelper.checkForFirstLocation(currentLocation)
            }
        }
    }
}