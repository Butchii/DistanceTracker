package com.example.distancetracker

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FireStore {

    companion object {
        fun getRoutes() {
            val db = Firebase.firestore

            //TODO
        }

        fun uploadRoute(route: Route) {
            val db = Firebase.firestore
            db.collection("routeList").document(route.routeId).set(route)
        }
    }
}