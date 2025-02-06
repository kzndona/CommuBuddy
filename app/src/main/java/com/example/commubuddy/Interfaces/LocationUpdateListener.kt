package com.example.commubuddy.Interfaces

import com.google.android.gms.maps.model.LatLng

interface LocationUpdateListener {
    fun onLocationUpdated(location: LatLng)
}