package com.example.distancetracker

import android.widget.TextView
import java.util.Timer
import java.util.TimerTask

class CustomTimer(private val sessionDurationTV: TextView, private val activity: MainActivity) {
    var sessionSeconds: Int = 0
    var sessionMinutes: Int = 0
    var sessionHours: Int = 0

    private var sessionTimer: Timer = Timer()

    fun createTimer() {
        val timerTask = createTimerTask()
        sessionTimer = Timer()
        sessionTimer.schedule(timerTask, 0, 1000)
    }

    private fun createTimerTask() = object : TimerTask() {
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
            activity.runOnUiThread {
                setSessionDurationDisplay()
            }
        }
    }

    fun setSessionDurationDisplay() {
        //set the textview, which displays session duration, to hours,minutes,
        // seconds saved in sessionHours, sessionMinutes and sessionSeconds
        sessionDurationTV.text =
            String.format("$sessionHours h $sessionMinutes m $sessionSeconds s")
    }

    fun getFormattedSessionDuration():String{
        return String.format("$sessionHours h $sessionMinutes m $sessionSeconds s")
    }

    fun stopTimer() {
        sessionTimer.cancel()
    }

    fun resetSessionTimes() {
        resetSessionSeconds()
        resetSessionMinutes()
        resetSessionHours()
    }

    private fun resetSessionHours() {
        sessionHours = 0
    }

    private fun resetSessionMinutes() {
        sessionMinutes = 0
    }

    private fun resetSessionSeconds() {
        sessionSeconds = 0
    }
}