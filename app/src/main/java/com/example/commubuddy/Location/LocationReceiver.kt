package com.example.commubuddy.Location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng

class LocationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null) {
            val result = LocationResult.extractResult(intent)
            if (result != null) {
                for (location in result.locations) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    Log.d("LocationReceiver", "New location: $latLng")
                    // You can update a ViewModel, local database, or send to a server here.
                }
            } else {
                Log.d("LocationReceiver", "No location result received")
            }
        }
    }
}
