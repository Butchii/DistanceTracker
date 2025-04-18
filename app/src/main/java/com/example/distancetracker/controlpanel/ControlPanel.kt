package com.example.distancetracker.controlpanel

import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.distancetracker.R

class ControlPanel(private val controlPanelLayout: LinearLayout) : AppCompatActivity() {
    private lateinit var infoSection: InfoSection
    private lateinit var buttonSection: ButtonSection

    init {
        initializeInfoSection()
        initializeButtonSection()
    }

    private fun initializeButtonSection() {
        val buttonSectionLayout =
            controlPanelLayout.findViewById<LinearLayout>(R.id.buttonSectionLayout)
        buttonSection = ButtonSection(buttonSectionLayout)
    }

    private fun initializeInfoSection() {
        val infoSectionLayout = controlPanelLayout.findViewById<LinearLayout>(R.id.infoSectionLayout)
        infoSection = InfoSection(infoSectionLayout)
    }
}