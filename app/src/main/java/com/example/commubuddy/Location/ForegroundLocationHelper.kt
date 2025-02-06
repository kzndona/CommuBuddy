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
import androidx.core.content.ContextCompat
import com.example.commubuddy.AlarmModel
import com.example.commubuddy.Dialog.DialogHelper
import com.example.commubuddy.Interfaces.LocationUpdateListener
import com.example.commubuddy.MainActivity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng

class ForegroundLocationHelper (
    private val context: Context,
    private val activity: MainActivity,
    private val listener: LocationUpdateListener
) {
    var userLatLng: LatLng? = null
    private var locationCallback: LocationCallback? = null
    private val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)

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
        } else { checkLocationService() }
    }

    @SuppressLint("MissingPermission")
    fun checkLocationService() {
        val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGPSEnabled =  locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (!isGPSEnabled) {
            DialogHelper.showGPSDialog(activity = activity,
                onPositiveAction = {
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    activity.startActivity(intent)
                })
        } else { startForegroundGPSUpdates() }
    }

    @SuppressLint("MissingPermission")
    fun startForegroundGPSUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()

        locationCallback?.let { fusedLocationProviderClient.removeLocationUpdates(it) }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    userLatLng = LatLng(location.latitude, location.longitude)

                    listener.onFirstLocationUpdated(userLatLng!!)

                    if (AlarmModel.alarmStatus == AlarmModel.ON || AlarmModel.alarmStatus == AlarmModel.RINGING) {
                        val results = FloatArray(1)
                        distanceBetween(
                            userLatLng!!.latitude,
                            userLatLng!!.longitude,
                            AlarmModel.destinationLatLng!!.latitude,
                            AlarmModel.destinationLatLng!!.longitude,
                            results
                        )
                        val distanceBetweenResult = results[0]
                        listener.onShowAlarmDistanceToDestination(distanceBetweenResult.toInt())

                        if (distanceBetweenResult <= AlarmModel.ringDistance!! && AlarmModel.alarmStatus != AlarmModel.RINGING) {
                            activity.showAlarm()
                        }
                    }
                }
            }
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())
        activity.map.isMyLocationEnabled = true
    }

    fun stopForegroundGPSUpdates() {
        locationCallback?.let {
            fusedLocationProviderClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }
}