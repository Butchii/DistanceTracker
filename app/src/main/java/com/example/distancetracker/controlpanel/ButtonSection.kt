package com.example.distancetracker.controlpanel

import android.app.Activity
import android.content.Context
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
    lateinit var buttonSubBar: ButtonSubBar

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
            distanceTracker
        )
    }

    private fun initializeSessionBtn() {
        sessionBtn = buttonSectionLayout.findViewById(R.id.sessionBtn)

        sessionBtnDescription = buttonSectionLayout.findViewById(R.id.sessionBtnDescription)

        sessionBtn.setOnClickListener {
            if (distanceTracker.mapHelper.isLocationEnabled()) {
                if (!distanceTracker.startedSession) {
                    //no session started yet
                    distanceTracker.startSession()
                    changeSessionButtonDescription(R.string.recording)
                    changeSessionButtonIcon(R.drawable.record_icon)
                    buttonSubBar.showResetButton()
                } else {
                    //session already exists
                    if (distanceTracker.recording) {
                        distanceTracker.pauseSession()
                        enterPauseMode()
                    } else {
                        distanceTracker.resumeSession()
                        enterRecordingMode()
                    }
                }
            } else {
                Toast.makeText(context, "Please enable your GPS!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    fun enterRecordingMode(){
        changeSessionButtonDescription(R.string.recording)
        changeSessionButtonIcon(R.drawable.record_icon)
        buttonSubBar.deactivateSaveBtn()
    }

    fun enterPauseMode(){
        changeSessionButtonDescription(R.string.paused)
        changeSessionButtonIcon(R.drawable.pause_icon)
        buttonSubBar.activateSaveBtn()
    }

    fun changeSessionButtonIcon(iconId: Int) {
        sessionBtn.setImageResource(iconId)
    }

    fun changeSessionButtonDescription(stringId: Int) {
        sessionBtnDescription.text =
            ContextCompat.getString(context, stringId)
    }
}