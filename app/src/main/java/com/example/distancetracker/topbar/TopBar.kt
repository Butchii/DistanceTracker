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
import androidx.core.content.ContextCompat
import com.example.distancetracker.Utility
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.Locale

class TopBar(
    private val context: Context,
    private val topBarLayout: LinearLayout,
    private val distanceTracker: DistanceTracker,
    private val activity: Activity
) {
    private lateinit var routesBtn: ImageButton
    private lateinit var settingsBtn: ImageButton
    private lateinit var topBarExpand: LinearLayout

    private lateinit var routeListLayout: View
    private lateinit var settingsLayout: View

    private lateinit var routeListContainer: LinearLayout

    private var showingRoutes: Boolean = false
    private var showingSettings: Boolean = false

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var autoPauseSwitch: Switch

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var autoResumeSwitch: Switch

    private var routeList: ArrayList<Route> = ArrayList()

    private lateinit var map: MapView

    init {
        initializeTopBarExpandLayout()
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

    private fun hideTopBarExpand() {
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

    @SuppressLint("InflateParams")
    private fun initializeRouteListLayout() {
        routeListLayout = LayoutInflater.from(context).inflate(R.layout.session_list, null)
        routeListContainer = routeListLayout.findViewById(R.id.routeListContainer)

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
            distanceTracker.activeAutoResume = autoResumeSwitch.isChecked
        }
    }

    private fun initializeAutoPauseSwitch() {
        autoPauseSwitch = settingsLayout.findViewById(R.id.pauseSwitch)
        autoPauseSwitch.setOnClickListener {
            distanceTracker.activeAutoPause = autoPauseSwitch.isChecked
        }
    }

    private fun initializeRoutesBtn() {
        routesBtn = topBarLayout.findViewById(R.id.listBtn)

        routesBtn.setOnClickListener {
            if (!showingRoutes) {
                topBarExpand.removeAllViews()
                showingSettings = false
                FireStore.getRoutes(routeList, this)
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
        topBarExpand.addView(routeListLayout)
    }


    private fun initializeCloseListBtn() {
        val closeListBtn = routeListLayout.findViewById<ImageButton>(R.id.closeListBtn)
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
        routeListContainer.removeAllViews()
        if (routeList.isEmpty()) {
            val noRoutesHint = LayoutInflater.from(context).inflate(R.layout.no_session_hint, null)
            routeListContainer.gravity = Gravity.CENTER
            routeListContainer.addView(noRoutesHint)
        } else {
            routeListContainer.gravity = Gravity.NO_GRAVITY
            for (route in routeList) {
                val newRoute = LayoutInflater.from(context).inflate(R.layout.session_layout, null)

                newRoute.layoutParams =
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 350)

                val routeName = newRoute.findViewById<TextView>(R.id.routeName)
                routeName.text = route.name

                val routeDate = newRoute.findViewById<TextView>(R.id.routeDate)
                routeDate.text = route.date

                val sessionDurationTV = newRoute.findViewById<TextView>(R.id.sessionDuration)
                sessionDurationTV.text = Utility.formatSessionTime(route.duration.toInt())

                val sessionAvgTV = newRoute.findViewById<TextView>(R.id.sessionAvg)
                sessionAvgTV.text =
                    String.format(Locale.getDefault(), "Avg Speed: %.4s km/h", route.averageSpeed)

                val sessionDistanceTV = newRoute.findViewById<TextView>(R.id.sessionDistance)
                sessionDistanceTV.text =
                    String.format(Locale.getDefault(), "Distance: %.4s km", route.totalDistance)

                val sessionBtn = newRoute.findViewById<ImageButton>(R.id.showOnMapBtn)
                sessionBtn.setOnClickListener {
                    showSessionDialog(route)
                }
                routeListContainer.addView(newRoute)

                val divider = View(context)
                divider.layoutParams =
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                divider.setBackgroundColor(ContextCompat.getColor(context, R.color.grey))
                routeListContainer.addView(divider)
            }
        }
    }

    private fun setupMap(dialog: Dialog) {
        map = dialog.findViewById(R.id.map)
        map.setMultiTouchControls(true)
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        map.controller.setZoom(18)
    }

    private fun showSessionDialog(route: Route) {
        val dialog = Dialog(activity)

        dialog.setContentView(R.layout.session_dialog)

        setupMap(dialog)

        val startLatitude = route.geoPointsToConvert[0]["latitude"].toString().toDouble()
        val startLongitude = route.geoPointsToConvert[0]["longitude"].toString().toDouble()
        map.controller.animateTo(GeoPoint(startLatitude, startLongitude))

        val startMarker = Marker(map)
        startMarker.position = GeoPoint(startLatitude, startLongitude)
        startMarker.setAnchor(0.25f, 0.35f)
        startMarker.icon = ContextCompat.getDrawable(context, R.drawable.start_marker_map_icon)
        map.overlays.add(startMarker)

        val endLatitude = route.geoPointsToConvert.last()["latitude"].toString().toDouble()
        val endLongitude = route.geoPointsToConvert.last()["longitude"].toString().toDouble()

        val endMarker = Marker(map)
        endMarker.position = GeoPoint(endLatitude, endLongitude)
        endMarker.setAnchor(0.25f, 0.35f)
        endMarker.icon = ContextCompat.getDrawable(context, R.drawable.end_marker_map_icon)
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
        val distance = route.totalDistance.toDouble() / 1000
        sessionTotalDistance.text = String.format("Distance: %.4s km", distance)

        val sessionDuration = dialog.findViewById<TextView>(R.id.sessionDuration)
        sessionDuration.text = Utility.formatSessionTime(route.duration.toInt())

        val sessionAvg = dialog.findViewById<TextView>(R.id.sessionAvg)
        sessionAvg.text = String.format("Average speed: %.2s km/h", route.averageSpeed)

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
}