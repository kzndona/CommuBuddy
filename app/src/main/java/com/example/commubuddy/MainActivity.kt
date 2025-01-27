package com.example.commubuddy

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var searchLocationButton: Button

    private lateinit var destinationID: String
    private lateinit var destinationName: String
    private lateinit var destinationAddress: String
    private lateinit var destinationLatLng: LatLng
    private lateinit var originLatLng: LatLng

    private val searchActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedPlace: PlacePredictionModel? = result.data?.getParcelableExtra("selected_place")
            selectedPlace?.let {
                destinationID = it.placeId
                destinationName = it.primaryText
                destinationAddress = it.secondaryText
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

    override fun onMapReady(googleMap: GoogleMap) {
    googleMap.addMarker(
        MarkerOptions()
            .position(LatLng(0.0, 0.0))
            .title("Marker")
        )
    }
}
