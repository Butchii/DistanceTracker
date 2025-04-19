package com.example.distancetracker.controlpanel

import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.example.distancetracker.R

class ButtonSection(private val buttonSectionLayout: LinearLayout) {
    private lateinit var buttonSubBar: ButtonSubBar

    private lateinit var sessionBtn: ImageButton
    private lateinit var sessionBtnDescription: TextView

    init {
        initializeButtonSubBar()
        initializeSessionBtn()
    }

    private fun initializeButtonSubBar() {
        buttonSubBar = ButtonSubBar(buttonSectionLayout.findViewById<LinearLayout>(R.id.buttonBar))
    }

    private fun initializeSessionBtn() {
        sessionBtn = buttonSectionLayout.findViewById(R.id.sessionBtn)

        sessionBtnDescription = buttonSectionLayout.findViewById(R.id.sessionBtnDescription)
    }
}