package com.example.distancetracker

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class Utility {
    companion object {
        fun calculateAverageSpeed(
            distance: Double,
            seconds: Int,
            minutes: Int,
            hours: Int
        ): Double {
            return (distance / (seconds + (minutes * 60) + (hours * 3600))) * 3.6
        }

        fun formatSessionTime(seconds: String): String {
            var sessionTime = seconds.toInt()

            val sessionHours = sessionTime / 3600
            sessionTime %= 3600

            val sessionMinutes = sessionTime / 60
            sessionTime %= 60
            return String.format("$sessionHours h $sessionMinutes m $sessionTime s")
        }

        fun transformMetresToKilometres(meters: Double): Double {
            return meters / 1000
        }

        fun isServiceRunningInForeground(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (RecordingService::class.java.name == service.service.className) {
                    if (service.foreground) {
                        return true
                    }
                }
            }
            return false
        }

        fun isGPSEnabled(context: Context): Boolean {
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled =
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            return isGpsEnabled || isNetworkEnabled
        }

        fun isLocationPermissionGranted(activity: MainActivity): Boolean {
            return hasAccessCoarsePermission(activity) && hasAccessFinePermission(activity)
        }

        private fun hasAccessFinePermission(activity: MainActivity): Boolean {
            return if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                true
            } else {
                requestLocationPermission(activity)
                false
            }
        }

        private fun hasAccessCoarsePermission(activity: MainActivity): Boolean {
            return if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                true
            } else {
                requestLocationPermission(activity)
                false
            }
        }

        fun requestLocationPermission(activity: MainActivity) {
            val BASIC_PERMISSION = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            val BASIC_PERMISSION_REQUESTCODE = 0
            ActivityCompat.requestPermissions(
                activity,
                BASIC_PERMISSION,
                BASIC_PERMISSION_REQUESTCODE
            )
        }

        fun showSettingsDialog(activity:Activity){
            val gpsDialog = AlertDialog.Builder(activity)
            gpsDialog.setTitle("GPS settings")
            gpsDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?")

            gpsDialog.setPositiveButton("Settings") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_LOCATION_SOURCE_SETTINGS
                )
                activity.startActivity(intent)
            }

            gpsDialog.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            gpsDialog.show()
        }
    }
}