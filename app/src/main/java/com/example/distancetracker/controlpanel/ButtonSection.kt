package com.example.distancetracker.controlpanel

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.distancetracker.DistanceTracker
import com.example.distancetracker.R

class ButtonSection(
    private val buttonSectionLayout: LinearLayout,
    private val context: Context,
    private val activity: Activity,
    private val distanceTracker: DistanceTracker
) {
    private lateinit var buttonSubBar: ButtonSubBar

    private lateinit var sessionBtn: ImageButton
    private lateinit var sessionBtnDescription: TextView

    init {
        initializeButtonSubBar()
        initializeSessionBtn()
    }

    private fun initializeButtonSubBar() {
        buttonSubBar = ButtonSubBar(
            buttonSectionLayout.findViewById(R.id.buttonBar),
            activity,
            context,
            distanceTracker.recording
        )
    }

    private fun initializeSessionBtn() {
        sessionBtn = buttonSectionLayout.findViewById(R.id.sessionBtn)

        sessionBtnDescription = buttonSectionLayout.findViewById(R.id.sessionBtnDescription)

        sessionBtn.setOnClickListener {
            if (isLocationEnabled()) {
                if (!startedSession) {
                    //no session started yet
                    startSession()
                    changeMainButtonDescription(R.string.recording)
                    changeMainButtonIcon(R.drawable.record_icon)
                    showResetButton()
                } else {
                    //session already exists
                    if (recording) {
                        pauseSession()
                        changeMainButtonDescription(R.string.paused)
                        changeMainButtonIcon(R.drawable.pause_icon)
                        activateSaveBtn()
                    } else {
                        resumeSession()
                        changeMainButtonDescription(R.string.recording)
                        changeMainButtonIcon(R.drawable.record_icon)
                        deactivateSaveBtn()
                    }
                }
            } else {
                Toast.makeText(context, "Please enable your GPS!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun resumeSession() {
        startRecording()
        sessionTimer.createTimer()
    }

    private fun startSession() {
        geoPointList.add(currentLocation)
        mapHelper.updateStartMarkerLocation(currentLocation)
        mapHelper.addEndMarker(currentLocation)
        mapHelper.route.addPoint(currentLocation)

        startedSession = true
        startRecording()
        showButtonBar()
        sessionTimer.createTimer()
    }

    private fun startRecording() {
        recording = true
    }

    private fun showButtonBar() {
        buttonBar.visibility = View.VISIBLE
    }

    private fun hideButtonBar() {
        buttonBar.visibility = View.GONE
    }

    private fun stopSession() {
        sessionTimer.stopTimer()
        startedSession = false
        stopRecording()
    }

    private fun changeSessionButtonIcon(iconId: Int) {
        sessionBtn.setImageResource(iconId)
    }

    private fun changeSessionButtonDescription(stringId: Int) {
        sessionBtnDescription.text =
            ContextCompat.getString(context, stringId)
    }
}