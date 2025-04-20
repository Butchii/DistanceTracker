package com.example.distancetracker

import android.util.Log
import com.example.distancetracker.topbar.TopBar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.osmdroid.util.GeoPoint

class FireStore {
    companion object {
        fun getRoutes(routes: ArrayList<Route>, topBar: TopBar) {
            routes.clear()
            val db = Firebase.firestore
            db.collection("routeList").get().addOnSuccessListener { documents ->
                for (document in documents) {
                    val route = Route(
                        document.get("name") as String,
                        document.get("duration") as String,
                        document.get("geoPoints") as ArrayList<GeoPoint>,
                        document.get("averageSpeed") as String,
                        document.get("totalDistance") as String,
                        document.get("routeId") as String,
                        document.get("date") as String
                    )
                    routes.add(route)
                }
                topBar.addRoutesToList()
            }
        }

        fun uploadRoute(route: Route) {
            val db = Firebase.firestore
            db.collection("routeList").document(route.routeId).set(route).addOnSuccessListener {
                Log.d("myTag", "Session uploaded to Fire store")
            }
        }
    }
}