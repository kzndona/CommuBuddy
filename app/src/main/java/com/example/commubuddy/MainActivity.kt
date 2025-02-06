package com.example.commubuddy

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.commubuddy.Direction.DirectionsHelper
import com.example.commubuddy.Interfaces.LocationUpdateListener
import com.example.commubuddy.Location.ForegroundLocationHelper
import com.example.commubuddy.Place.PlacePredictionModel
import com.example.commubuddy.Place.PlaceSearchesActivity
import com.example.commubuddy.databinding.ActivityMainBinding
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

class MainActivity : AppCompatActivity(), OnMapReadyCallback, LocationUpdateListener {

    lateinit var map: GoogleMap

    private var destinationMarker: Marker? = null
    private var route: Polyline? = null
    private var mapCircle: Circle? = null

    private lateinit var foregroundLocationHelper: ForegroundLocationHelper
    private lateinit var searchActivityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var binding: ActivityMainBinding

    private var isFirstLaunch: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setUpSearchLauncher()

        binding.buttonMainSearchLocation.setOnClickListener {
            val intent = Intent(this, PlaceSearchesActivity::class.java)
            searchActivityResultLauncher.launch(intent)
        }
        binding.seekbarMainRingDistance.setOnSeekBarChangeListener( object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val scaledProgress = 50 + (progress / 100.0) * (2000 - 50)
                updateMapCircle(scaledProgress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) { return }
            override fun onStopTrackingTouch(seekBar: SeekBar?) { return }
        })
        binding.buttonMainStartAlarm.setOnClickListener {
            when (AlarmModel.alarmStatus) {
                AlarmModel.ON -> cancelAlarm()
                AlarmModel.OFF -> startAlarm()
                AlarmModel.RINGING -> dismissAlarm()
            }
        }
        binding.seekbarMainRingDistance.isEnabled = false
        binding.buttonMainStartAlarm.isEnabled = false
    }

    override fun onResume() {
        super.onResume()

        if (!isFirstLaunch) {
            foregroundLocationHelper.checkLocationPermission()
        }
    }

    override fun onPause() {
        super.onPause()
        foregroundLocationHelper.stopForegroundGPSUpdates()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        if (isFirstLaunch) {
            initializeLocationHelper()
        }
        googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition(
            LatLng(14.59,121.04), 10.2f, 0f, 0f)))
    }

    private fun startAlarm() {
        AlarmModel.originLatLng = foregroundLocationHelper.userLatLng
        if (AlarmModel.originLatLng == null) {
            Toast.makeText(this, "Unable to get user location.", Toast.LENGTH_SHORT).show()
            return
        }
        if (AlarmModel.destinationLatLng == null) {
            Toast.makeText(this, "Unable to get destination coordinates.", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            val directionsHelper = DirectionsHelper()
            val urlString = directionsHelper.buildDirectionsURL(AlarmModel.originLatLng!!, AlarmModel.destinationLatLng!!)
            val response = withContext(Dispatchers.IO) {
                directionsHelper.fetchDirectionsData(urlString)
            }

            if (response != null) {
                val routePoints = directionsHelper.parseRoute(response)
                if (routePoints != null) {
                    drawRoute(routePoints)
                    binding.buttonMainStartAlarm.text = "Cancel Alarm"
                    AlarmModel.alarmStatus = AlarmModel.ON

                    binding.buttonMainSearchLocation.visibility = View.INVISIBLE
                    binding.seekbarMainRingDistance.visibility = View.INVISIBLE
                } else {
                    Toast.makeText(this@MainActivity, "Failed to parse directions", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@MainActivity, "Failed to fetch directions", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cancelAlarm() {
        AlarmModel.originLatLng = null
        route?.remove()
        binding.buttonMainStartAlarm.text = "Start Alarm"
        AlarmModel.alarmStatus = AlarmModel.OFF
        binding.buttonMainSearchLocation.visibility = View.VISIBLE
        binding.seekbarMainRingDistance.visibility = View.VISIBLE
    }

    private fun dismissAlarm() {
        cancelAlarm()
    }

    fun showAlarm() {
        binding.buttonMainStartAlarm.text = "Stop Alarm"
        AlarmModel.alarmStatus = AlarmModel.RINGING
    }

    private fun setDestination(destinationLatLng: LatLng) {
        destinationMarker?.remove()
        destinationMarker = null
        mapCircle?.remove()
        mapCircle = null

        destinationMarker = map.addMarker(MarkerOptions().position(destinationLatLng))
        updateMapCircle(50.0)
        binding.seekbarMainRingDistance.isEnabled = true
        binding.buttonMainStartAlarm.isEnabled = true
    }

    private fun updateMapCircle(radius: Double) {
        AlarmModel.ringDistance = radius
        val center = AlarmModel.destinationLatLng?: return
        if (mapCircle == null) {
            mapCircle = map.addCircle(
                CircleOptions()
                    .center(center)
                    .radius(radius)
                    .strokeWidth(1f)
                    .fillColor(Color.argb(.2f,0f,0f,255f)))
        } else {
            mapCircle!!.radius = radius
        }
    }

    private fun drawRoute(routePoints: List<LatLng>) {
        val polylineOptions = PolylineOptions().apply {
            addAll(routePoints)
            color(Color.BLUE)
            width(10f)
            zIndex(50f)
        }
        route = map.addPolyline(polylineOptions)
        if (mapCircle != null && AlarmModel.ringDistance != null) {
            updateMapCircle(AlarmModel.ringDistance!!)
        }
    }

    private fun initializeLocationHelper() {
        foregroundLocationHelper = ForegroundLocationHelper(this, this, this)
        foregroundLocationHelper.checkLocationPermission()
        isFirstLaunch = false
    }

    override fun onLocationUpdated(location: LatLng) {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15.2f))
    }

    private fun setUpSearchLauncher() {
        searchActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val selectedPlace: PlacePredictionModel? = result.data?.getParcelableExtra("selected_place")
                selectedPlace?.let {
                    AlarmModel.destinationID = it.placeId
                    AlarmModel.destinationName = it.primaryText
                    AlarmModel.destinationAddress = it.secondaryText
                    AlarmModel.destinationLatLng = it.latLng!!
                    setDestination(AlarmModel.destinationLatLng!!)
                }
            }
        }
    }
}