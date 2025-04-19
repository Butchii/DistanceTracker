package com.example.distancetracker.topbar

import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import com.example.distancetracker.R

class TopBar(private val topBarLayout: LinearLayout) {

    private lateinit var listBtn: ImageButton
    private lateinit var routeListLayout: LinearLayout

    private var showingRouteList: Boolean = false

    init {
        initializeRouteListLayout()
        initializeListBtn()
        initializeCloseListBtn()
    }

    private fun initializeRouteListLayout() {
        routeListLayout = topBarLayout.findViewById(R.id.routeListLayout)
    }

    private fun initializeListBtn() {
        listBtn = topBarLayout.findViewById(R.id.listBtn)
        routeListLayout = topBarLayout.findViewById(R.id.routeListLayout)

        listBtn.setOnClickListener {
            if (!showingRouteList) {
                routeListLayout.visibility = View.VISIBLE
                mapHelper.map.visibility = View.GONE
                showingRouteList = true

                topBarLayout.layoutParams =
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 8f)
                mapHelper.map.layoutParams =
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            } else {
                routeListLayout.visibility = View.GONE
                mapHelper.map.visibility = View.VISIBLE
                showingRouteList = false

                topBarLayout.layoutParams =
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
                mapHelper.map.layoutParams =
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 7f)

            }
        }
    }

    private fun initializeCloseListBtn() {
        val closeListBtn = topBarLayout.findViewById<ImageButton>(R.id.closeListBtn)
        closeListBtn.setOnClickListener {
            routeListLayout.visibility = View.GONE
            mapHelper.map.visibility = View.VISIBLE
            showingRouteList = true

            topBarLayout.layoutParams =
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            mapHelper.map.layoutParams =
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 7f)
        }
    }
}