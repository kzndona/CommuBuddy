package com.example.commubuddy

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.commubuddy.Location.LocationBackgroundHelper
import com.example.commubuddy.Location.LocationMainHelper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    lateinit var map: GoogleMap
    private lateinit var searchLocationButton: Button
    lateinit var startAlarmButton: Button
    private lateinit var ringDistanceSeekBar: SeekBar

    private lateinit var locationMainHelper: LocationMainHelper

    private var destinationID: String? = null
    private var destinationName: String? = null
    private var destinationAddress: String? = null
    var destinationLatLng: LatLng? = null
    private var destinationMarker: Marker? = null
    private var originLatLng: LatLng? = null
    private var originMarker: Marker? = null
    var ringDistance: Double? = null
    private var route: Polyline? = null
    private var mapCircle: Circle? = null

    private var isFirstLaunch: Boolean = true
    var isAlarmActive: Boolean = false

    private val searchActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedPlace: PlacePredictionModel? = result.data?.getParcelableExtra("selected_place")
            selectedPlace?.let {
                destinationID = it.placeId
                destinationName = it.primaryText
                destinationAddress = it.secondaryText
                destinationLatLng = it.latLng!!
                setDestination(destinationLatLng!!)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        searchLocationButton = findViewById(R.id.button_main_search_location)
        searchLocationButton.setOnClickListener {
            // Intent to locations searches activity
            val intent = Intent(this, LocationSearchesActivity::class.java)
            searchActivityResultLauncher.launch(intent)
        }
        ringDistanceSeekBar = findViewById(R.id.seekbar_main_ring_distance)
        ringDistanceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val scaledProgress = 50 + (progress / 100.0) * (2000 - 50)
                updateMapCircle(scaledProgress)
                ringDistance = scaledProgress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                return
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                return
            }
        })
        ringDistanceSeekBar.isEnabled = false
        startAlarmButton = findViewById(R.id.button_main_start_alarm)
        startAlarmButton.setOnClickListener {
            // Intent to locations searches activity
            if (isAlarmActive) {
                cancelAlarm()
            } else startAlarm()
        }
        startAlarmButton.isEnabled = false
    }

    override fun onResume() {
        super.onResume()

        if (!isFirstLaunch) {
            stopLocationBackgroundService()
            locationMainHelper.checkLocationPermission()
        }

    }

    override fun onPause() {
        super.onPause()

        if (isAlarmActive) {
            locationMainHelper.stopLocationUpdates()
            startLocationBackgroundService()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isAlarmActive) {
            locationMainHelper.stopLocationUpdates()
            startLocationBackgroundService()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        if (isFirstLaunch) {
            initializeLocationHelper()
        }
        googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition(
            LatLng(14.59,121.04),
            10.2f, 0f, 0f
        )))
    }

    private fun initializeLocationHelper() {
        locationMainHelper = LocationMainHelper(this, this)
        locationMainHelper.checkLocationPermission()
        isFirstLaunch = false
    }

    private fun setDestination(destinationLatLng: LatLng) {
        destinationMarker?.remove()
        destinationMarker = map.addMarker(MarkerOptions().position(destinationLatLng))
        mapCircle?.remove()
        mapCircle = null
        updateMapCircle(50.0)
        ringDistanceSeekBar.isEnabled = true
        startAlarmButton.isEnabled = true
    }

    private fun startAlarm() {
        originLatLng = locationMainHelper.userLatLng
        if (originLatLng == null) {
            Toast.makeText(this, "Unable to get user location.", Toast.LENGTH_SHORT).show()
            return
        }
        if (destinationLatLng == null) {
            Toast.makeText(this, "Unable to get destination coordinates.", Toast.LENGTH_SHORT).show()
            return
        }

        originMarker = map.addMarker(MarkerOptions().position(originLatLng!!))

        CoroutineScope(Dispatchers.Main).launch {
            val directionsHelper = DirectionsHelper()
            val urlString = directionsHelper.buildDirectionsURL(originLatLng!!, destinationLatLng!!)
            val response = withContext(Dispatchers.IO) {
                directionsHelper.fetchDirectionsData(urlString)
            }

            if (response != null) {
                val routePoints = directionsHelper.parseRoute(response)
                if (routePoints != null) {
                    drawRoute(routePoints)
                    startAlarmButton.text = "Cancel Alarm"
                    isAlarmActive = true
                    locationMainHelper.hasAlarmTriggered = false
                    searchLocationButton.visibility = View.INVISIBLE
                    ringDistanceSeekBar.visibility = View.INVISIBLE
                } else {
                    Toast.makeText(this@MainActivity, "Failed to parse directions", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@MainActivity, "Failed to fetch directions", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cancelAlarm() {
        originLatLng = null
        originMarker?.remove()
        route?.remove()
        startAlarmButton.text = "Start Alarm"
        isAlarmActive = false
        searchLocationButton.visibility = View.VISIBLE
        ringDistanceSeekBar.visibility = View.VISIBLE
    }

    private fun drawRoute(routePoints: List<LatLng>) {
        val polylineOptions = PolylineOptions().apply {
            addAll(routePoints)
            color(Color.BLUE)
            width(10f)
            zIndex(50f)
        }
        route = map.addPolyline(polylineOptions)
        if (mapCircle != null && ringDistance != null) {
            updateMapCircle(ringDistance!!)
        }
    }

    private fun updateMapCircle(radius: Double) {
        val center = destinationLatLng?: return
        if (mapCircle == null) {
            mapCircle = map.addCircle(
                CircleOptions()
                    .center(center)
                    .radius(radius)
                    .strokeWidth(1f)
                    .fillColor(Color.argb(.2f,0f,0f,255f))
                    .zIndex(10f)
            )
        } else {
            mapCircle!!.radius = radius
        }
    }

    private fun startLocationBackgroundService() {
        val intent = Intent(this, LocationBackgroundHelper::class.java)
        this.startForegroundService(intent)
        Log.d("LocationService", "Background service called")
    }

    private fun stopLocationBackgroundService() {
        val intent = Intent(this, LocationBackgroundHelper::class.java)
        this.stopService(intent)
    }
}