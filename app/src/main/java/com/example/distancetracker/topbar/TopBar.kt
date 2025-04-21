package com.example.distancetracker.topbar

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
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
    private lateinit var listBtn: ImageButton
    private lateinit var routeLayout: LinearLayout
    private lateinit var routeListLayout:LinearLayout

    private var showingRouteList: Boolean = false

    private var routeList: ArrayList<Route> = ArrayList()

    init {
        initializeRouteLayout()
        initializeListBtn()
        initializeCloseListBtn()
    }

    private fun initializeRouteLayout() {
        routeLayout = topBarLayout.findViewById(R.id.routeListLayout)
        routeListLayout = routeLayout.findViewById(R.id.routeList)
    }

    private fun initializeListBtn() {
        listBtn = topBarLayout.findViewById(R.id.listBtn)

        listBtn.setOnClickListener {
            if (!showingRouteList) {
                FireStore.getRoutes(routeList, this)
                showRouteList()
                distanceTracker.mapHelper.hideMap()                
            } else {
                hideRouteList()
                distanceTracker.mapHelper.showMap()
            }
        }
    }

    private fun showRouteList(){
        routeLayout.visibility = View.VISIBLE
        showingRouteList = true
        topBarLayout.layoutParams =
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 8f)
    }

    private fun hideRouteList(){
        routeLayout.visibility = View.GONE
        showingRouteList = false
        topBarLayout.layoutParams =
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
    }

    private fun initializeCloseListBtn() {
        val closeListBtn = routeLayout.findViewById<ImageButton>(R.id.closeListBtn)
        closeListBtn.setOnClickListener {
            routeLayout.visibility = View.GONE
            distanceTracker.mapHelper.map.visibility = View.VISIBLE
            showingRouteList = true

            topBarLayout.layoutParams =
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            distanceTracker.mapHelper.map.layoutParams =
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 7f)
        }
    }

    @SuppressLint("InflateParams")
    fun addRoutesToList() {
        routeListLayout.removeAllViews()
        if (routeList.isEmpty()) {
            val noRoutesHint = LayoutInflater.from(context).inflate(R.layout.no_routes_hint, null)
            routeListLayout.gravity = Gravity.CENTER
            routeListLayout.addView(noRoutesHint)
        } else {
            for (route in routeList) {
                routeListLayout.gravity = Gravity.NO_GRAVITY
                val newRoute = LayoutInflater.from(context).inflate(R.layout.route_layout, null)

                val routeName = newRoute.findViewById<TextView>(R.id.routeName)
                routeName.text = route.name

                val mapBtn = newRoute.findViewById<ImageButton>(R.id.showOnMapBtn)
                mapBtn.setOnClickListener {
                    //TODO
                }
                routeListLayout.addView(newRoute)
            }
        }
    }
}