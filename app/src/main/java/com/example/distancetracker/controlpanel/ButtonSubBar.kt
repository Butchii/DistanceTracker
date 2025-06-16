package com.example.distancetracker.controlpanel

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
import com.example.distancetracker.models.Route

class ButtonSubBar(
    private val buttonSubBar: LinearLayout,
    private val activity: Context,
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

        dialog.setContentView(R.layout.save_session_dialog)

        val totalDistanceDialog = dialog.findViewById<TextView>(R.id.totalDistance)
        val totalDistanceMetres = distanceTracker.totalDistance / 1000
        totalDistanceDialog.text = String.format("%.2f km", totalDistanceMetres)

        val sessionTimeDialog = dialog.findViewById<TextView>(R.id.sessionTime)

        val averageSpeedDialog = dialog.findViewById<TextView>(R.id.averageSpeed)
        averageSpeedDialog.text = String.format("%.2f km/h", distanceTracker.averageSpeed)

        val routeNameWarning = dialog.findViewById<TextView>(R.id.routeNameWarning)
        val routeNameEntry = dialog.findViewById<EditText>(R.id.routeNameEntry)

        val saveBtn = dialog.findViewById<Button>(R.id.saveBtn)
        saveBtn.setOnClickListener {
            if (saveBtn.alpha == 1f) {
                saveSession(routeNameEntry.text.toString())
                dialog.dismiss()
            } else {
                Toast.makeText(context, "Route name is not valid!", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        routeNameEntry.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                isInputValid(saveBtn, p0.toString(), routeNameWarning)
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

    private fun isInputValid(saveBtn: Button, input: String, warning: TextView) {
        //check if value of edittext on save dialog has 3 or more characters
        //changes save button alpha depending on the result
        if (input.length >= 3) {
            saveBtn.alpha = 1f
            hideRouteNameWarning(warning)
        } else {
            saveBtn.alpha = 0.3f
            showRouteNameWarning(warning)
        }
    }

    private fun showRouteNameWarning(warning: TextView) {
        warning.visibility = View.VISIBLE
    }

    private fun hideRouteNameWarning(warning: TextView) {
        warning.visibility = View.GONE
    }

    private fun saveSession(routeName: String) {
        Toast.makeText(context, "Session saved", Toast.LENGTH_SHORT).show()
        FireStore.uploadRoute(
            Route(
                routeName,
                "",
                distanceTracker.geoPointList,
                distanceTracker.averageSpeed.toString(),
                distanceTracker.totalDistance.toString()
            )
        )
        distanceTracker.resetSession()
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
                distanceTracker.resetSession()
            }
            setNegativeButton("Cancel") { _, _ -> }
        }.create().show()
    }

    fun hideButtonBar() {
        buttonSubBar.visibility = View.GONE
    }

    fun showButtonBar() {
        buttonSubBar.visibility = View.VISIBLE
    }
}