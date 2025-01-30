package com.example.commubuddy

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng

class LocationHelper (private val context: Context, private val activity: MainActivity) {

    var userLatLng: LatLng? = null

    fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            DialogHelper.showPermissionDialog(activity = activity,
                onPositiveAction = { val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", activity.packageName, null)
                }
                    activity.startActivity(intent)
                } )
        } else {
            checkLocationService()
        }
    }

    @SuppressLint("MissingPermission")
    fun checkLocationService() {
        // Check if Location Service is on
        val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGPSEnabled =  locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        // If not, call DialogHelper.showServiceDialog()
        if (!isGPSEnabled) {
            DialogHelper.showGPSDialog(activity = activity,
                onPositiveAction = { val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    activity.startActivity(intent)
                })
        } else {
            // If yes, dismiss dialog if any, activate userLocation on maps
            enableGPSLocationUpdates()
        }
    }

    @SuppressLint("MissingPermission")
    fun enableGPSLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    userLatLng = LatLng(location.latitude, location.longitude)
                }
            }
        }, Looper.getMainLooper())
        activity.map.isMyLocationEnabled = true
    }
}