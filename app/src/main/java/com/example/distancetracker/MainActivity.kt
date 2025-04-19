package com.example.distancetracker

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.osmdroid.util.GeoPoint
import android.Manifest
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

open class MainActivity : AppCompatActivity() {
    private lateinit var locationRequest: LocationRequest
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var distanceTracker: DistanceTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        createDistanceTracker()
        setFusedLocationClient()
        createLocationRequest()
        getCurrentLocation()
        distanceTracker.mapHelper.centerOnPoint()
        startLocationsUpdates()
    }

    private fun createDistanceTracker() {
        distanceTracker =
            DistanceTracker(findViewById(R.id.distanceTrackerLayout), applicationContext, this)
    }

    private fun getCurrentLocation() {
        if (distanceTracker.mapHelper.isLocationEnabled()) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions()
                return
            }
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    distanceTracker.mapHelper.currentLocation.latitude = location.latitude
                    distanceTracker.mapHelper. currentLocation.longitude = location.longitude
                }
                distanceTracker.mapHelper.updateStartMarkerLocation(distanceTracker.mapHelper.currentLocation)
            }
        }
    }

    private fun requestPermissions() {
        val LOCATION_REQUEST = 44
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            LOCATION_REQUEST
        )
    }

    private fun setFusedLocationClient() {
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(applicationContext)
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000).build()
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

                    if (locations[0].distanceTo(newLocation) > 1) {
                        distanceTracker.mapHelper.updateCurrentLocation(
                            newGeoPoint
                        )
                        distanceTracker.controlPanel.infoSection.updateTotalDistance(locations[0].distanceTo(newLocation))
                        distanceTracker.mapHelper.updateEndMarkerLocation(
                            newGeoPoint
                        )
                        distanceTracker.geoPointList.add(newGeoPoint)
                        Log.d("myTag", "Distance ACCEPTED by threshhold and updated")
                    } else {
                        Log.d("myTag", "Distance NOT ACCEPTED by threshhold")
                    }
                    distanceTracker.controlPanel.infoSection.updateAverageSpeed()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("myTag", "Resuming tracking")
        startLocationsUpdates()
    }

    override fun onPause() {
        super.onPause()
        if (!distanceTracker.recording) {
            Log.d("myTag", "Stopped tracking")
            stopLocationUpdates()
        }
    }

    private fun startLocationsUpdates() {
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest, locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }
}