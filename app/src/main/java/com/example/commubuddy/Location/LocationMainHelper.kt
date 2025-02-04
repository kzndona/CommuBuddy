package com.example.commubuddy.Location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location.distanceBetween
import android.location.LocationManager
import android.net.Uri
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.commubuddy.DialogHelper
import com.example.commubuddy.MainActivity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng

class LocationMainHelper (private val context: Context, private val activity: MainActivity) {

    var userLatLng: LatLng? = null
    var isLaunch: Boolean = true
    var hasAlarmTriggered: Boolean = false
    private var locationCallback: LocationCallback? = null

    fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            DialogHelper.showPermissionDialog(activity = activity,
                onPositiveAction = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", activity.packageName, null)
                    }
                    activity.startActivity(intent)
                })
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
                onPositiveAction = {
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
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

        if (locationCallback == null) {
            locationCallback = object: LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    for (location in locationResult.locations) {
                        userLatLng = LatLng(location.latitude, location.longitude)
                        if (isLaunch) {
                            activity.map.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    userLatLng!!,
                                    15.2f
                                )
                            )
                            isLaunch = false
                        }
                        if (activity.isAlarmActive && !hasAlarmTriggered) {
                            // TODO: Get distance between userLatLng and activity.destinationLatLng
                            val results = FloatArray(1)
                            distanceBetween(
                                userLatLng!!.latitude,
                                userLatLng!!.longitude,
                                activity.destinationLatLng!!.latitude,
                                activity.destinationLatLng!!.longitude,
                                results
                            )
                            val distanceBetweenResult = results[0]

                            // TODO: If distanceBetweenResult <= activity.ringDistance
                            if (distanceBetweenResult <= activity.ringDistance!!) {
                                hasAlarmTriggered = true
                                activity.startAlarmButton.text = "Stop Alarm"
                            }
                        }
                    }
                }
            }
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())
            Toast.makeText(activity, "new Callback", Toast.LENGTH_SHORT).show()
            activity.map.isMyLocationEnabled = true
        }
    }

    fun stopLocationUpdates() {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)
        locationCallback?.let {
            fusedLocationProviderClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

}