package com.example.commubuddy.Bookmark

import com.google.android.gms.maps.model.LatLng

data class BookmarksItem(
    var destinationID: String? = null,
    var destinationName: String? = null,
    var destinationAddress: String? = null,
    var destinationLatLng: LatLng? = null,
    var originLatLng: LatLng? = null,
    var ringDistance: Double? = null
)