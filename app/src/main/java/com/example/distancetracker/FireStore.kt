package com.example.distancetracker

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.osmdroid.util.GeoPoint

class FireStore {

    companion object {
        fun getRoutes(routeList: ArrayList<Route>, activity: MainActivity) {
            val db = Firebase.firestore
            db.collection("routeList").get().addOnSuccessListener { documents ->
                for (document in documents) {
                    val route = Route(
                        document.get("name") as String,
                        document.get("duration") as String,
                        document.get("geoPoints") as ArrayList<GeoPoint>,
                        document.get("averageSpeed") as String,
                        document.get("totalDistance") as String,
                        document.get("routeId") as String
                    )
                    routeList.add(route)
                }
                activity.addRoutestoList()
            }
        }

        fun uploadRoute(route: Route) {
            val db = Firebase.firestore
            db.collection("routeList").document(route.routeId).set(route)
        }
    }
}