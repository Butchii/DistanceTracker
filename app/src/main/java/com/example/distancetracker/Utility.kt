package com.example.distancetracker

import android.app.ActivityManager
import android.content.Context
import android.location.LocationManager
import android.util.Log

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

        fun formatSessionTime(seconds: Int): String {
            var sessionTime = seconds

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

        fun isGPSEnabled(context: Context):Boolean{
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled =
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            return !isGpsEnabled && !isNetworkEnabled
        }
    }
}