package com.example.distancetracker

import android.content.Intent
import android.os.IBinder

class MyLocationService :android.app.Service() {
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {


        return START_STICKY
    }
}