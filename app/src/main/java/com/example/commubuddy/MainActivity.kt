package com.example.commubuddy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var searchLocationButton: Button

    private lateinit var destinationID: String
    private lateinit var destinationName: String
    private lateinit var destinationAddress: String
    private lateinit var destinationLatLng: LatLng
    private var destinationMarker: Marker? = null
    private lateinit var originLatLng: LatLng

    private val searchActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedPlace: PlacePredictionModel? = result.data?.getParcelableExtra("selected_place")
            selectedPlace?.let {
                destinationID = it.placeId
                destinationName = it.primaryText
                destinationAddress = it.secondaryText
                destinationLatLng = it.latLng!!
                setDestination(destinationLatLng)
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
    }

    override fun onResume() {
        super.onResume()

        checkLocationPermission()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
    }

    fun setDestination(destinationLatLng: LatLng) {
        destinationMarker?.remove()
        destinationMarker = map.addMarker(MarkerOptions().position(destinationLatLng))
    }

    fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            DialogHelper.showPermissionDialog(activity = this, onPositiveAction = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            } )
        }
    }
}