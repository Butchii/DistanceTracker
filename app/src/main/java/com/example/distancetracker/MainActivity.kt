package com.example.distancetracker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.util.GeoPoint
import android.location.Location
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
            val newGeoPoint =
                GeoPoint(locations[0].latitude, locations[0].longitude)
            if (!distanceTracker.startedSession) {
                distanceTracker.mapHelper.updateCurrentLocation(newGeoPoint)
                distanceTracker.mapHelper.updateStartMarkerLocation(distanceTracker.mapHelper.currentLocation)
            } else {
                if (distanceTracker.recording) {
                    val newLocation = Location("")
                    newLocation.latitude = distanceTracker.mapHelper.endMarker.position.latitude
                    newLocation.longitude = distanceTracker.mapHelper.endMarker.position.longitude
                    Log.d(
                        "myTag",
                        String.format("Distance walked ${locations[0].distanceTo(newLocation)} metres")
                    )
                    val distance = locations[0].distanceTo(newLocation)
                    if (distance > distanceTracker.mapHelper.lowerThreshHold && distance < distanceTracker.mapHelper.upperThreshHold) {
                        distanceTracker.mapHelper.updateCurrentLocation(
                            newGeoPoint
                        )
                        distanceTracker.controlPanel.infoSection.updateTotalDistance(
                            locations[0].distanceTo(
                                newLocation
                            )
                        )
                        distanceTracker.mapHelper.updateEndMarkerLocation(
                            newGeoPoint
                        )
                        distanceTracker.geoPointList.add(newGeoPoint)
                        Log.d(
                            "myTag",
                            "Distance ACCEPTED by thresh hold and updated EndMarker Position"
                        )
                        distanceTracker.mapHelper.resetPauseCounter()
                    } else if (distance > distanceTracker.mapHelper.upperThreshHold) {
                        distanceTracker.mapHelper.increaseLocationCounter()
                        distanceTracker.mapHelper.saveLocationForOptimization(newGeoPoint, distance)
                        distanceTracker.mapHelper.checkLocationCounter()
                    } else {
                        distanceTracker.mapHelper.increasePauseCounter()
                        Log.d("myTag", "Distance NOT ACCEPTED by thresh hold")
                    }
                    distanceTracker.controlPanel.infoSection.updateAverageSpeed()
                }
            }
        }
    }
}