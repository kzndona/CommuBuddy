package com.example.commubuddy.Direction

import android.util.Log
import com.example.commubuddy.BuildConfig
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import org.json.JSONObject
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

    fun parseRoute(jsonString: String): MutableList<LatLng>? {
        val routePoints = mutableListOf<LatLng>()
        try {
            val jsonObject = JSONObject(jsonString)
            val routes = jsonObject.getJSONArray("routes")

            Log.e("CMBDY", routes.length().toString())
            if (routes.length() == 0) return null

            val legs = routes.getJSONObject(0).getJSONArray("legs")
            val steps = legs.getJSONObject(0).getJSONArray("steps")

            for (i in 0 until steps.length()) {
                val polyline = steps.getJSONObject(i)
                    .getJSONObject("polyline")
                    .getString("points")
                routePoints.addAll(PolyUtil.decode(polyline))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        return routePoints
    }
}