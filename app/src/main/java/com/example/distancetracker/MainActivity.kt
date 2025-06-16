package com.example.distancetracker

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.cancel
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
            distanceTracker.startedSession = true
            distanceTracker.controlPanel.buttonSection.enterRecordingMode()
        }
    }

    override fun onStop() {
        distanceTracker.mapHelper.locationScope.cancel()
        super.onStop()
    }
}
