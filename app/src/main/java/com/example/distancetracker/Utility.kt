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
            //TODO
            var sessionTime: String = ""

            return sessionTime
        }

        fun transformMetresToKilometres(meters:Double):Double{
            return meters / 1000
        }
    }
}