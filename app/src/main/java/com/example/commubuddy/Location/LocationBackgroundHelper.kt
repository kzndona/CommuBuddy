package com.example.commubuddy.Location

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng

class LocationBackgroundHelper : Service() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()

    override fun onCreate() {
        super.onCreate()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        startForeground(1, createNotification()) // Foreground service with notification
        requestLocationUpdates()
        Log.d("LocationService", "Background location service started")
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        val pendingIntent = getPendingIntent()
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, pendingIntent)
        Log.d("LocationService", "Requested location updates with PendingIntent")
    }

    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(this, LocationReceiver::class.java)
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationProviderClient.removeLocationUpdates(getPendingIntent())
        stopForeground(STOP_FOREGROUND_REMOVE)
        Log.d("LocationService", "Background location service stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channelId = "location_service"
        val channel =
            NotificationChannel(channelId, "Location Service", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Tracking location")
            .build()
    }

}
