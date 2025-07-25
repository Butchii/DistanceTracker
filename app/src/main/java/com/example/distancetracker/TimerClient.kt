package com.example.distancetracker


import android.os.Build
import androidx.annotation.RequiresApi
import java.util.Timer
import java.util.TimerTask

class TimerClient(val recordingService: RecordingService) {
    var sessionSeconds: Int = 0
    var sessionMinutes: Int = 0
    var sessionHours: Int = 0

    private var sessionTimer: Timer = Timer()

    private var startedTimer: Boolean = false

    fun startTimer() {
        if (!startedTimer) {
            val timerTask = createTimerTask()
            sessionTimer = Timer()
            sessionTimer.schedule(timerTask, 0, 1000)
            startedTimer = true
        }
    }

    private fun createTimerTask(
    ) = object : TimerTask() {
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun run() {
            if (recordingService.isRecording()) {
                //recording service is on record
                runClock()
                checkGPSStatus()
                if (recordingService.isDistanceHighEnough()) {
                    recordingService.locationClient.totalDistanceInKilometres += (recordingService.locationClient.lastDistance / 1000)
                    recordingService.routePoints.add(recordingService.locationClient.currentLocation)
                    recordingService.resetPauseCounter()
                } else {
                    recordingService.increasePauseCounter()
                    recordingService.checkPauseCounter()
                }
                recordingService.locationClient.calculateAverageSpeed(sessionSeconds + sessionMinutes * 60 + sessionHours * 3600)
                recordingService.updateNotification()
            } else {
                //recording service is paused
                if (recordingService.isDistanceHighEnough()) {
                    recordingService.increaseResumeCounter()
                    recordingService.checkResumeCounter()
                }
            }
            recordingService.logData()

            recordingService.sendData(
                getTotalTimeInSeconds(),
                recordingService.locationClient.totalAverageSpeed
            )
        }
    }

    private fun runClock() {
        sessionSeconds++
        if (sessionSeconds > 0 && sessionSeconds % 60 == 0) {
            sessionSeconds = 0
            sessionMinutes++
        }
        if (sessionMinutes > 0 && sessionMinutes % 60 == 0) {
            sessionMinutes = 0
            sessionHours++
        }
    }

    fun getFormattedSessionDuration(): String {
        return String.format("$sessionHours h $sessionMinutes m $sessionSeconds s")
    }

    fun stopTimer() {
        sessionTimer.cancel()
    }

    fun getTotalTimeInSeconds(): Int {
        return sessionSeconds + sessionMinutes * 60 + sessionHours * 3600
    }

    fun checkGPSStatus() {
        if (!Utility.isGPSEnabled(recordingService.applicationContext)) {
            recordingService.pause()
        }
    }
}