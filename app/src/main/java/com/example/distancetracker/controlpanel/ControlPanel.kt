package com.example.distancetracker.controlpanel

import android.app.Activity
import android.content.Context
import android.widget.LinearLayout
import com.example.distancetracker.DistanceTracker
import com.example.distancetracker.R

class ControlPanel(
    private val context: Context,
    private val activity: Activity,
    private val controlPanelLayout: LinearLayout,
    private val distanceTracker: DistanceTracker
) {
    private lateinit var infoSection: InfoSection
    private lateinit var buttonSection: ButtonSection

    init {
        initializeInfoSection()
        initializeButtonSection()
    }

    private fun initializeButtonSection() {
        val buttonSectionLayout =
            controlPanelLayout.findViewById<LinearLayout>(R.id.buttonSectionLayout)
        buttonSection = ButtonSection(buttonSectionLayout, context, activity, distanceTracker)
    }

    private fun initializeInfoSection() {
        val infoSectionLayout =
            controlPanelLayout.findViewById<LinearLayout>(R.id.infoSectionLayout)
        infoSection = InfoSection(infoSectionLayout, distanceTracker, context)
    }


}