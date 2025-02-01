package com.example.commubuddy

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    lateinit var map: GoogleMap
    private lateinit var searchLocationButton: Button
    private lateinit var startAlarmButton: Button

    private lateinit var locationHelper: LocationHelper

    private var destinationID: String? = null
    private var destinationName: String? = null
    private var destinationAddress: String? = null
    private var destinationLatLng: LatLng? = null
    private var destinationMarker: Marker? = null
    private var originLatLng: LatLng? = null
    private var originMarker: Marker? = null
    private var isFirstLaunch: Boolean = true
    private var isAlarm: Boolean = false

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
        startAlarmButton = findViewById(R.id.button_main_start_alarm)
        startAlarmButton.setOnClickListener {
            // Intent to locations searches activity
            if (isAlarm) {
                cancelAlarm()
            } else startAlarm()
        }
    }

    override fun onResume() {
        super.onResume()

        if (!isFirstLaunch) {
            locationHelper.checkLocationPermission()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        if (isFirstLaunch) {
            initializeLocationHelper()
        }
    }

    private fun initializeLocationHelper() {
        locationHelper = LocationHelper(this, this)
        locationHelper.checkLocationPermission()
        isFirstLaunch = false
    }

    private fun setDestination(destinationLatLng: LatLng) {
        destinationMarker?.remove()
        destinationMarker = map.addMarker(MarkerOptions().position(destinationLatLng))
    }

    private fun startAlarm() {
        // Verify if origin and destination is not empty
        originLatLng = locationHelper.userLatLng
        if (originLatLng == null) {
            Toast.makeText(this, "Unable to get user location.", Toast.LENGTH_SHORT).show()
            return
        }
        if (destinationLatLng == null) {
            Toast.makeText(this, "Unable to get destination coordinates.", Toast.LENGTH_SHORT).show()
            return
        }
        originMarker = map.addMarker(MarkerOptions().position(originLatLng!!))
        setDestination(destinationLatLng!!)

        CoroutineScope(Dispatchers.Main).launch {
            val directionsHelper = DirectionsHelper()
            val urlString = directionsHelper.buildDirectionsURL(originLatLng!!, destinationLatLng!!)
            val response = withContext(Dispatchers.IO) {
                directionsHelper.fetchDirectionsData(urlString)
            }

            if (response != null) {
                Toast.makeText(this@MainActivity, "Success", Toast.LENGTH_SHORT).show()
                startAlarmButton.text = "Cancel Alarm"
                isAlarm = true
            } else {
                Toast.makeText(this@MainActivity, "Failed to fetch directions", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cancelAlarm() {
        originLatLng = null
        originMarker?.remove()
        destinationMarker?.remove()
        startAlarmButton.text = "Start Alarm"
        isAlarm = false
    }
}