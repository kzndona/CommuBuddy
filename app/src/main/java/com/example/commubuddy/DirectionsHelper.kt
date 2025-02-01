package com.example.commubuddy

import com.google.android.gms.maps.model.LatLng
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class DirectionsHelper {
    fun buildDirectionsURL(origin: LatLng, destination: LatLng): String {
        return "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${origin.latitude},${origin.longitude}" +
                "&destination=${destination.latitude},${destination.longitude}" +
                "&key=${BuildConfig.MAPS_API_KEY}"
    }

    fun fetchDirectionsData(urlString: String): String? {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.readText()
            reader.close()

            response
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}