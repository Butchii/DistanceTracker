package com.example.distancetracker.controlpanel

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.distancetracker.DistanceTracker
import com.example.distancetracker.FireStore
import com.example.distancetracker.R
import com.example.distancetracker.Route

class ButtonSubBar(
    private val buttonSubBar: LinearLayout,
    private val activity: Activity,
    private val context: Context,
    private val distanceTracker: DistanceTracker
) {
    private lateinit var saveSessionBtn: ImageButton
    private lateinit var resetBtn: ImageButton

    init {
        initializeResetBtn()
        initializeSaveBtn()
    }

    private fun initializeSaveBtn() {
        saveSessionBtn = buttonSubBar.findViewById(R.id.saveBtn)

        saveSessionBtn.setOnClickListener {
            if (!distanceTracker.recording) {
                showSaveDialog()
            } else {
                Toast.makeText(
                    context,
                    "Please pause the recording before saving!",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }
    }

    private fun initializeResetBtn() {
        resetBtn = buttonSubBar.findViewById(R.id.resetBtn)

        resetBtn.setOnClickListener {
            showResetDialog()
        }
    }

    private fun showSaveDialog() {
        val dialog = Dialog(activity)

        dialog.setContentView(R.layout.dialog_save_route)

        val totalDistanceDialog = dialog.findViewById<TextView>(R.id.totalDistance)
        val totalDistanceMetres = distanceTracker.totalDistance / 1000
        totalDistanceDialog.text = String.format("%.2f km", totalDistanceMetres)

        val sessionTimeDialog = dialog.findViewById<TextView>(R.id.sessionTime)
        sessionTimeDialog.text = distanceTracker.sessionTimer.getFormattedSessionDuration()

        val averageSpeedDialog = dialog.findViewById<TextView>(R.id.averageSpeed)
        averageSpeedDialog.text = String.format("%.2f km/h", distanceTracker.averageSpeed)

        val saveBtn = dialog.findViewById<Button>(R.id.saveBtn)
        saveBtn.setOnClickListener {
            if (saveBtn.alpha == 1f) {
                saveSession()
                dialog.dismiss()
            } else {
                Toast.makeText(context, "Route name is not valid!", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        val routeNameEntry = dialog.findViewById<EditText>(R.id.routeNameEntry)
        routeNameEntry.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                isInputValid(saveBtn, p0.toString())
            }
        })

        val cancelBtn = dialog.findViewById<Button>(R.id.cancelBtn)
        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun isInputValid(saveBtn: Button, input: String) {
        //check if value of edittext on save dialog has 3 or more characters
        //changes save button alpha depending on the result
        if (input.length >= 3) {
            saveBtn.alpha = 1f
        } else {
            saveBtn.alpha = 0.3f
        }
    }

    private fun saveSession() {
        Toast.makeText(context, "Session saved", Toast.LENGTH_SHORT).show()
        FireStore.uploadRoute(
            Route(
                "route_name",
                String.format(
                    "${distanceTracker.sessionTimer.sessionSeconds + distanceTracker.sessionTimer.sessionMinutes * 60 + distanceTracker.sessionTimer.sessionHours * 3600}"
                ),
                distanceTracker.geoPointList,
                distanceTracker.averageSpeed.toString(),
                distanceTracker.totalDistance.toString()
            )
        )
        resetSession()
    }

    private fun resetSession() {
        distanceTracker.stopSession()
        distanceTracker.sessionTimer.resetSessionTimes()
        distanceTracker.sessionTimer.setSessionDurationDisplay()
        distanceTracker.controlPanel.infoSection.resetTotalDistance()
        distanceTracker.controlPanel.infoSection.resetAverageSpeed()
        distanceTracker.controlPanel.buttonSection.changeSessionButtonDescription(R.string.start_session)
        distanceTracker.controlPanel.buttonSection.changeSessionButtonIcon(R.drawable.start_icon)
        distanceTracker.mapHelper.removeRouteFromMap()
        distanceTracker.mapHelper.removeEndMarker()
        hideButtonBar()
    }

    fun showResetButton() {
        resetBtn.visibility = View.VISIBLE
    }

    fun activateSaveBtn() {
        saveSessionBtn.alpha = 1f
    }

    fun deactivateSaveBtn() {
        saveSessionBtn.alpha = 0.2f
    }

    private fun showResetDialog() {
        val alertDialog = AlertDialog.Builder(activity)
        alertDialog.apply {
            setTitle("Reset session")
            setMessage("Are you sure you want to reset the current session?")
            setPositiveButton("Yes") { _: DialogInterface?, _: Int ->
                resetSession()
            }
            setNegativeButton("Cancel") { _, _ -> }
        }.create().show()
    }

    private fun hideButtonBar() {
        buttonSubBar.visibility = View.GONE
    }

    fun showButtonBar() {
        buttonSubBar.visibility = View.VISIBLE
    }
}