package com.example.distancetracker

import java.util.Timer
import java.util.TimerTask

class TimerClient(val recordingService: RecordingService) {
    var sessionSeconds: Int = 0
    var sessionMinutes: Int = 0
    var sessionHours: Int = 0

    private var sessionTimer: Timer = Timer()

    fun startTimer() {
        val timerTask = createTimerTask()
        sessionTimer = Timer()
        sessionTimer.schedule(timerTask, 0, 1000)
    }

    private fun createTimerTask(
    ) = object : TimerTask() {
        override fun run() {
            sessionSeconds++
            if (sessionSeconds > 0 && sessionSeconds % 60 == 0) {
                sessionSeconds = 0
                sessionMinutes++
            }
            if (sessionMinutes > 0 && sessionMinutes % 60 == 0) {
                sessionMinutes = 0
                sessionHours++
            }
            recordingService.logInformation(
                recordingService.locationClient.currentLocation.latitude,
                recordingService.locationClient.currentLocation.longitude
            )
            recordingService.sendData(
                getTotalTimeInSeconds(),
                recordingService.locationClient.totalAverageSpeed
            )
            if(recordingService.isRecording()){
                recordingService.checkPauseCounter()
            }else{
                recordingService.checkResumeCounter()
            }
            if(recordingService.isRecording() && !Utility.isGPSEnabled(recordingService.applicationContext)){
                recordingService.pause()
            }

            recordingService.updateNotification()
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
}