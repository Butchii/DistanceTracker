package com.example.distancetracker.topbar

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.util.Log
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import com.example.distancetracker.DistanceTracker
import com.example.distancetracker.FireStore
import com.example.distancetracker.R
import com.example.distancetracker.Route

class TopBar(
    private val context: Context,
    private val topBarLayout: LinearLayout,
    private val distanceTracker: DistanceTracker
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
        routeListLayout = LayoutInflater.from(context).inflate(R.layout.routes_list, null)
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
            val noRoutesHint = LayoutInflater.from(context).inflate(R.layout.no_routes_hint, null)
            routeListContainer.gravity = Gravity.CENTER
            routeListContainer.addView(noRoutesHint)
        } else {
            routeListContainer.gravity = Gravity.NO_GRAVITY
            for (route in routeList) {
                val newRoute = LayoutInflater.from(context).inflate(R.layout.route_layout, null)

                val routeName = newRoute.findViewById<TextView>(R.id.routeName)
                routeName.text = route.name

                val routeDate = newRoute.findViewById<TextView>(R.id.routeDate)
                routeDate.text = route.date

                val mapBtn = newRoute.findViewById<ImageButton>(R.id.showOnMapBtn)
                mapBtn.setOnClickListener {
                    //TODO
                }
                routeListContainer.addView(newRoute)
            }
        }
    }
}