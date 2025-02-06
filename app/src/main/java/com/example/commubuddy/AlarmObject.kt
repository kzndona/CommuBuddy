package com.example.commubuddy

import com.google.android.gms.maps.model.LatLng

object AlarmObject {
    const val ON: Int = 1
    const val OFF: Int = 0
    const val RINGING: Int = -1
    const val BOOKMARKING: Int = 2

    var destinationID: String? = null
    var destinationName: String? = null
    var destinationAddress: String? = null
    var destinationLatLng: LatLng? = null
    var originLatLng: LatLng? = null
    var ringDistance: Double? = null
    var status: Int = OFF

    fun save() {
        // TODO: Save this to a list temporarily, doesn't have enough time for database
    }
}