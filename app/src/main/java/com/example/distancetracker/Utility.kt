package com.example.distancetracker

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
    }
}