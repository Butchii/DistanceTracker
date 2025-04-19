package com.example.distancetracker.controlpanel

import android.widget.ImageButton
import android.widget.LinearLayout
import com.example.distancetracker.R

class ButtonSubBar(private val buttonSubBar: LinearLayout) {
    private lateinit var saveBtn: ImageButton
    private lateinit var resetBtn: ImageButton

    init {
        initializeResetBtn()
        initializeSaveBtn()
    }

    private fun initializeSaveBtn() {
        saveBtn = buttonSubBar.findViewById(R.id.saveBtn)
    }

    private fun initializeResetBtn() {
        resetBtn = buttonSubBar.findViewById(R.id.resetBtn)
    }
}