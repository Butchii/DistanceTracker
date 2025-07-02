package com.example.distancetracker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import kotlinx.coroutines.cancel

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
                this
            )
    }

    override fun onDestroy() {
        super.onDestroy()
        distanceTracker.unregisterReceiver()
    }

    override fun onRestart() {
        super.onRestart()
        Log.d("myTag", "Resuming tracking")
        if (!Utility.isServiceRunningInForeground(applicationContext)) {
            if (!Utility.isGPSEnabled(applicationContext)) {
                distanceTracker.mapHelper.startLocationUpdates()
            } else {
                throw LocationClient.LocationException("GPS is disabled")
            }
        } else {
            distanceTracker.runningSession = true
            distanceTracker.controlPanel.buttonSection.enterRecordingMode()
        }
    }

    override fun onStop() {
        distanceTracker.mapHelper.locationScope.cancel()
        super.onStop()
    }
}
