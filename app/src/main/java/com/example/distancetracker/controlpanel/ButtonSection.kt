package com.example.distancetracker.controlpanel

import android.content.Context
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.distancetracker.DistanceTracker
import com.example.distancetracker.MainActivity
import com.example.distancetracker.R
import com.example.distancetracker.Utility

class ButtonSection(
    private val buttonSectionLayout: LinearLayout,
    private val context: Context,
    private val activity: MainActivity,
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
            distanceTracker
        )
    }

    private fun initializeSessionBtn() {
        sessionBtn = buttonSectionLayout.findViewById(R.id.sessionBtn)

        sessionBtnDescription = buttonSectionLayout.findViewById(R.id.sessionBtnDescription)

        sessionBtn.setOnClickListener {
            if (!Utility.isServiceRunningInForeground(context)) {
                //no session started yet
                if(Utility.isLocationPermissionGranted(activity)){
                    distanceTracker.startSession()
                }else{
                    Utility.requestLocationPermission(activity)
                }
            } else {
                //session started
                if (distanceTracker.isRecording) {
                    //recording
                    distanceTracker.pauseSession()
                    enterPauseMode()
                } else {
                    //paused
                    distanceTracker.resumeSession()
                    distanceTracker.topBar.hideTopBarExpand()
                    enterRecordingMode()
                }
            }
        }
    }

    fun enterRecordingMode() {
        changeSessionButtonDescription(R.string.recording)
        changeSessionButtonIcon(R.drawable.record_icon)
        buttonSubBar.showButtonBar()
        buttonSubBar.deactivateSaveBtn()
    }

    fun enterPauseMode() {
        changeSessionButtonDescription(R.string.paused)
        changeSessionButtonIcon(R.drawable.pause_icon)
        buttonSubBar.activateSaveBtn()
    }

    private fun changeSessionButtonIcon(iconId: Int) {
        sessionBtn.setImageResource(iconId)
    }

    private fun changeSessionButtonDescription(stringId: Int) {
        sessionBtnDescription.text =
            ContextCompat.getString(context, stringId)
    }

    fun reset(){
        changeSessionButtonDescription(R.string.start_session)
        changeSessionButtonIcon(R.drawable.start_icon)
        buttonSubBar.hideButtonBar()
    }
}