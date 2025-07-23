package com.example.distancetracker.topbar

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import com.example.distancetracker.DistanceTracker
import com.example.distancetracker.FireStore
import com.example.distancetracker.R
import com.example.distancetracker.models.Route
import android.widget.Button
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.example.distancetracker.MapHelper
import com.example.distancetracker.Utility
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.Locale

class TopBar(
    private val context: Context,
    private val topBarLayout: LinearLayout,
    private val distanceTracker: DistanceTracker,
    private val activity: Context
) {
    private lateinit var sessionsBtn: ImageButton
    private lateinit var settingsBtn: ImageButton

    lateinit var gpsIndicator: ImageView

    private lateinit var topBarExpand: LinearLayout

    private lateinit var sessionListLayout: View
    private lateinit var settingsLayout: View

    private lateinit var sessionListContainer: LinearLayout

    private var showingRoutes: Boolean = false
    private var showingSettings: Boolean = false

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var autoPauseSwitch: Switch

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var autoResumeSwitch: Switch

    private var sessionList: ArrayList<Route> = ArrayList()

    private lateinit var map: MapView

    init {
        initializeTopBarExpandLayout()
        initializeGPSIndicator()
        initializeRouteListLayout()
        initializeSettingsLayout()
        initializeRoutesBtn()
        initializeSettingsBtn()
    }

    private fun initializeSettingsBtn() {
        settingsBtn = topBarLayout.findViewById(R.id.settingsBtn)

        settingsBtn.setOnClickListener {
            if (!showingSettings) {
                showingRoutes = false
                topBarExpand.removeAllViews()
                showSettings()
                showTopBarExpand()
                distanceTracker.mapHelper.hideMap()
            } else {
                hideTopBarExpand()
                distanceTracker.mapHelper.showMap()
            }
        }
    }

    private fun showSettings() {
        showingSettings = true
        topBarExpand.addView(settingsLayout)
    }

    private fun showTopBarExpand() {
        topBarExpand.visibility = View.VISIBLE
        topBarLayout.layoutParams =
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 7f)
    }

    fun hideTopBarExpand() {
        topBarExpand.visibility = View.GONE
        topBarExpand.removeAllViews()
        showingSettings = false
        showingRoutes = false
        topBarLayout.layoutParams =
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
    }

    private fun initializeTopBarExpandLayout() {
        topBarExpand = topBarLayout.findViewById(R.id.topBarExpandLayout)
    }

    private fun initializeGPSIndicator() {
        gpsIndicator = topBarLayout.findViewById(R.id.gpsIcon)
    }

    @SuppressLint("InflateParams")
    private fun initializeRouteListLayout() {
        sessionListLayout = LayoutInflater.from(context).inflate(R.layout.session_list, null)
        sessionListContainer = sessionListLayout.findViewById(R.id.sessionListContainer)

        initializeCloseListBtn()
    }

    @SuppressLint("InflateParams")
    private fun initializeSettingsLayout() {
        settingsLayout = LayoutInflater.from(context).inflate(R.layout.settings_menu, null)
        initializeAutoPauseSwitch()
        initializeAutoResumeSwitch()
    }

    private fun initializeAutoResumeSwitch() {
        autoResumeSwitch = settingsLayout.findViewById(R.id.resumeSwitch)
        autoResumeSwitch.setOnClickListener {
            distanceTracker.isAutoResumeActivated = autoResumeSwitch.isChecked
        }
    }

    private fun initializeAutoPauseSwitch() {
        autoPauseSwitch = settingsLayout.findViewById(R.id.pauseSwitch)
        autoPauseSwitch.setOnClickListener {
            distanceTracker.isAutoPauseActivated = autoPauseSwitch.isChecked
        }
    }

    private fun initializeRoutesBtn() {
        sessionsBtn = topBarLayout.findViewById(R.id.listBtn)

        sessionsBtn.setOnClickListener {
            if (!showingRoutes) {
                topBarExpand.removeAllViews()
                showingSettings = false
                FireStore.getRoutes(sessionList, this)
                showTopBarExpand()
                showRouteList()
                distanceTracker.mapHelper.hideMap()
            } else {
                hideTopBarExpand()
                distanceTracker.mapHelper.showMap()
            }
        }
    }

    private fun showRouteList() {
        showingRoutes = true
        topBarExpand.addView(sessionListLayout)
    }


    private fun initializeCloseListBtn() {
        val closeListBtn = sessionListLayout.findViewById<ImageButton>(R.id.closeListBtn)
        closeListBtn.setOnClickListener {
            topBarExpand.visibility = View.GONE
            distanceTracker.mapHelper.map.visibility = View.VISIBLE
            showingRoutes = false

            topBarLayout.layoutParams =
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            distanceTracker.mapHelper.map.layoutParams =
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 6f)
        }
    }

    @SuppressLint("InflateParams")
    fun addRoutesToList() {
        sessionListContainer.removeAllViews()
        if (sessionList.isEmpty()) {
            val noRoutesHint = LayoutInflater.from(context).inflate(R.layout.no_session_hint, null)
            sessionListContainer.gravity = Gravity.CENTER
            sessionListContainer.addView(noRoutesHint)
        } else {
            sessionListContainer.gravity = Gravity.NO_GRAVITY
            for (session in sessionList) {
                val newRoute = LayoutInflater.from(context).inflate(R.layout.session_layout, null)

                newRoute.layoutParams =
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 350)

                val sessionName = newRoute.findViewById<TextView>(R.id.sessionName)
                sessionName.text = session.name

                val sessionDate = newRoute.findViewById<TextView>(R.id.sessionDate)
                sessionDate.text = session.date

                val sessionDurationTV = newRoute.findViewById<TextView>(R.id.sessionDuration)
                sessionDurationTV.text = Utility.formatSessionTime(session.duration.toInt())

                val sessionAvgTV = newRoute.findViewById<TextView>(R.id.sessionAvg)
                sessionAvgTV.text =
                    String.format(Locale.getDefault(), "%.4s km/h", session.averageSpeed)

                val sessionDistanceTV = newRoute.findViewById<TextView>(R.id.sessionDistance)
                sessionDistanceTV.text =
                    String.format(Locale.getDefault(), "%.4s km", session.totalDistance)

                val sessionBtn = newRoute.findViewById<ImageButton>(R.id.showOnMapBtn)
                sessionBtn.setOnClickListener {
                    showSessionDialog(session)
                }
                sessionListContainer.addView(newRoute)

                val divider = View(context)
                divider.layoutParams =
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                divider.setBackgroundColor(ContextCompat.getColor(context, R.color.grey))
                sessionListContainer.addView(divider)
            }
        }
    }

    private fun showSessionDialog(route: Route) {
        val dialog = Dialog(activity)

        dialog.setContentView(R.layout.session_dialog)

        map = dialog.findViewById(R.id.map)
        MapHelper.setupMap(map)

        val startLatitude = route.geoPointsToConvert[0]["latitude"].toString().toDouble()
        val startLongitude = route.geoPointsToConvert[0]["longitude"].toString().toDouble()
        map.controller.animateTo(GeoPoint(startLatitude, startLongitude))

        val startMarker = Marker(map)
        startMarker.position = GeoPoint(startLatitude, startLongitude)

        val endLatitude = route.geoPointsToConvert.last()["latitude"].toString().toDouble()
        val endLongitude = route.geoPointsToConvert.last()["longitude"].toString().toDouble()

        val endMarker = Marker(map)
        endMarker.position = GeoPoint(endLatitude, endLongitude)

        MapHelper.configureMarkers(startMarker, endMarker, context)

        map.overlays.add(startMarker)
        map.overlays.add(endMarker)

        val routeLine = Polyline()
        for (geoPoint: HashMap<String, String> in route.geoPointsToConvert) {
            val pointLatitude = geoPoint["latitude"].toString().toDouble()
            val pointLongitude = geoPoint["longitude"].toString().toDouble()
            routeLine.addPoint(GeoPoint(pointLatitude, pointLongitude))
        }
        map.overlays.add(routeLine)

        val sessionName = dialog.findViewById<TextView>(R.id.sessionName)
        sessionName.text = route.name

        val sessionTotalDistance = dialog.findViewById<TextView>(R.id.sessionDistance)
        val distance = Utility.transformMetresToKilometres(route.totalDistance.toDouble())
        sessionTotalDistance.text = String.format("%.4s km", distance)

        val sessionDuration = dialog.findViewById<TextView>(R.id.sessionDuration)
        sessionDuration.text = Utility.formatSessionTime(route.duration.toInt())

        val sessionAvg = dialog.findViewById<TextView>(R.id.sessionAvg)
        sessionAvg.text = String.format("%.4s km/h", route.averageSpeed)

        val sessionDate = dialog.findViewById<TextView>(R.id.sessionDate)
        sessionDate.text = route.date

        val closeBtn = dialog.findViewById<Button>(R.id.closeBtn)
        closeBtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(false)
        dialog.show()
    }

    fun showGPSDisabled(activity: Activity) {
        gpsIndicator.setImageResource(R.drawable.gps_disabled)
        gpsIndicator.setOnClickListener {
            Utility.showSettingsDialog(activity)
        }
    }

    fun showGPSEnabled() {
        gpsIndicator.setImageResource(R.drawable.gps_enabled)
        gpsIndicator.setOnClickListener(null)
    }
}