package com.example.commubuddy.Interfaces

import com.google.android.gms.maps.model.LatLng

interface LocationUpdateListener {
    fun onLocationUpdate(location: LatLng)

    fun onShowAlarmDistanceToDestination(distance: Float)
}