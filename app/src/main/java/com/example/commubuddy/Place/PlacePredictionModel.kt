package com.example.commubuddy.Place

import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlacePredictionModel(
    val placeId: String,       // Unique ID of the place
    val primaryText: String,   // Main text (e.g., name of the place)
    val secondaryText: String,  // Secondary text (e.g., address)
    val latLng: LatLng? = null // to be added
) : Parcelable