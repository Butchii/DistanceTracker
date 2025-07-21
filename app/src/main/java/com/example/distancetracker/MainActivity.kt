package com.example.distancetracker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.cancel

open class MainActivity : AppCompatActivity() {
    private lateinit var distanceTracker: DistanceTracker

    private var checkedGPSAlready: Boolean = false

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
        if (!Utility.isRecordingServiceRunning(applicationContext)) {
            if (Utility.isGPSEnabled(applicationContext)) {
                distanceTracker.mapHelper.startLocationUpdates()
                distanceTracker.topBar.showGPSEnabled()
            } else {
                if (!checkedGPSAlready) {
                    distanceTracker.topBar.showGPSDisabled(this)
                    Utility.showSettingsDialog(this)
                    checkedGPSAlready != checkedGPSAlready
                }
            }
        }
    }

    override fun onStop() {
        distanceTracker.mapHelper.locationScope.cancel()
        super.onStop()
    }
}
