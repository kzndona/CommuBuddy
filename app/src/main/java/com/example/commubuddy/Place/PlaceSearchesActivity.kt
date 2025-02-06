package com.example.commubuddy.Place

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.commubuddy.BuildConfig
import com.example.commubuddy.R
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient

class PlaceSearchesActivity : AppCompatActivity() {

    private val apiKey = BuildConfig.MAPS_API_KEY
    private lateinit var searchLocationBar : SearchView
    private lateinit var placesClient : PlacesClient
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PlacePredictionAdapter
    private val predictions = mutableListOf<PlacePredictionModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_searches)

        // Log an error if apiKey is not set.
        if (apiKey.isEmpty() || apiKey == "DEFAULT_API_KEY") {
            Log.e("Places test", "No api key")
            finish()
            return
        }

        // Initialize the SDK
        Places.initializeWithNewPlacesApiEnabled(applicationContext, apiKey)
        // Create a new PlacesClient instance
        placesClient = Places.createClient(this)


        searchLocationBar = findViewById(R.id.search_location_searches_search_location)
        // Listen for queries then request for Places API autocompletes using query
        searchLocationBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(query: String?): Boolean {
                query?.let { handleQuery(it) }
                return true
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { handleQuery(it) }
                return true
            }
        })

        // Initialize Recycler View
        recyclerView = findViewById(R.id.layout_location_searches_recycler)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Set up Adapter
        adapter = PlacePredictionAdapter(predictions) { prediction ->
            fetchPlaceDetails(prediction.placeId) { latLng ->
                val resultIntent = Intent()
                resultIntent.putExtra("selected_place", PlacePredictionModel(
                    placeId = prediction.placeId,
                    primaryText = prediction.primaryText,
                    secondaryText = prediction.secondaryText,
                    latLng = latLng
                ))
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
        recyclerView.adapter = adapter
    }

    private fun handleQuery(query: String) {
        // Request query to Places API
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setRegionCode("ph")
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                predictions.clear()
                response.autocompletePredictions.forEach { prediction ->
                    predictions.add(
                        PlacePredictionModel(
                            placeId = prediction.placeId,
                            primaryText = prediction.getPrimaryText(null).toString(),
                            secondaryText = prediction.getSecondaryText(null).toString()
                        )
                    )
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, exception.toString(), Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchPlaceDetails(placeId: String, callback: (LatLng) -> Unit) {
        val placeFields = listOf(Place.Field.LAT_LNG)
        val request = FetchPlaceRequest.newInstance(placeId, placeFields)

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                val place = response.place
                val latLng = place.latLng
                latLng?.let { callback(it) }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, exception.toString(), Toast.LENGTH_SHORT).show()
            }
    }
}